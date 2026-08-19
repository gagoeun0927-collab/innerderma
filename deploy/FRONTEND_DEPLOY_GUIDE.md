# InnerDerma 프론트엔드 배포 가이드

프론트엔드 개발자가 InnerDerma 서버에 프론트를 배포하기 위한 가이드입니다.

---

## 현재 서버 구조

```
서버: 가비아 Ubuntu (IP: 1.201.116.161)
도메인: inner-derma.duckdns.org

┌─────────────────────────────────────────────────────┐
│                    서버 내부                          │
│                                                     │
│  [Nginx :80/:443] ← 인터넷                          │
│      │                                              │
│      ├── inner-derma.duckdns.org                    │
│      │     ├── /api/*        → Spring Boot :8080    │
│      │     ├── /swagger-ui/* → Spring Boot :8080    │
│      │     └── /*            → 프론트 정적 파일      │
│      │                                              │
│      └── skinage-api.duckdns.org                    │
│            └── /api/*        → SkinAge :8000        │
│                                                     │
│  [MySQL :3306] ← Spring Boot (내부만)               │
└─────────────────────────────────────────────────────┘
```

**핵심**: `inner-derma.duckdns.org`의 `/api/` 이외 모든 요청은 프론트엔드 정적 파일로 서빙됩니다 (SPA `try_files $uri /index.html` 방식).

---

## 프론트엔드 레포

- **URL**: https://github.com/dunsan1008/innerderma_front
- **서버 경로**: `/opt/innerderma-frontend/`
- **빌드 결과물 위치**: `/opt/innerderma-frontend/dist/` (nginx가 이 경로를 서빙)

---

## 백엔드 API 정보

| 항목 | 값 |
|---|---|
| Base URL (프로덕션) | `https://inner-derma.duckdns.org/api` |
| Base URL (로컬 개발) | `http://localhost:8080/api` |
| Swagger UI | https://inner-derma.duckdns.org/swagger-ui/index.html |
| API Docs (JSON) | https://inner-derma.duckdns.org/api-docs |
| 인증 방식 | Bearer JWT (`Authorization: Bearer <token>`) |
| CORS | `https://inner-derma.duckdns.org` 허용됨 |

---

## 주요 API 엔드포인트 (프론트에서 호출할 것)

### 인증
```
POST /api/auth/register        ← 회원가입 (userCode, name, phoneNumber → token 반환)
POST /api/auth/token?userCode= ← 기존 사용자 토큰 발급
```

### 사용자
```
GET  /api/users/{userCode}              ← 프로필 조회
PUT  /api/users/{userCode}              ← 프로필 수정 (name, phoneNumber)
GET  /api/users/{userCode}/preference   ← 언어 설정 조회
PUT  /api/users/{userCode}/preference   ← 언어 설정 변경 (locale: "ko", "en", "ja"...)
```

### 피부 사진
```
POST /api/users/{userCode}/skin-captures         ← 사진 업로드 (multipart file)
POST /api/users/{userCode}/skin-captures/analyze ← 사진 업로드 + AI 분석 한번에 (multipart file, actualAge)
GET  /api/users/{userCode}/skin-captures/latest  ← 최신 촬영
GET  /api/users/{userCode}/skin-captures/today   ← 오늘 촬영 상태
GET  /api/users/{userCode}/skin-captures/history ← 촬영 이력 (from, to)
```

### 자가문진
```
POST /api/users/{userCode}/self-checks         ← 문진 제출 (→ 스냅샷도 자동 생성됨)
GET  /api/users/{userCode}/self-checks/latest  ← 최신 문진
GET  /api/users/{userCode}/self-checks/history ← 문진 이력 (from, to)
```

### AI Care (핵심)
```
POST /api/users/{userCode}/ai-care?locale=ko ← AI 케어 솔루션 생성 (LLM 다국어)
```

### 케어 완료 기록
```
PUT  /api/users/{userCode}/care-completions          ← 케어 완료 기록
GET  /api/users/{userCode}/care-completions          ← 일자별 완료 조회
GET  /api/users/{userCode}/care-completions/history  ← 이력
GET  /api/users/{userCode}/care-completions/summary  ← 순응도 요약
```

### 기타
```
GET  /api/products                     ← 제품 카탈로그 (category, concern 필터)
GET  /api/products/{productCode}       ← 제품 상세
GET  /api/facilities                   ← 시설 목록
GET  /api/innerderma/health            ← 서버 상태
```

---

## 인증 플로우

```
1. POST /api/auth/register → { token, userCode, name }
2. 이후 모든 /api/users/** 요청에 헤더 추가:
   Authorization: Bearer <token>
```

토큰 유효 시간: 24시간. 만료되면 `POST /api/auth/token?userCode=`으로 재발급.

---

## 프론트엔드 빌드 & 배포 방법

### 서버에 SSH 접속

```bash
ssh -i <키파일> ubuntu@1.201.116.161
```

### 첫 배포 (최초 1회)

```bash
sudo mkdir -p /opt/innerderma-frontend && sudo chown ubuntu:ubuntu /opt/innerderma-frontend
git clone https://github.com/dunsan1008/innerderma_front.git /opt/innerderma-frontend
cd /opt/innerderma-frontend
npm install
npm run build
```

### 업데이트 배포

```bash
cd /opt/innerderma-frontend
git pull origin main
npm install
npm run build
cd /opt/innerderma
docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml restart frontend
```

> `npm run build` 결과물이 `dist/` 폴더에 생성되면, nginx가 자동으로 서빙합니다.
> nginx restart는 캐시 갱신을 위해 필요합니다.

---

## 환경변수 (프론트엔드 빌드 시)

`.env.production` 또는 빌드 설정에서:

```env
VITE_API_BASE_URL=https://inner-derma.duckdns.org/api
# 또는 상대경로로:
VITE_API_BASE_URL=/api
```

> 같은 도메인에서 서빙되므로 `/api`로 상대경로 사용이 가장 깔끔합니다.
> CORS 이슈 없음.

---

## 로컬 개발 시

백엔드를 로컬에서 돌리거나, 프로덕션 API를 직접 호출할 수 있습니다:

```env
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api
# 또는 프로덕션 직접 호출 (CORS 허용됨):
VITE_API_BASE_URL=https://inner-derma.duckdns.org/api
```

---

## 빌드 결과물 구조 (nginx 서빙)

```
/opt/innerderma-frontend/dist/
  ├── index.html        ← SPA 진입점
  ├── assets/
  │   ├── index-xxx.js
  │   └── index-xxx.css
  └── ...
```

nginx 설정 (`/opt/innerderma/deploy/nginx/active.conf`):
```nginx
location / {
    root /usr/share/nginx/html/innerderma;
    try_files $uri /index.html;
}
```

Docker compose에서 이 경로를 마운트:
```yaml
volumes:
  - ${FRONTEND_BUILD_PATH:-./frontend-dist}:/usr/share/nginx/html/innerderma:ro
```

`/opt/innerderma.env`에서:
```
FRONTEND_BUILD_PATH=/opt/innerderma-frontend/dist
```

---

## 주의사항

1. **빌드 후 반드시 nginx restart** — 빌드만 하면 Docker 볼륨이 실시간 반영되지만, 브라우저 캐시 문제 방지를 위해 restart 권장
2. **SPA 라우팅** — nginx `try_files $uri /index.html`로 설정됨, 프론트 라우팅은 자유롭게 사용 가능
3. **API 호출 시 `/api` prefix** — 모든 백엔드 API는 `/api`로 시작
4. **이미지 업로드** — `multipart/form-data`로 전송, 최대 10MB
5. **locale 파라미터** — AI Care 호출 시 `?locale=ko` 등으로 언어 지정 가능

---

## 문제 발생 시

| 증상 | 해결 |
|---|---|
| 404 on page refresh | nginx `try_files` 확인 — `docker compose restart frontend` |
| CORS 에러 | 백엔드 CORS 설정 확인 — 현재 `https://inner-derma.duckdns.org` 허용됨 |
| API 401 Unauthorized | JWT 토큰 만료 — 재발급 필요 |
| 빌드 후 변경 안 보임 | 브라우저 하드 리프레시 (Ctrl+Shift+R) + `docker compose restart frontend` |
| nginx 502 | 백엔드 서버 다운 — `docker compose logs app-api` 확인 |

---

## 연락처

- 백엔드/인프라 문의: 서버 관리자에게
- Swagger UI에서 API 직접 테스트 가능: https://inner-derma.duckdns.org/swagger-ui/index.html
