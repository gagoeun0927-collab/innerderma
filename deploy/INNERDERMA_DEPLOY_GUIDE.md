# InnerDerma 통합 서버 배포 가이드

InnerDerma가 메인 프로젝트로서 **모든 서비스(백엔드, 프론트, SkinAge, MySQL)를 하나의 nginx에서 통합 관리**합니다.

| 도메인 | 서비스 |
|---|---|
| `inner-derma.duckdns.org` | InnerDerma (API + 프론트엔드 SPA) |
| `skinage-api.duckdns.org` | SkinAge (피부 분석 API) |

---

## 아키텍처

```
인터넷 :80/:443
    │
    └── [InnerDerma Nginx] (도메인 기반 라우팅)
            │
            ├── inner-derma.duckdns.org
            │     ├── /api/*        → app-api :8080 (Spring Boot)
            │     ├── /swagger-ui/* → app-api :8080
            │     └── /*            → 프론트엔드 정적 파일
            │
            └── skinage-api.duckdns.org
                  ├── /api/*        → skinage :8000 (FastAPI)
                  └── /docs         → skinage :8000
            
            [MySQL :3306] ← app-api (내부만)
```

---

## 전제 조건

- Ubuntu 서버에 Docker 설치 완료
- 방화벽 80/443 열림 (가비아 콘솔 + ufw)
- DuckDNS 도메인 2개 (`inner-derma`, `skinage-api`) 같은 서버 IP로 설정
- certbot 설치됨
- SkinAge 레포가 `/opt/skinage`에 클론됨 (Dockerfile 포함)

---

## Step 1: SkinAge 기존 스택 내리기

SkinAge가 이미 Docker로 배포되어 있으므로, 기존 컨테이너(nginx + api)를 전부 내립니다.
InnerDerma compose에서 SkinAge를 새로 빌드해 올릴 것이므로 안전합니다.

```bash
cd /opt/skinage/SkinAge
docker compose -f docker-compose.prod.yml down
```

확인:
```bash
docker ps | grep skinage   # 아무것도 안 나오면 OK
```

---

## Step 2: 레포 클론

```bash
# InnerDerma (메인 백엔드)
sudo mkdir -p /opt/innerderma && sudo chown $USER:$USER /opt/innerderma
git clone https://github.com/gagoeun0927-collab/InnerDerma.git /opt/innerderma
cd /opt/innerderma

# 프론트엔드
sudo mkdir -p /opt/innerderma-frontend && sudo chown $USER:$USER /opt/innerderma-frontend
git clone https://github.com/dunsan1008/innerderma_front.git /opt/innerderma-frontend

# SkinAge (이미 /opt/skinage에 있으면 스킵)
# Dockerfile 위치: /opt/skinage/SkinAge/Dockerfile
```

---

## Step 3: 환경변수 설정

```bash
sudo cp /opt/innerderma/deploy/.env.example /opt/innerderma.env
sudo chown $USER:$USER /opt/innerderma.env
chmod 600 /opt/innerderma.env
```

랜덤 시크릿 생성:
```bash
echo "MYSQL_ROOT_PASSWORD: $(openssl rand -hex 20)"
echo "DB_PASSWORD: $(openssl rand -hex 20)"
echo "JWT_SECRET: $(openssl rand -hex 32)"
```

`vi /opt/innerderma.env` 로 아래 내용 채우기:

```env
# MySQL
MYSQL_ROOT_PASSWORD=<위에서 생성한 값>
DB_USER=innerderma_app
DB_PASSWORD=<위에서 생성한 값>

# JWT
JWT_SECRET=<위에서 생성한 값>

# OpenAI (본인의 API 키)
OPENAI_API_KEY=sk-...

# SkinAge (Docker 내부 통신 — 이 값 그대로 사용)
SKINAGE_BASE_URL=http://skinage:8000

# CORS
CORS_ALLOWED_ORIGINS=https://inner-derma.duckdns.org

# Storage
INNERDERMA_SKIN_CAPTURE_STORAGE_PATH=/data/skin-captures

# 프론트엔드 빌드 경로 (프론트 배포 시 설정, 아직 없으면 빈 디렉토리라도 만들어 두기)
FRONTEND_BUILD_PATH=/opt/innerderma-frontend/dist

# Timezone
TZ=Asia/Seoul
```

> **직접 넣어야 하는 값**: `OPENAI_API_KEY`만. 나머지 비밀번호/시크릿은 `openssl rand`로 생성.

프론트엔드 디렉토리가 아직 없으면:
```bash
sudo mkdir -p /opt/innerderma-frontend/dist
sudo chown $USER:$USER /opt/innerderma-frontend/dist
echo '<!DOCTYPE html><html><body><h1>InnerDerma Frontend (placeholder)</h1></body></html>' > /opt/innerderma-frontend/dist/index.html
```

---

## Step 4: 인증서 발급

SkinAge nginx를 내렸으므로 80번 포트가 비어있습니다.

```bash
cd /opt/innerderma
mkdir -p certbot-webroot

# HTTP 전용 nginx로 부트스트랩
cp deploy/nginx/nginx.http-only.conf deploy/nginx/active.conf
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d frontend
```

인증서 발급:
```bash
# inner-derma 도메인 발급
sudo certbot certonly --webroot -w /opt/innerderma/certbot-webroot \
  -d inner-derma.duckdns.org
```

> `skinage-api.duckdns.org` 인증서가 이미 있다면 그대로 사용합니다.
> 없거나 만료됐으면 추가 발급:
> ```bash
> sudo certbot certonly --webroot -w /opt/innerderma/certbot-webroot \
>   -d skinage-api.duckdns.org
> ```

인증서 경로 확인:
```bash
sudo ls /etc/letsencrypt/live/inner-derma.duckdns.org/
sudo ls /etc/letsencrypt/live/skinage-api.duckdns.org/
```

---

## Step 5: HTTPS 전환 + 전체 기동

```bash
cd /opt/innerderma

# HTTPS 통합 conf 적용
cp deploy/nginx/nginx.innerderma.conf deploy/nginx/active.conf

# 전체 스택 기동 (MySQL + app-api + SkinAge + nginx)
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d

# nginx에 새 설정 반영
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml restart frontend
```

> SkinAge 빌드에 시간이 걸릴 수 있습니다 (첫 빌드 시 2~5분).
> `docker compose logs -f skinage`로 빌드/기동 상태 확인.

---

## Step 6: 검증

```bash
# 전 서비스 상태
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml ps

# InnerDerma Health
curl -fsSL https://inner-derma.duckdns.org/api/innerderma/health

# SkinAge Health
curl -fsSL https://skinage-api.duckdns.org/health

# Swagger UI
# 브라우저: https://inner-derma.duckdns.org/swagger-ui/index.html
# 브라우저: https://skinage-api.duckdns.org/docs

# 회원가입 + 토큰 테스트
curl -X POST https://inner-derma.duckdns.org/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"userCode":"DEMO-001","name":"테스트","phoneNumber":"010-1234-1234"}'
```

---

## 업데이트 배포

```bash
cd /opt/innerderma

# 백엔드 업데이트
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml build app-api
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d app-api

# SkinAge 업데이트
cd /opt/skinage && git pull origin master
cd /opt/innerderma
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml build skinage
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d skinage
docker compose --env
# 프론트엔드 업데이트
cd /opt/innerderma-frontend && git pull origin main && npm install && npm run build
cd /opt/innerderma
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml restart frontend
```

---

## 인증서 자동 갱신

certbot이 자동 갱신할 때 nginx에 새 인증서를 반영하는 훅을 설정합니다.

```bash
# 훅 디렉토리 생성
sudo mkdir -p /etc/letsencrypt/renewal-hooks/deploy

# nginx reload 훅 작성
sudo tee /etc/letsencrypt/renewal-hooks/deploy/00-reload-nginx.sh > /dev/null <<'EOF'
#!/bin/sh
docker compose --env-file /opt/innerderma.env -f /opt/innerderma/docker-compose.prod.yml exec -T frontend nginx -s reload
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/deploy/00-reload-nginx.sh

# skinage 인증서의 webroot도 InnerDerma nginx를 사용하도록 통합
sudo sed -i 's|skinage-api.duckdns.org = .*|skinage-api.duckdns.org = /opt/innerderma/certbot-webroot|' /etc/letsencrypt/renewal/skinage-api.duckdns.org.conf

# 검증 (실제 갱신 없이 dry-run)
sudo certbot renew --dry-run
```

> certbot.timer가 기본 활성화되어 있으므로 별도 cron은 추가하지 마세요.
> 확인: `sudo systemctl is-enabled certbot.timer`

---

## 로그 확인

```bash
# 전체
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs -f

# 서비스별
docker compose --git pull origin main
env-file /opt/innerderma.env -f docker-compose.prod.yml logs -f app-api
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs -f skinage
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs -f mysql
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs -f frontend
```

---

## 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| certbot Timeout | 방화벽 80 안 열림 | 가비아 콘솔 + ufw 확인 |
| skinage-api.duckdns.org 502 | SkinAge 모델 로딩 중 (최대 60초) | `docker logs skinage` 확인, 대기 |
| inner-derma.duckdns.org 502 | app-api 미기동 또는 MySQL 연결 대기 | `docker logs app-api` 확인 |
| Flyway 실패 | DB 스키마 불일치 | baseline-on-migrate 확인, 또는 MySQL 볼륨 삭제 후 재기동 |
| CORS 에러 | CORS_ALLOWED_ORIGINS 불일치 | `/opt/innerderma.env` 확인 |
| 프론트 404 | FRONTEND_BUILD_PATH 경로에 파일 없음 | 빌드 결과물 확인, placeholder index.html이라도 |
| `Permission denied` | /opt 디렉토리 권한 | `sudo chown $USER:$USER /opt/<dir>` |
| nginx가 설정 변경 안 반영 | inode 변경 문제 | `docker compose restart frontend` |

---

## 서버 파일 구조

```
/opt/innerderma/              ← InnerDerma 레포 (메인)
  ├─ docker-compose.prod.yml  ← 전체 스택 정의
  ├─ Dockerfile               ← Spring Boot 빌드
  ├─ deploy/nginx/active.conf ← 통합 nginx 설정 (런타임)
  └─ certbot-webroot/
/opt/innerderma.env           ← 시크릿 (레포 밖)
/opt/skinage/                 ← SkinAge 레포 (Dockerfile + 모델)
/opt/innerderma-frontend/dist ← 프론트엔드 빌드 결과물
/data/skin-captures/          ← 피부 사진 파일 (데이터 볼륨)
```
