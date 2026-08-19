# InnerDerma 서버 배포 가이드

InnerDerma 백엔드(Spring Boot 4.x + Java 21) + SkinAge(Python FastAPI) + MySQL + Nginx를 가비아 서버(Ubuntu)에 배포하는 절차입니다.

## 전제 조건

- Ubuntu 22.04+ 서버 (RAM 4GB 이상 권장, 2GB면 스왑 필수)
- 루트 50GB + 데이터 50GB 디스크 구성
- 공인 IP + DuckDNS 도메인 (예: `innerderma.duckdns.org`)
- SSH 키 접속 설정 완료
- SkinAge 레포에 Dockerfile 준비됨

## 0. 디스크 마운트 (데이터 볼륨)

```bash
# 데이터 디스크 확인
lsblk
# 포맷 (첫 배포 시만 — 기존 데이터 있으면 절대 실행 금지)
sudo mkfs.ext4 /dev/vdb
sudo mkdir -p /data
sudo mount /dev/vdb /data
echo '/dev/vdb /data ext4 defaults 0 2' | sudo tee -a /etc/fstab

# 디렉토리 구조
sudo mkdir -p /data/mysql /data/skin-captures /data/logs
sudo chown -R $USER:$USER /data
```

## 1. Docker 설치

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
docker --version
```

## 2. 방화벽

**가비아 콘솔**: 인바운드 규칙에 TCP 80, 443 추가

**서버 ufw**:
```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

## 3. DuckDNS 도메인 설정

1. https://www.duckdns.org 에서 서브도메인 생성
2. A 레코드를 가비아 공인 IP로 설정
3. 이 문서의 `<domain>`을 실제 도메인으로 치환 (예: `innerderma.duckdns.org`)

## 4. certbot 설치

```bash
sudo apt-get update && sudo apt-get install -y certbot
```

## 5. 레포 클론

```bash
# InnerDerma 백엔드
sudo mkdir -p /opt/innerderma && sudo chown $USER:$USER /opt/innerderma
git clone https://github.com/<owner>/InnerDerma.git /opt/innerderma
cd /opt/innerderma

# SkinAge (별도 레포)
sudo mkdir -p /opt/skinage && sudo chown $USER:$USER /opt/skinage
git clone https://github.com/<owner>/SkinAge.git /opt/skinage
```

## 6. SkinAge Docker 이미지 빌드

```bash
cd /opt/skinage
docker build -t innerderma/skinage:latest .
```

## 7. 환경변수 설정

```bash
sudo cp /opt/innerderma/deploy/.env.example /opt/innerderma.env
sudo chown $USER:$USER /opt/innerderma.env
chmod 600 /opt/innerderma.env
```

에디터로 `/opt/innerderma.env` 값 채우기:

```bash
# 생성 도구
openssl rand -hex 20   # DB_PASSWORD용
openssl rand -hex 32   # JWT_SECRET용
```

```env
MYSQL_ROOT_PASSWORD=<생성된 값>
DB_USER=innerderma_app
DB_PASSWORD=<생성된 값>
JWT_SECRET=<생성된 값>
OPENAI_API_KEY=sk-...
SKINAGE_IMAGE=innerderma/skinage:latest
SKINAGE_BASE_URL=http://skinage:8000
CORS_ALLOWED_ORIGINS=https://<domain>
INNERDERMA_SKIN_CAPTURE_STORAGE_PATH=/data/skin-captures
TZ=Asia/Seoul
```

## 8. 인증서 발급 (2단계 부트스트랩)

```bash
cd /opt/innerderma
mkdir -p certbot-webroot

# 1단계: HTTP 전용 nginx로 frontend만 기동
cp deploy/nginx/nginx.http-only.conf deploy/nginx/active.conf
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d frontend

# 인증서 발급
sudo certbot certonly --webroot -w /opt/innerderma/certbot-webroot -d <domain>

# 2단계: HTTPS conf로 교체
cp deploy/nginx/nginx.innerderma.conf deploy/nginx/active.conf
sed -i "s/<domain>/<domain>/g" deploy/nginx/active.conf
```

## 9. 전체 스택 기동

```bash
cd /opt/innerderma
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d

# nginx conf 반영
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml restart frontend
```

## 10. 스왑 설정 (RAM < 4GB)

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 11. 검증

```bash
# 전 서비스 상태 확인
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml ps

# Health Check
curl -fsSL https://<domain>/api/innerderma/health

# Swagger UI 접근
# 브라우저: https://<domain>/swagger-ui/index.html

# API 테스트
curl -X POST https://<domain>/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"userCode":"DEMO-001","name":"테스트","phoneNumber":"010-1234-1234"}'
```

## 12. 인증서 자동 갱신

```bash
sudo mkdir -p /etc/letsencrypt/renewal-hooks/deploy
sudo tee /etc/letsencrypt/renewal-hooks/deploy/00-reload-nginx.sh > /dev/null <<'EOF'
#!/bin/sh
docker compose --env-file /opt/innerderma.env -f /opt/innerderma/docker-compose.prod.yml exec -T frontend nginx -s reload
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/deploy/00-reload-nginx.sh
sudo certbot renew --dry-run
```

## 13. 업데이트 배포

```bash
cd /opt/innerderma
git pull origin main
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml build app-api
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d app-api
```

SkinAge 업데이트:
```bash
cd /opt/skinage
git pull origin main
docker build -t innerderma/skinage:latest .
cd /opt/innerderma
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d skinage
```

## 14. 로그 확인

```bash
# 전체 로그
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs -f

# 특정 서비스
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs -f app-api
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs -f skinage
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs -f mysql
```

## 15. 롤백

```bash
cd /opt/innerderma
git checkout <이전-commit>
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml build app-api
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d app-api
```

## 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| `certbot: Timeout during connect` | 가비아 콘솔에서 80 안 열림 | 인바운드 규칙 추가 |
| `app-api` 기동 실패 | MySQL 아직 안 뜸 | `depends_on: mysql (service_healthy)` 확인, 재시도 |
| `SKINAGE_API_UNAVAILABLE` | SkinAge 컨테이너 다운 | `docker compose logs skinage` 확인 |
| Flyway migration 실패 | 기존 테이블과 V1 스키마 불일치 | `baseline-on-migrate=true` 확인, 또는 DB 초기화 |
| OOM Kill | 메모리 부족 | 스왑 확인, SkinAge 모델 로딩 시 메모리 사용량 체크 |
| nginx 502 Bad Gateway | app-api 아직 기동 중 | 30초 대기 후 재시도 |

## 파일 구조 (서버)

```
/opt/innerderma/          ← InnerDerma 레포 클론
  ├─ docker-compose.prod.yml
  ├─ Dockerfile
  ├─ deploy/nginx/active.conf
  └─ ...
/opt/innerderma.env       ← 시크릿 (레포 밖)
/opt/skinage/             ← SkinAge 레포 클론
/data/mysql/              ← MySQL 데이터
/data/skin-captures/      ← 피부 사진 파일
/data/logs/               ← 앱 로그
```
