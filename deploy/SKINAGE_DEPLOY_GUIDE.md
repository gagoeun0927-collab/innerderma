# SkinAge 서버 배포 가이드

InnerDerma 백엔드가 SkinAge API (`POST /api/v1/analyze`)를 호출하여 피부 분석을 수행합니다.
SkinAge는 별도 레포/프로젝트로 관리되므로, 이 문서는 InnerDerma와 연동하기 위한 배포 요구사항을 정리합니다.

## 1. InnerDerma가 기대하는 SkinAge 스펙

| 항목 | 값 |
|---|---|
| 엔드포인트 | `POST /api/v1/analyze` |
| Content-Type | `multipart/form-data` |
| 필수 파라미터 | `file` (이미지 바이너리) |
| 선택 파라미터 | `age` (Integer), `include_heatmaps` (Boolean) |
| 응답 | JSON (summary, zone_scores, aggregate_metrics, metadata) |
| 포트 | 8000 (기본) |
| Health Check | `GET /health` (또는 `/api/v1/health`) |

## 2. 배포 옵션

### Option A: Docker 컨테이너 (권장)

SkinAge 레포에 Dockerfile이 있다면:

```bash
# 서버에서
cd /opt/skinage
git clone <skinage-repo-url> .
docker build -t innerderma/skinage:latest .
```

InnerDerma의 `docker-compose.prod.yml`에서 `skinage` 서비스가 이 이미지를 사용합니다:
```yaml
skinage:
  image: innerderma/skinage:latest
```

### Option B: 같은 서버에서 Python venv로 직접 실행

```bash
cd /opt/skinage
git clone <skinage-repo-url> .
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 서비스로 등록
sudo tee /etc/systemd/systemd/skinage.service > /dev/null <<'EOF'
[Unit]
Description=SkinAge AI Analysis Server
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/skinage
ExecStart=/opt/skinage/venv/bin/uvicorn src.main:app --host 0.0.0.0 --port 8000
Restart=always
RestartSec=5
Environment=PYTHONPATH=/opt/skinage

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable skinage
sudo systemctl start skinage
```

이 경우 `docker-compose.prod.yml`에서 skinage 서비스를 제거하고,
`app-api` 환경변수를 변경합니다:
```
SKINAGE_BASE_URL=http://host.docker.internal:8000
# 또는 Docker network mode=host를 사용하는 경우:
SKINAGE_BASE_URL=http://localhost:8000
# 또는 서버 내부 IP:
SKINAGE_BASE_URL=http://172.17.0.1:8000
```

### Option C: Docker Compose에 직접 빌드 포함

SkinAge 레포를 서버에 클론하고 docker-compose에서 빌드:

```yaml
# docker-compose.prod.yml의 skinage 서비스를 아래로 교체:
skinage:
  build:
    context: /opt/skinage
    dockerfile: Dockerfile
  restart: unless-stopped
  expose:
    - "8000"
```

## 3. InnerDerma 연동 확인

배포 후 확인:

```bash
# SkinAge 단독 테스트
curl -X POST http://localhost:8000/api/v1/analyze \
  -F "file=@test-face.jpg" \
  -F "age=25"

# 기대 응답 (요약):
# {"summary":{"predicted_skin_age":...,"overall_score":...,"skin_health_grade":"..."}, ...}
```

InnerDerma에서 연동 확인:
```bash
# 사진 업로드 + 분석 원스텝 API 호출
curl -X POST http://localhost:8080/api/users/WHS-DEMO-001/skin-captures/analyze \
  -H "Authorization: Bearer <token>" \
  -F "file=@test-face.jpg" \
  -F "actualAge=25"
```

## 4. 네트워크 구성

```
┌─────────────────────────────────────────────┐
│                  Server                      │
│                                             │
│  ┌──────────┐    :8000    ┌──────────┐     │
│  │ app-api  │ ──────────▶ │ skinage  │     │
│  │ (Spring) │             │ (FastAPI)│     │
│  └──────────┘             └──────────┘     │
│       ▲                                     │
│       │ :8080                               │
│  ┌──────────┐                               │
│  │  nginx   │ :80/:443 ◀── Internet        │
│  └──────────┘                               │
│       │                                     │
│  ┌──────────┐                               │
│  │  MySQL   │ :3306 (internal only)        │
│  └──────────┘                               │
└─────────────────────────────────────────────┘
```

## 5. 주의사항

- SkinAge는 **외부에 노출하지 않음** — nginx에서 proxy 안 함, Docker expose만 (ports 아님)
- GPU가 없어도 CPU 모드로 동작 (추론 시간 ~500ms → 가비아 일반 서버에서도 OK)
- SkinAge가 다운되어도 InnerDerma 백엔드는 에러를 graceful하게 처리 (`SKINAGE_API_UNAVAILABLE`)
- 모델 파일이 크면 (>1GB) 서버 디스크 확인 필요 — `/opt/skinage/models/` 용량 체크

## 6. 환경변수 요약

InnerDerma 측 (`/opt/innerderma.env`):
```
SKINAGE_BASE_URL=http://skinage:8000    # Docker Compose 내부 (Option A/C)
# 또는
SKINAGE_BASE_URL=http://172.17.0.1:8000 # venv 직접 실행 (Option B)
```

SkinAge 측 (필요 시):
```
MODEL_PATH=/opt/skinage/models/
HOST=0.0.0.0
PORT=8000
```
