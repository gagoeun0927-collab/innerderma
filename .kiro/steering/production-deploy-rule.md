# 프로덕션 배포 운영 규칙

## 배포 방식

`main` 브랜치에 push하면 `.github/workflows/deploy.yml`이 자동 배포한다.

1. Build & Test (`./gradlew build` — 실패 시 배포 안 됨)
2. 서버 SSH → `git pull` → `active.conf` 갱신 → `build app-api` → `up -d app-api` → `restart frontend`
3. `https://inner-derma.duckdns.org/api/innerderma/health` 로 기동 확인 (30회 × 5초)
   실패하면 `app-api` 로그를 출력하고 배포를 실패 처리한다.

`main` merge/push는 공유 브랜치 작업이므로 **사용자 확인을 받고 진행한다.**

## 서버 환경

| 항목 | 값 |
|---|---|
| 서버 | `ubuntu@1.201.116.161` |
| 레포 경로 | `/opt/innerderma` |
| 시크릿 | `/opt/innerderma.env` (레포 밖) |
| DB | MySQL 8.0.46 컨테이너, 스키마 `innerderma` |
| compose | `docker-compose.prod.yml` |
| 도메인 | `inner-derma.duckdns.org`, `skinage-api.duckdns.org` |

프로덕션 설정: `ddl-auto=validate` + `spring.flyway.enabled=true`
로컬 설정: PostgreSQL(Docker) + `ddl-auto=update` + Flyway 비활성

## nginx 주의사항

nginx가 실제로 읽는 파일은 `deploy/nginx/active.conf`이며, 이는
`deploy/nginx/nginx.innerderma.conf`의 **복사본**이다.
`nginx.innerderma.conf`만 수정하고 push하면 반영되지 않는다.

이제 `deploy.yml`이 매 배포마다 자동 복사하므로 수동 작업은 필요 없다.
수동으로 반영해야 할 때:

```bash
cd /opt/innerderma
cp deploy/nginx/nginx.innerderma.conf deploy/nginx/active.conf
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml restart frontend
```

라우팅: `/api/`, `/swagger-ui/`, `/actuator/`, `/product-images/` → `app-api:8080`
그 외 `/` → 프론트엔드 SPA (`try_files $uri /index.html`).
**새 백엔드 경로를 추가하면 nginx location도 추가해야 한다.**
빼먹으면 SPA의 `index.html`(Content-Type: text/html)이 200으로 반환되어
"동작하는 것처럼 보이지만 실제로는 잘못된 응답"이 나온다.

## 상품 이미지

- 저장 위치: `src/main/resources/static/product-images/{product_id}.jpg` (git 관리)
- 서빙: Spring Boot 정적 리소스 → nginx가 `app-api`로 프록시
- DB `image_url`: `/product-images/{product_code}.jpg` **상대경로**
- 이미지 추가 = 파일을 위 디렉토리에 넣고 commit/push하면 CI가 배포

## MySQL 접속 (대화형 프롬프트 회피)

`exec` 로 대화형 비밀번호 입력은 실패하기 쉽다. env 파일에서 읽어 `MYSQL_PWD`로 전달한다.
**비밀번호를 echo하거나 로그에 남기지 않는다.**

```bash
cd /opt/innerderma
MYSQL_ROOT_PASSWORD=$(grep -E '^MYSQL_ROOT_PASSWORD=' /opt/innerderma.env | cut -d= -f2-)
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml exec -T \
  -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
  mysql -u root -N -e "<SQL>"
```

앱 계정을 쓸 때는 `DB_USER` / `DB_PASSWORD`를 같은 방식으로 읽는다.

## 자주 쓰는 진단 명령

```bash
# 앱 로그
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs --tail=50 app-api

# 컨테이너 상태
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml ps

# Flyway 이력
... mysql -u root -N -e "SELECT installed_rank, version, description, success FROM innerderma.flyway_schema_history ORDER BY installed_rank;"

# 스키마 전체 덤프 (엔티티와 대조할 때)
... mysql -u root -N -e "SELECT CONCAT(table_name,' | ',column_name,' | ',column_type,' | ',is_nullable) FROM information_schema.columns WHERE table_schema='innerderma' ORDER BY table_name, ordinal_position;"
```

## 502 Bad Gateway 대응 순서

1. `logs --tail=50 app-api` 로 원인 확인
2. `Detected failed migration to version N` → Flyway 실패 기록 삭제 후 재기동
   (부분 적용된 DDL이 있는지 먼저 확인. MySQL DDL은 롤백되지 않는다)
3. `Schema validation: wrong column type` → 엔티티와 실제 스키마 불일치.
   새 마이그레이션으로 스키마를 엔티티에 맞춘다
4. Hibernate validate는 **첫 에러에서 멈춘다.** 하나 고치면 다음 게 나올 수 있으니
   위 "스키마 전체 덤프"로 한 번에 대조하는 편이 빠르다

## 배포 후 검증 체크리스트

```bash
curl -fsS https://inner-derma.duckdns.org/api/innerderma/health
curl -fsS "https://inner-derma.duckdns.org/api/products?source=PIECE_SEOUL"   # 22개
curl -fsS "https://inner-derma.duckdns.org/api/products?source=WIM_STORE"     # 16개
curl -fsS "https://inner-derma.duckdns.org/api/products/PSS_001?locale=en"    # translation 포함
curl -fsSI https://inner-derma.duckdns.org/product-images/PSS_001.jpg         # image/jpeg 확인
```

이미지 확인 시 **Content-Type을 반드시 본다.** `text/html`이면 nginx가 SPA로
폴백한 것이므로 200이어도 실패다.

## 현재 프로덕션 상태 (2026-08-20 기준)

- Flyway V5까지 적용 완료
- 상품 45개 (데모 7 + KB 38: PIECE_SEOUL 22 / WIM_STORE 16)
- 번역 192건 (48개 상품 × ko/en/ja/zh)
- 상품 이미지 48개 서빙 중
