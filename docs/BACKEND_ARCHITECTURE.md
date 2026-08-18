# InnerDerma 백엔드 설계 및 API 정리

## 1. 기술 구성

- Java 21
- Spring Boot / Spring MVC
- Spring Data JPA
- Jakarta Validation
- 운영 데이터베이스: MySQL
- 테스트 데이터베이스: H2
- 사진 저장소: 서버 로컬 파일 시스템
- 외부 분석 연동: SkinAge HTTP API

애플리케이션은 기능별로 `api`, `application`, `domain`, 필요 시 `infrastructure` 계층을 분리한다.

```text
Controller(API)
  -> Application Service
  -> Domain Entity / Repository
  -> MySQL
```

## 2. 핵심 업무 흐름

```text
사용자 조회
  -> 피부 사진 촬영
  -> 자가 피부 상태 입력
  -> SkinAge 피부 분석
  -> 케어 사이클 생성
  -> 규칙 기반 케어 솔루션 생성
  -> 제품 추천
  -> 아침/귀가 후 케어 제공
  -> 수행 여부 기록
  -> 캘린더, 이력, 수행률 조회
```

케어는 달력의 하루만을 기준으로 하지 않는다. 사진을 촬영한 날의 `EVENING` 케어와 다음 날의 `MORNING` 케어를 하나의 사이클로 연결한다. 사진을 새로 찍지 않은 날에는 최근 솔루션을 승계한다.

## 3. API 엔드포인트

모든 정상 응답은 공통 `ApiResponse<T>` 형식으로 반환된다.

### 상태 및 기준 정보

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/innerderma/health` | 서버 상태 확인 |
| `GET` | `/api/users/{userCode}` | 사용자 기본정보 조회 |
| `GET` | `/api/facilities` | 시설 목록 조회 |
| `GET` | `/api/users/{userCode}/procedures?facilityCode={code}&date={date}` | 시설과 날짜에 해당하는 시술 기록 조회 |
| `GET` | `/api/users/{userCode}/procedures/treatment-context?date={date}` | 지정일에 적용되는 최신 검증 시술 컨텍스트 조회 |
| `GET` | `/api/users/{userCode}/skin-diagnosis` | 최신 WHS 피부 진단 조회 |
| `GET` | `/api/ai-rules` | 현재 활성화된 버전별 AI 규칙 목록 조회 |

### 피부 사진

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/users/{userCode}/skin-captures` | `multipart/form-data`의 `file` 항목으로 사진 업로드 |
| `GET` | `/api/users/{userCode}/skin-captures/latest` | 최신 촬영 기록 조회 |
| `GET` | `/api/users/{userCode}/skin-captures/today` | 오늘 촬영 여부와 촬영 가능 상태 조회 |

파일 크기는 최대 10MB이며 `SKIN_CAPTURE_STORAGE_PATH` 또는 기본 `./data/skin-captures` 경로에 저장한다.

### 자가 피부 점검

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/users/{userCode}/self-checks` | 피부 증상 심각도와 메모 저장 |
| `GET` | `/api/users/{userCode}/self-checks/latest` | 최신 자가 점검 조회 |

점검 항목은 통증, 열감, 당김, 건조, 가려움, 부기, 각질, 트러블이며 결과에 안전 주의 필요 여부가 포함된다.

### SkinAge 분석

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/users/{userCode}/skin-analyses` | 촬영 사진을 SkinAge로 분석 |
| `GET` | `/api/users/{userCode}/skin-analyses/latest` | 최신 피부 분석 조회 |

요청 예시:

```json
{
  "captureId": 1,
  "actualAge": 30
}
```

### 케어 사이클

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/users/{userCode}/care-cycles` | 최신 분석과 자가 점검을 바탕으로 케어 사이클 생성 |
| `GET` | `/api/users/{userCode}/care-cycles/daily?date={date}` | 지정 날짜에 적용되는 케어 사이클 조회 |

응답에는 원본 촬영일, 저녁·아침 케어 날짜, 승계 여부와 안전 주의 여부가 포함된다.

### 케어 솔루션

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/users/{userCode}/care-solutions` | 케어 사이클 기반 규칙형 솔루션 생성 |
| `GET` | `/api/users/{userCode}/care-solutions/daily?date={date}` | 날짜별 적용 솔루션 조회 |

생성할 때 선택적으로 `careCycleId`를 전달할 수 있다. 결과에는 계절, 안전 수준, 주요 고민, 아침·저녁 단계, WHS 진단과 시술 관리 정보가 포함된다.

### 제품 추천

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/products` | 활성 제품 목록 조회 |
| `GET` | `/api/users/{userCode}/product-recommendations/daily?date={date}` | 적용 솔루션을 기준으로 날짜별 제품 추천 조회 |

제품 추천은 현재 솔루션과 제품 카탈로그를 조합하여 조회 시 계산하며, 안전 주의 상태에서는 호환 제품을 필터링한다.

### 오늘의 통합 케어

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/users/{userCode}/daily-care?date={date}` | 아침·귀가 후 케어, 제품 추천, 수행 상태 통합 조회 |

프론트엔드의 일일 케어 화면에서 사용할 중심 조회 API다. 단계별 솔루션, 관리 순서, 추천 제품, 안전 안내, 수행 상태를 함께 제공한다.

### 케어 이력

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/users/{userCode}/care-history?from={date}&to={date}` | 기간별 케어 진행 이력 조회 |
| `GET` | `/api/users/{userCode}/care-history/{date}` | 특정 날짜의 케어 상세 조회 |

촬영, 분석, 사이클 생성, 솔루션 생성 진행 상태를 구분한다.

### 케어 수행 기록

| Method | Endpoint | 설명 |
|---|---|---|
| `PUT` | `/api/users/{userCode}/care-completions` | 날짜와 단계별 완료 상태 저장 또는 수정 |
| `GET` | `/api/users/{userCode}/care-completions?date={date}` | 날짜별 수행 상태 조회 |
| `GET` | `/api/users/{userCode}/care-completions/history?from={date}&to={date}` | 기간별 수행 기록 조회 |
| `GET` | `/api/users/{userCode}/care-completions/summary?from={date}&to={date}` | 기간별 수행률 요약 |

저장 요청 예시:

```json
{
  "servedDate": "2026-08-17",
  "phase": "EVENING",
  "completed": true
}
```

단계는 `MORNING`과 `EVENING`으로 구분한다. 같은 사용자, 날짜, 단계의 상태는 중복 추가하지 않고 갱신한다.

## 4. 주요 도메인

- `User`
- `Facility`
- `ProcedureRecord`
  - 기존 `procedureName`, `careGuide` 조회 계약을 유지한다.
  - Treatment Context는 nullable 시술 코드·유형·부위, nullable 예상 회복일 최소/최대, 정상/경고 증상, 사후 제한, 허용/제한 제품 태그, nullable 출처·규칙 버전으로 저장한다.
  - 컨텍스트 조회는 지정일 이하 기록 중 가장 최신 시술을 사용하여 `daysSinceTreatment`를 계산한다. 미래 시술은 제외하며, 원본에 없는 의학적 값은 채우지 않는다.
- `WhsSkinDiagnosis` (WHS 초기 Baseline)
  - `WhsSkinDiagnosisMetric`: 진단당 항목별 1건. 피부 나이, 이마·눈꼬리·눈밑 주름, 색소, 피부 균일도, 여드름, 블랙헤드, 다크서클, 눈처짐, 모공을 지원한다.
  - `userScore`와 `averageScore`는 nullable 원본값이며 서비스가 평균·차이·종합점수를 추론해 저장하지 않는다. `grade`는 `EXCELLENT`, `NORMAL`, `NEEDS_IMPROVEMENT` 또는 미제공(null)이다.
- `SkinCapture`
- `SelfCheck`
- `SkinAnalysis`
- `CareCycle`
- `CareSolution`
- `Product`
- `CareCompletion`

주요 연결 구조는 다음과 같다.

```text
User
 ├─ SkinCapture ─ SkinAnalysis ─ CareCycle ─ CareSolution
 ├─ SelfCheck ─────────────────────┘
 ├─ WhsSkinDiagnosis ─────────────────────────┐
 ├─ ProcedureRecord ──────────────────────────┤
 └─ CareCompletion ───────────────────────────┘
```

### WHS 피부 진단 응답

`GET /api/users/{userCode}/skin-diagnosis`는 기존 `resultSummary`를 호환용으로 유지하고, 의사결정용 데이터는 `metrics` 배열로 반환한다. 각 metric은 `metricType`, nullable `userScore`, nullable `averageScore`, nullable `grade`를 가진다. 주름은 위치별 metric으로 분리되며 단일 주름 점수로 합산하지 않는다.

## 5. 공통 처리

- 정상 응답: `common/response/ApiResponse`
- 업무 예외: `BusinessException`과 `ErrorCode`
- 전역 오류 변환: `GlobalExceptionHandler`
- 날짜가 선택사항인 일일 조회는 서비스에서 오늘 날짜를 기본값으로 사용
- 테스트는 JUnit과 H2를 사용

## 6. 현재 범위와 남은 운영 기반

현재 구현된 범위는 피부 촬영부터 분석, 케어 생성, 제품 추천, 일일 수행과 이력 관리까지의 핵심 사용자 흐름이다.

아직 코드에서 확인되지 않는 운영 기능은 다음과 같다.

- 사용자 인증과 권한 관리
- 사용자·시설·시술·제품 관리용 생성 및 수정 API
- 클라우드 객체 스토리지
- Flyway/Liquibase 기반 DB 마이그레이션
- OpenAPI/Swagger 문서 자동화
- 다국어 처리 기반

AI Master Rule 적용을 위한 버전형 Rule DB 기반이 추가되었으며, 초기 규칙으로 Safety First, Image Quality Gate, Minimum Intervention을 관리한다. 실제 상태·추세 계산과 전체 Rule Engine 실행기는 후속 단계에서 연결한다.

## 7. 주요 코드 위치

- 애플리케이션 설정: `src/main/resources/application.properties`
- 의존성과 빌드: `build.gradle`
- 공통 응답과 오류: `src/main/java/com/innerderma/common`
- 오늘의 통합 케어: `src/main/java/com/innerderma/dailycare`
- 케어 사이클: `src/main/java/com/innerderma/carecycle`
- 케어 솔루션: `src/main/java/com/innerderma/caresolution`
- 제품 추천: `src/main/java/com/innerderma/productrecommendation`
- 수행 기록: `src/main/java/com/innerderma/carecompletion`
