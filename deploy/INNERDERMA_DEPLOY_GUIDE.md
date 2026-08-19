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

## 배포 절차

### 1. 서버 기본 설정 (이미 완료되어 있다면 스킵)

```bash
# Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# 방화벽 (가비아 콘솔 + ufw)
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# 스왑 (RAM < 4GB)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# certbot
sudo apt-get update && sudo apt-get install -y certbot
```

### 2. DuckDNS 도메인

두 도메인 모두 같은 서버 IP를 가리키도록 설정:
- `inner-derma.duckdns.org` → 서버 IP
- `skinage-api.duckdns.org` → 서버 IP

### 3. 레포 클론

```bash
# InnerDerma (메인)
sudo mkdir -p /opt/innerderma && sudo chown $USER:$USER /opt/innerderma
git clone https://github.com/<owner>/InnerDerma.git /opt/innerderma

# SkinAge
sudo mkdir -p /opt/skinage && sudo chown $USER:$USER /opt/skinage
git clone https://github.com/<owner>/SkinAge.git /opt/skinage

# 프론트엔드 (빌드 결과물만 필요)
sudo mkdir -p /opt/innerderma-frontend && sudo chown $USER:$USER /opt/innerderma-frontend
# 프론트 레포 클론 후 빌드, 또는 빌드된 dist를 직접 복사
```

### 4. 환경변수 설정

```bash
sudo cp /opt/innerderma/deploy/.env.example /opt/innerderma.env
sudo chown $USER:$USER /opt/innerderma.env
chmod 600 /opt/innerderma.env
```

에디터로 값 채우기:
```bash
MYSQL_ROOT_PASSWORD=$(openssl rand -hex 20)
DB_USER=innerderma_app
DB_PASSWORD=$(openssl rand -hex 20)
JWT_SECRET=$(openssl rand -hex 32)
OPENAI_API_KEY=sk-...
SKINAGE_BASE_URL=http://skinage:8000
CORS_ALLOWED_ORIGINS=https://inner-derma.duckdns.org
INNERDERMA_SKIN_CAPTURE_STORAGE_PATH=/data/skin-captures
FRONTEND_BUILD_PATH=/opt/innerderma-frontend/dist
TZ=Asia/Seoul
```

### 5. 인증서 발급

```bash
cd /opt/innerderma
mkdir -p certbot-webroot

# HTTP 전용 nginx로 부트스트랩
cp deploy/nginx/nginx.http-only.conf deploy/nginx/active.conf
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d frontend

# 두 도메인 인증서 한번에 발급
sudo certbot certonly --webroot -w /opt/innerderma/certbot-webroot \
  -d inner-derma.duckdns.org \
  -d skinage-api.duckdns.org
```

> 인증서가 **한 인증서에 두 도메인**으로 발급됩니다. 별도로 발급하려면 certbot을 2번 실행하세요.
> 별도 발급 시 nginx conf의 ssl_certificate 경로가 도메인별로 달라야 합니다.

만약 한 인증서에 2개 도메인을 넣었다면 nginx conf에서 두 서버 블록 모두 같은 경로를 사용:
```
ssl_certificate     /etc/letsencrypt/live/inner-derma.duckdns.org/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/inner-derma.duckdns.org/privkey.pem;
```

### 6. HTTPS 전환 + 전체 기동

```bash
# HTTPS conf 적용
cp deploy/nginx/nginx.innerderma.conf deploy/nginx/active.conf

# 인증서 경로가 별도 발급이면 skinage 블록의 경로도 확인

# 전체 스택 기동
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d

# nginx conf 반영
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml restart frontend
```

### 7. SkinAge 기존 nginx 제거

SkinAge가 기존에 자체 nginx를 갖고 있었다면 중지합니다:
```bash
cd /opt/skinage/SkinAge
docker compose -f docker-compose.prod.yml down
# SkinAge API 컨테이너는 InnerDerma compose에서 관리하므로 여기선 전부 내림
```

---

## 검증

```bash
# InnerDerma
curl -fsSL https://inner-derma.duckdns.org/api/innerderma/health
# 브라우저: https://inner-derma.duckdns.org/swagger-ui/index.html

# SkinAge
curl -fsSL https://skinage-api.duckdns.org/health
# 브라우저: https://skinage-api.duckdns.org/docs

# 통합 테스트 (사진 업로드 → SkinAge 분석)
TOKEN=$(curl -s -X POST "https://inner-derma.duckdns.org/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"userCode":"DEMO-001","name":"테스트","phoneNumber":"010-1234-1234"}' | jq -r '.data.token')

curl -X POST https://inner-derma.duckdns.org/api/users/DEMO-001/skin-captures/analyze \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@face.jpg"
```

---

## 업데이트 배포

```bash
cd /opt/innerderma

# 백엔드 업데이트
git pull origin main
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml build app-api
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d app-api

# SkinAge 업데이트
cd /opt/skinage && git pull origin main
cd /opt/innerderma
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml build skinage
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d skinage

# 프론트엔드 업데이트
cd /opt/innerderma-frontend && git pull && npm run build
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml restart frontend
```

---

## 인증서 자동 갱신

```bash
sudo mkdir -p /etc/letsencrypt/renewal-hooks/deploy
sudo tee /etc/letsencrypt/renewal-hooks/deploy/00-reload-nginx.sh > /dev/null <<'EOF'
#!/bin/sh
docker compose --env-file /opt/innerderma.env -f /opt/innerderma/docker-compose.prod.yml exec -T frontend nginx -s reload
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/deploy/00-reload-nginx.sh
sudo certbot renew --dry-run
```

---

## 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| certbot Timeout | 방화벽 80 안 열림 | 가비아 콘솔 + ufw 확인 |
| skinage-api.duckdns.org 502 | SkinAge 컨테이너 미기동/모델 로딩 중 | `docker logs skinage` 확인, start_period 60s 대기 |
| inner-derma.duckdns.org 502 | app-api 미기동 | `docker logs app-api` 확인, MySQL healthy 대기 |
| Flyway 실패 | DB 스키마 불일치 | baseline-on-migrate 확인 또는 DB 초기화 |
| CORS 에러 | CORS_ALLOWED_ORIGINS 불일치 | /opt/innerderma.env 확인 |
| 프론트 404 | FRONTEND_BUILD_PATH 경로 오류 | 빌드 결과물 위치 확인 |
| `sed -i` 후 nginx 변경 안 됨 | inode 변경 | `docker compose restart frontend` |

---

## 서버 파일 구조

```
/opt/innerderma/              ← InnerDerma 레포 (메인 프로젝트)
  ├─ docker-compose.prod.yml  ← 전체 스택 정의
  ├─ Dockerfile               ← Spring Boot 빌드
  ├─ deploy/nginx/active.conf ← 통합 nginx 설정
  └─ certbot-webroot/
/opt/innerderma.env           ← 시크릿 (레포 밖)
/opt/skinage/                 ← SkinAge 레포 (Dockerfile + 모델)
/opt/innerderma-frontend/     ← 프론트엔드 레포 (빌드 → dist/)
/data/skin-captures/          ← 피부 사진 파일
```
