# InnerDerma 서버 배포 가이드

**동일 서버에 SkinAge(`skinage-api.duckdns.org`)가 이미 배포된 상태**에서 InnerDerma를 추가 배포합니다.

- 도메인: `inner-derma.duckdns.org`
- 구성: Spring Boot (백엔드) + MySQL + 프론트엔드 (React/SPA) + Nginx
- SkinAge 연동: 같은 서버 내부 `http://localhost:8000` 또는 `https://skinage-api.duckdns.org`

---

## 아키텍처 (같은 서버)

```
인터넷
  │
  ├── inner-derma.duckdns.org:443 → [InnerDerma Nginx :8443]
  │       ├── /api/*          → app-api :8080
  │       ├── /swagger-ui/*   → app-api :8080
  │       ├── /api-docs       → app-api :8080
  │       └── /*              → 프론트엔드 정적 파일
  │
  └── skinage-api.duckdns.org:443 → [SkinAge Nginx :443] (기존)
          └── /api/*          → skinage :8000
```

**포트 전략:** SkinAge가 이미 80/443을 점유하므로 InnerDerma nginx는 **8443(HTTPS) / 8080(HTTP redirect)** 를 사용하거나, **하나의 nginx로 통합**합니다.

---

## Option A: 통합 Nginx (권장)

SkinAge의 nginx를 확장해서 두 도메인을 모두 처리합니다.

### A-1. DuckDNS 도메인 추가

https://www.duckdns.org 에서 `inner-derma` 서브도메인 추가, 같은 서버 IP로 설정.

### A-2. InnerDerma 인증서 발급

```bash
# SkinAge nginx가 80을 서빙 중이므로 webroot 방식 사용
sudo certbot certonly --webroot \
  -w /opt/skinage/SkinAge/certbot-webroot \
  -d inner-derma.duckdns.org
```

### A-3. InnerDerma 레포 클론

```bash
sudo mkdir -p /opt/innerderma && sudo chown $USER:$USER /opt/innerderma
git clone https://github.com/<owner>/InnerDerma.git /opt/innerderma
cd /opt/innerderma
```

### A-4. MySQL + app-api 기동 (nginx 없이)

`docker-compose.prod.yml`에서 `frontend` 서비스를 제거하고 InnerDerma 백엔드만 띄움:

```bash
# 환경변수 설정
sudo cp deploy/.env.example /opt/innerderma.env
sudo chown $USER:$USER /opt/innerderma.env
chmod 600 /opt/innerderma.env
# 에디터로 값 채우기 (아래 참조)
```

`/opt/innerderma.env`:
```env
MYSQL_ROOT_PASSWORD=<openssl rand -hex 20>
DB_USER=innerderma_app
DB_PASSWORD=<openssl rand -hex 20>
JWT_SECRET=<openssl rand -hex 32>
OPENAI_API_KEY=sk-...
SKINAGE_BASE_URL=http://host.docker.internal:8000
CORS_ALLOWED_ORIGINS=https://inner-derma.duckdns.org
INNERDERMA_SKIN_CAPTURE_STORAGE_PATH=/data/skin-captures
TZ=Asia/Seoul
```

> **SKINAGE_BASE_URL**: 같은 서버에서 SkinAge가 Docker로 돌고 있으면 `http://172.17.0.1:8000` 또는 Docker network로 연결. SkinAge가 host network에서 돌면 `http://localhost:8000`. 확실하면 외부 URL `https://skinage-api.duckdns.org`도 가능.

기동:
```bash
cd /opt/innerderma
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d mysql app-api
```

### A-5. SkinAge nginx에 InnerDerma 서버 블록 추가

SkinAge의 `deploy/nginx/active.conf`를 수정하여 `inner-derma.duckdns.org` 서버 블록을 추가합니다:

```bash
# SkinAge nginx 설정 편집
vi /opt/skinage/SkinAge/deploy/nginx/active.conf
```

기존 내용 뒤에 추가:

```nginx
# === InnerDerma ===
server {
    listen 80;
    server_name inner-derma.duckdns.org;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name inner-derma.duckdns.org;

    ssl_certificate     /etc/letsencrypt/live/inner-derma.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/inner-derma.duckdns.org/privkey.pem;

    client_max_body_size 12m;

    # InnerDerma API
    location /api/ {
        proxy_pass http://172.17.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Swagger
    location /swagger-ui/ {
        proxy_pass http://172.17.0.1:8080/swagger-ui/;
        proxy_set_header Host $host;
    }

    location /api-docs {
        proxy_pass http://172.17.0.1:8080/api-docs;
        proxy_set_header Host $host;
    }

    # 프론트엔드 (정적 파일)
    location / {
        root /usr/share/nginx/html/innerderma;
        try_files $uri /index.html;
    }
}
```

> **172.17.0.1**은 Docker bridge network에서 호스트를 가리키는 IP. InnerDerma app-api가 Docker로 돌면서 port 8080을 publish하면 이 주소로 접근 가능.

### A-6. InnerDerma app-api 포트 공개

`docker-compose.prod.yml`에서 app-api의 `expose`를 `ports`로 변경:

```yaml
  app-api:
    ...
    ports:
      - "8080:8080"
```

### A-7. 프론트엔드 배포

프론트엔드 빌드 결과물을 SkinAge nginx 컨테이너에서 접근할 수 있는 위치에 놓습니다:

```bash
# 프론트엔드 빌드 (프론트 레포에서)
cd /opt/innerderma-frontend
npm run build

# 빌드 결과물을 SkinAge nginx에서 접근할 위치에 복사
sudo mkdir -p /opt/skinage/SkinAge/frontend-innerderma
cp -r dist/* /opt/skinage/SkinAge/frontend-innerderma/
```

SkinAge `docker-compose.prod.yml`의 nginx 볼륨에 추가:
```yaml
  nginx:
    volumes:
      - ./deploy/nginx/active.conf:/etc/nginx/conf.d/default.conf:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - ./certbot-webroot:/var/www/certbot:ro
      - ./frontend-innerderma:/usr/share/nginx/html/innerderma:ro   # 추가
```

### A-8. nginx 재시작

```bash
cd /opt/skinage/SkinAge
docker compose -f docker-compose.prod.yml restart nginx
```

---

## Option B: 별도 Nginx (포트 분리)

InnerDerma만의 nginx를 별도 포트(8443)에서 실행. Cloudflare나 외부 로드밸런서가 있을 때 적합.

이 방식은 클라이언트가 `https://inner-derma.duckdns.org:8443/api/...`로 접근해야 하므로 Option A보다 불편합니다.

---

## 검증

```bash
# InnerDerma Health Check
curl -fsSL https://inner-derma.duckdns.org/api/innerderma/health

# 회원가입
curl -X POST https://inner-derma.duckdns.org/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"userCode":"DEMO-001","name":"테스트","phoneNumber":"010-1234-1234"}'

# Swagger UI
# 브라우저: https://inner-derma.duckdns.org/swagger-ui/index.html

# SkinAge 연동 확인 (토큰 필요)
TOKEN=$(curl -s -X POST "https://inner-derma.duckdns.org/api/auth/token?userCode=DEMO-001" | jq -r '.data.token')
curl -X POST https://inner-derma.duckdns.org/api/users/DEMO-001/skin-captures/analyze \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@face.jpg"
```

## 업데이트 배포

```bash
cd /opt/innerderma
git pull origin main
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml build app-api
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml up -d app-api
```

## 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| nginx 502 on inner-derma | app-api 미기동 또는 포트 미공개 | `docker ps`로 app-api 확인, ports 8080 공개 확인 |
| SKINAGE_API_UNAVAILABLE | SkinAge URL 접근 불가 | `curl http://172.17.0.1:8000/api/v1/health` 확인 |
| Flyway 실패 | MySQL 아직 안 뜸 | `depends_on: mysql (service_healthy)` + 재시도 |
| CORS 에러 | `CORS_ALLOWED_ORIGINS` 미설정 | `/opt/innerderma.env`에 도메인 설정 |
| certbot 실패 | SkinAge nginx가 inner-derma 도메인의 challenge를 서빙 못 함 | nginx conf에 `server_name _` 대신 도메인별 분리 |
