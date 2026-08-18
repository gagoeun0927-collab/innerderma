# Codex/OMX → Kiro 완전 인수인계 문서

> 작성일: 2026-08-18
> 작성 기준: 실제 코드 전수 확인 + Git 이력 + OMX 작업 로그 + 기획서 원문 + 이해관계자 코멘트
> 검증 시각: 전체 테스트 재실행 2026-08-18 02:09 (KST), 원격 fetch 및 DB 연결 확인 동일 시점

이 문서는 추측을 배제하고 **직접 확인한 사실만** 기록한다. 확인하지 못한 항목은 `미검증`으로 명시했다.

---

## 1. 프로젝트 목적

InnerDerma는 AAC의 오프라인 시설에서 피부 분석 또는 분석+시술을 받은 **해외 고객이 귀국한 뒤에도 홈케어를 이어갈 수 있게 하는 사후관리 백엔드**다. 기존 WHS 앱을 대체하는 별도 앱이 아니라, WHS 앱에 연결되는 기능으로 정의되어 있다.

```text
WHS 초기 피부 진단
+ 귀국 후 스마트폰 사진 분석(SkinAge)
+ 사용자 자가 문진
+ 클리닉 시술 기록 및 주의사항
+ 계절
= 귀가 후(EVENING) 케어 + 다음 날 기상 후(MORNING) 케어
```

### 시설 용어 (최종 기획서 기준)

| 명칭 | 역할 |
|---|---|
| 웰니스 하우스 서울 (WHS) | 오프라인 피부 분석 시설 |
| 더나클리닉 (DERNA) | 피부 분석 및 시술이 가능한 AAC 계열 클리닉 |
| 엠레드의원 (AMRED) | 피부 분석 및 시술이 가능한 AAC 계열 클리닉 |
| WHS 앱 | 기존 앱. 초기 진단·시술 내역·케어 가이드 제공 |
| Pith | AMRED 노하우 기반 AAC 스킨케어 브랜드 |
| WHS Store | Pith 포함 스킨케어·디바이스 큐레이션 |
| 윔스토어 (WIM) | AAC 산하 식품·웰니스 판매 사업 |

---

## 2. 전체 아키텍처

Java 21 / Spring Boot 4.0.8-SNAPSHOT / Spring MVC / Spring Data JPA + Hibernate / Gradle 9.5.1 단일 모듈. 운영 DB는 MySQL 8, 테스트는 H2(`create-drop`). Jackson은 3 계열(`tools.jackson.*`)이며 애노테이션만 `com.fasterxml.jackson.annotation`을 사용한다.

```text
Controller (api)
  → Application Service (application)
  → Domain Entity / Repository (domain)
  → MySQL
외부 연동만 infrastructure 계층 추가
```

도메인 17개 패키지가 예외 없이 이 구조를 따른다.

```text
com.innerderma
├─ common          config(DemoDataInitializer) / error / health / response
├─ user, facility, procedure, skindiagnosis        기준 정보
├─ skincapture, selfcheck, skinanalysis            일일 입력
├─ carecycle, caresolution, productrecommendation  케어 생성
├─ dailycare, carehistory, carecompletion          제공·기록
├─ product                                        제품 카탈로그
└─ airule                                         AI 규칙 저장소
```

### 구성 규모 (전수 확인값)

| 구분 | 개수 |
|---|---|
| Controller | 16 |
| REST 엔드포인트 | 27 |
| Entity | 13 |
| Repository | 12 |
| Service | 15 |
| Enum | 11 |
| ErrorCode | 20 |
| 물리 테이블(코드 기준) | 18 (엔티티 13 + `@ElementCollection` 5) |

---

## 3. 현재 브랜치와 Git 상태

```text
원격:        https://github.com/gagoeun0927-collab/innerderma
현재 브랜치: feature/treatment-context
현재 커밋:   3ad814a  feat: add treatment context
워킹트리:    clean (미커밋 변경 없음)
원격 동기화: origin/feature/treatment-context 와 동일 (ahead/behind 0)
main:        f528743  docs: persist concise change reporting preference
origin/main: f528743 (로컬과 동일)
```

`main`에 병합되지 않은 브랜치는 **2개뿐**이다.

```text
main f528743
 └─ 68441eb  feat: add versioned AI rule foundation
     └─ 5f2dac0  feat: structure WHS skin diagnosis metrics   ← feature/ai-rule-foundation
         └─ 3ad814a  feat: add treatment context              ← feature/treatment-context (HEAD)
```

선형 누적이므로 `feature/treatment-context`를 `main`에 fast-forward 병합하면 세 커밋이 한 번에 반영된다.

### 브랜치 특이사항

- `feature/i18n-foundation` = `feature/care-history`와 **동일 커밋(`73dbf59`)**. 코드가 한 줄도 없는 빈 브랜치이며 원격 추적도 없다. OMX 로그에도 "브랜치만 생성됐고 커밋은 없다"고 기록되어 있다.
- `integration/backend-features` = `24451d9`. 8/17에 20개 기능 브랜치를 `main`에 fast-forward 병합할 때 경유한 로컬 전용 브랜치.
- 원격 브랜치 22개. `git fetch --all --prune` 결과 **다른 작업자의 새 커밋 없음**.

---

## 4. 실행 검증 결과 (2026-08-18)

### 4-1. 원격 저장소 — 통과

`git fetch --all --prune` 실행 후 `.git/FETCH_HEAD`로 확인. 원격 22개 브랜치를 가져왔고 로컬과 차이가 없다.

### 4-2. 전체 테스트 재실행 — 통과

```powershell
.\gradlew.bat cleanTest test --console=plain --no-daemon
→ BUILD SUCCESSFUL in 34s
```

- 테스트 클래스 **22개**, 테스트 메서드 **54개**
- **failures 0, errors 0** (결과 XML 전수 검색으로 확인)
- `InnerDermaApplicationTests.contextLoads()` 포함 통과 → Spring 컨텍스트 정상 로딩. 실행 로그에 HikariPool 초기화·종료, JPA EntityManagerFactory 생성이 실제로 남았다.
- 첫 실행은 `:test UP-TO-DATE`로 스킵됐다. Gradle이 소스 입력 해시가 이전 실행과 동일하다고 판정한 것이므로, **코드가 마지막 통과 시점 이후 변경되지 않았다는 근거**가 된다. 이후 `cleanTest`로 강제 재실행하여 실제 통과를 확인했다.

### 4-3. DB 연결 — 연결은 통과, 실기동은 미실행

확인된 사실:

```text
MySQL80 서비스        Running
포트 3306             LISTENING
DB_PASSWORD 환경변수  설정됨
계정 innerderma_app   인증 성공 (SELECT 1 → CONNECT_OK)
대상 스키마 innerderma 존재
```

**중요 발견 — MySQL 스키마가 코드보다 크게 뒤처져 있다.**

실제 `innerderma` DB에 존재하는 테이블은 **4개뿐**이다.

```text
facilities            (약 2행)
procedure_records     (0행)
users                 (0행)
whs_skin_diagnoses    (0행)
```

코드가 요구하는 18개 중 **14개가 없다**: `whs_skin_diagnosis_metrics`, `skin_captures`, `self_checks`, `skin_analyses`, `care_cycles`, `care_solutions`, `products`, `care_completions`, `ai_rules`, `procedure_normal_symptoms`, `procedure_warning_symptoms`, `procedure_aftercare_restrictions`, `procedure_allowed_product_tags`, `procedure_restricted_product_tags`.

해석: 8/17 오전 정보수집 단계에서 `bootRun`을 한 번 실행해 4개 테이블이 생성된 뒤, 이후 모든 기능은 **H2 테스트로만 검증되었고 MySQL로 기동한 적이 없다**. `facilities`가 2행인 것도 당시 시설이 2개였던 시점의 잔여 데이터로 보인다(현재 코드는 WHS/DERNA/AMRED 3개를 생성).

`bootRun`을 실행하지 않은 이유: `spring.jpa.hibernate.ddl-auto=update`이므로 기동 시 **14개 테이블이 자동 생성**되고, `DemoDataInitializer`와 `AiRuleInitializer`가 데모 사용자·시설·진단·시술·제품 7건·AI 규칙 3건을 **삽입**한다. 이는 "DB 데이터 변경 금지" 지시에 해당하므로 승인 없이 진행하지 않았다.

**미검증 항목**: MySQL 기준 애플리케이션 기동(`bootRun`), SkinAge 서버 연동(`SKINAGE_BASE_URL` 미설정, 기본값 `http://localhost:8000`).

---

## 5. 구현 완료 기능과 주요 파일

### 5-1. 공통 기반

| 파일 | 역할 |
|---|---|
| `common/response/ApiResponse.java` | 모든 성공 응답 `{success, data}` |
| `common/error/ErrorCode.java` | 오류 코드 20종 |
| `common/error/BusinessException.java` | 업무 예외 |
| `common/error/ErrorResponse.java` | 실패 응답 `{success, code, message, errors}` |
| `common/error/GlobalExceptionHandler.java` | 전역 변환. 날짜·enum·JSON 오류를 500이 아닌 `COMMON_001` 400으로 처리 |
| `common/config/DemoDataInitializer.java` | 더미 사용자·시설 3곳·진단·시술·데모 제품 7건 삽입 |
| `common/health/HealthController.java` | 헬스 체크 |

### 5-2. 기준 정보

| 기능 | 주요 파일 |
|---|---|
| 사용자 | `user/{domain/User, domain/UserRepository, application/UserService, api/UserController, api/UserResponse}` |
| 시설 | `facility/{Facility, FacilityRepository, FacilityService, FacilityController, FacilityResponse}` |
| WHS 진단 | `skindiagnosis/domain/{WhsSkinDiagnosis, WhsSkinDiagnosisMetric, SkinDiagnosisMetricType, SkinDiagnosisGrade, WhsSkinDiagnosisRepository}`, `application/WhsSkinDiagnosisService`, `api/{WhsSkinDiagnosisController, WhsSkinDiagnosisResponse, WhsSkinDiagnosisMetricResponse}` |
| 시술 + Treatment Context | `procedure/domain/{ProcedureRecord, ProcedureRecordRepository}`, `application/{ProcedureRecordService, TreatmentContext}`, `api/{ProcedureRecordController, ProcedureRecordResponse, TreatmentContextResponse}` |

### 5-3. 일일 입력

| 기능 | 주요 파일 |
|---|---|
| 사진 촬영 | `skincapture/domain/{SkinCapture, SkinCaptureQualityStatus, SkinCaptureRepository}`, `application/{SkinCaptureService, SkinCaptureFile, SkinCaptureStorage, DailyCaptureStatus}`, `infrastructure/LocalSkinCaptureStorage`, `api/{SkinCaptureController, SkinCaptureResponse, DailyCaptureStatusResponse}` |
| 자가 문진 | `selfcheck/domain/{SelfCheck, SymptomSeverity, SelfCheckRepository}`, `application/{SelfCheckService, SelfCheckCommand}`, `api/{SelfCheckController, SelfCheckRequest, SelfCheckResponse}` |
| SkinAge 분석 | `skinanalysis/domain/{SkinAnalysis, SkinAnalysisRepository}`, `application/{SkinAnalysisService, SkinAgeClient, SkinAgeAnalysisResult, SkinAnalysisResult}`, `infrastructure/HttpSkinAgeClient`, `api/{SkinAnalysisController, SkinAnalysisRequest, SkinAnalysisResponse}` |

### 5-4. 케어 생성·제공·기록

| 기능 | 주요 파일 |
|---|---|
| 케어 사이클 | `carecycle/domain/{CareCycle, CareCycleRepository}`, `application/{CareCycleService, CareCycleResult}`, `api/{CareCycleController, CareCycleResponse}` |
| 케어 솔루션 | `caresolution/domain/{CareSolution, CareSeason, SafetyLevel, CareSolutionRepository}`, `application/{CareSolutionService, CareSolutionResult}`, `api/{CareSolutionController, CareSolutionRequest, CareSolutionResponse}` |
| 제품 카탈로그 | `product/domain/{Product, ProductCategory, ProductConcern, ProductRepository}`, `application/ProductService`, `api/{ProductController, ProductResponse}` |
| 제품 추천 | `productrecommendation/application/{ProductRecommendationService, ProductRecommendationItem, ProductRecommendationResult}`, `api/{ProductRecommendationController, ProductRecommendationResponse}` |
| 오늘의 통합 케어 | `dailycare/application/{DailyCareService, DailyCareResult, DailyCarePhaseResult}`, `api/{DailyCareController, DailyCareResponse}` |
| 케어 이력 | `carehistory/application/{CareHistoryService, CareHistoryItem, CareHistoryResult, DailyCareHistoryItem, DailyCareHistoryResult, CarePhase, CareProgressStatus}`, `api/{CareHistoryController, CareHistoryResponse, DailyCareHistoryResponse}` |
| 수행 기록 | `carecompletion/domain/{CareCompletion, CareCompletionRepository}`, `application/{CareCompletionService, CareCompletionHistoryResult, CareAdherenceSummary}`, `api/{CareCompletionController, CareCompletionRequest, CareCompletionResponse, CareCompletionHistoryResponse}` |
| AI 규칙 저장소 | `airule/domain/{AiRule, AiRuleCategory, AiRuleRepository}`, `application/AiRuleService`, `config/AiRuleInitializer`, `api/{AiRuleController, AiRuleResponse}` |

---

## 6. API 구조 (엔드포인트 27개)

성공 응답은 전부 `{"success": true, "data": {...}}`, 실패는 `{"success": false, "code": "...", "message": "...", "errors": {}}`.

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/api/innerderma/health` | 서버 상태 |
| GET | `/api/users/{userCode}` | 사용자 조회 |
| GET | `/api/facilities` | 시설 목록 |
| GET | `/api/users/{userCode}/skin-diagnosis` | 최신 WHS 진단 + metrics |
| GET | `/api/users/{userCode}/procedures?facilityCode&date` | 시설·일자별 시술 기록 |
| GET | `/api/users/{userCode}/procedures/treatment-context?date` | 지정일 기준 최신 시술 컨텍스트 + 경과일 |
| POST | `/api/users/{userCode}/skin-captures` | 사진 업로드 (multipart, part명 `file`) |
| GET | `/api/users/{userCode}/skin-captures/latest` | 최신 촬영 |
| GET | `/api/users/{userCode}/skin-captures/today` | 오늘 촬영 여부·재촬영 가능 상태 |
| POST | `/api/users/{userCode}/self-checks` | 자가 문진 저장 |
| GET | `/api/users/{userCode}/self-checks/latest` | 최신 자가 문진 |
| POST | `/api/users/{userCode}/skin-analyses` | SkinAge 분석 실행 |
| GET | `/api/users/{userCode}/skin-analyses/latest` | 최신 분석(외부 재호출 없음) |
| POST | `/api/users/{userCode}/care-cycles` | 케어 사이클 생성 |
| GET | `/api/users/{userCode}/care-cycles/daily?date` | 날짜별 사이클(승계 포함) |
| POST | `/api/users/{userCode}/care-solutions` | 규칙 기반 솔루션 생성 |
| GET | `/api/users/{userCode}/care-solutions/daily?date` | 날짜별 솔루션 |
| GET | `/api/products` | 활성 제품 목록 |
| GET | `/api/users/{userCode}/product-recommendations/daily?date` | 날짜별 제품 추천 |
| GET | `/api/users/{userCode}/daily-care?date` | **프론트 메인 진입점.** 단계별 솔루션+제품+수행상태 통합 |
| GET | `/api/users/{userCode}/care-history?from&to` | 기간 케어 이력 |
| GET | `/api/users/{userCode}/care-history/{date}` | 날짜별 상세(MORNING/EVENING) |
| PUT | `/api/users/{userCode}/care-completions` | 수행 체크 저장·갱신 |
| GET | `/api/users/{userCode}/care-completions?date` | 날짜별 수행 상태 |
| GET | `/api/users/{userCode}/care-completions/history?from&to` | 기간 수행 기록 |
| GET | `/api/users/{userCode}/care-completions/summary?from&to` | 기간 수행률 요약 |
| GET | `/api/ai-rules` | 활성 AI 규칙 목록 |

### 오류 코드 20종

```text
COMMON_001  잘못된 요청 (날짜·enum·JSON 파싱 오류 포함)
COMMON_002  내부 서버 오류
USER_001    사용자 없음
SKIN_001    WHS 진단 없음
PROCEDURE_001         시술 기록 없음
CAPTURE_001~004       지원하지 않는 이미지 / 오늘 이미 촬영 / 기록 없음 / 저장 실패
SELF_CHECK_001        자가 문진 없음
ANALYSIS_001~005      분석 없음 / 이미 존재 / 이미지 로드 불가 / SkinAge 연결 실패(502) / 응답 형식 오류(502)
CARE_CYCLE_001/002    사이클 없음 / 이미 존재
CARE_SOLUTION_001/002 솔루션 없음 / 이미 존재
CARE_HISTORY_001      해당 날짜 기록 없음
```

기간 조회는 기본 최근 30일, 최대 31일이며 초과 시 `COMMON_001`.

---

## 7. DB 구조

```text
users
 ├─ whs_skin_diagnoses ─ whs_skin_diagnosis_metrics
 ├─ procedure_records ─ facilities
 │    └─ procedure_{normal_symptoms | warning_symptoms | aftercare_restrictions
 │                  | allowed_product_tags | restricted_product_tags}
 ├─ skin_captures ─ skin_analyses ─ care_cycles ─ care_solutions ─ care_completions
 ├─ self_checks ──────────────────────┘
 └─ (독립) products, ai_rules
```

### 유니크 제약 (설계 의도가 담긴 부분)

| 제약 | 의미 |
|---|---|
| `skin_analyses.skin_capture_id` UNIQUE | 사진 1건당 분석 1건 |
| `care_cycles.skin_analysis_id` UNIQUE | 분석 1건당 사이클 1개 |
| `care_solutions.care_cycle_id` UNIQUE | 사이클 1건당 솔루션 1개 |
| `care_completions(user_id, served_date, phase)` UNIQUE | 중복 생성 대신 갱신 |
| `ai_rules(rule_id, version)` UNIQUE | 규칙 버전 관리 |
| `whs_skin_diagnosis_metrics(diagnosis_id, metric_type)` UNIQUE | 진단당 항목 1건 (애플리케이션에서도 예외 처리) |
| `skin_captures.image_path` UNIQUE | 이미지 경로 중복 방지 |

### 주요 컬럼

- `users`: `user_code`, `name`, `phone_number` — 타임스탬프 없음(MVP에서 의도적으로 제거)
- `care_cycles`: `origin_capture_date`, `evening_care_date`(=origin), `morning_care_date`(=origin+1일)
- `care_solutions`: `season`, `safety_level`, `headline`, `evening_steps_json`, `morning_steps_json`, `safety_message`, `primary_concern`
- `skin_analyses`: `overall_score`, `skin_health_grade`, `model_version`, `raw_result`(SkinAge 원본 JSON 전량)
- `procedure_records`: 기존 `procedure_name`/`care_guide` 호환 유지 + Treatment Context 컬럼 12개(전부 nullable/빈 컬렉션)
- `whs_skin_diagnosis_metrics`: `user_score`/`average_score`가 **nullable** — WHS가 주지 않은 값을 서비스가 만들지 않는다는 원칙을 스키마로 강제

### Enum 값

```text
CarePhase             MORNING, EVENING
CareProgressStatus    CAPTURED, ANALYZED, CYCLE_CREATED, SOLUTION_READY
CareSeason            SPRING(3~5), SUMMER(6~8), AUTUMN(9~11), WINTER(그 외)
SafetyLevel           NORMAL, ATTENTION
SymptomSeverity       NONE, MILD, MODERATE, SEVERE
SkinCaptureQualityStatus  VALID, QUALITY_CHECK_FAILED
SkinDiagnosisGrade    EXCELLENT, NORMAL, NEEDS_IMPROVEMENT
SkinDiagnosisMetricType   SKIN_AGE, FOREHEAD_WRINKLE, CROW_FEET_WRINKLE, UNDER_EYE_WRINKLE,
                          PIGMENTATION, SKIN_UNIFORMITY, ACNE, BLACKHEAD, DARK_CIRCLE,
                          EYE_SAGGING, PORE
ProductCategory       CLEANSER, MOISTURIZER, SUNSCREEN, TARGETED_CARE
ProductConcern        GENERAL, WRINKLE, PORE_TEXTURE, PIGMENTATION, REDNESS, ACNE,
                      BLACKHEAD, DARK_CIRCLE, EYE_SAGGING, SKIN_UNIFORMITY
AiRuleCategory        SAFETY, INPUT_IMAGE, SKIN_STATE, TREND, TREATMENT, PRIORITY_GOAL,
                      NIGHT_CARE, MORNING_CARE, PIECE_SEOUL, WIM_INNER_CARE, RESPONSE_UX
```

---

## 8. 주요 DTO

- `ApiResponse<T>` — `{success, data}` 공통 래퍼
- `ErrorResponse` — `{success, code, message, errors}`
- `SkinAgeAnalysisResult` — SkinAge 외부 응답. 중첩 record `Summary`, `ZoneScore`, `ConcernScore`, `AggregateMetrics`, `PriorityConcernItem`, `Heatmaps`, `Metadata`. snake_case를 `@JsonProperty`로 매핑
- `TreatmentContextResponse` — 시술 코드·유형·부위, 경과일, 회복일 min/max, 정상/경고 증상, 사후 제한, 허용/제한 제품 태그, 출처, 규칙 버전
- `WhsSkinDiagnosisResponse` — 호환용 `resultSummary` + `metrics[]`(metricType, nullable userScore/averageScore/grade)
- `DailyCareResponse` — `servedDate` + `phases[]`. 각 phase에 `originCaptureDate`, `inherited`, `safetyLevel`, `headline`, `steps[]`, `products[]`, `safetyMessage`, `productNotice`, `completionRecorded`, `completed`
- `CareSolutionResponse` — 계절·안전수준·헤드라인·우선 고민·저녁/아침 단계·WHS 진단 요약·시술명·시술 가이드
- `CareHistoryItem` — 촬영일, 저녁/아침 케어일, capture/analysis/cycle/solution ID, `progressStatus`, 계절, 안전수준, 자가문진 포함 여부
- `CareAdherenceSummary` — 기록/완료 수, 아침·저녁 분리 집계, `completionRatePercent`
- `SelfCheckRequest` — 8개 증상 `@NotNull` + `note` 최대 500자
- `SkinAnalysisRequest` — `captureId`(optional, `@Positive`), `actualAge`(optional, 0~120)

---

## 9. 주요 설계 결정과 이유

1. **케어의 시간 단위는 달력 하루가 아니라 2일 사이클.** 촬영일 저녁 + 다음 날 아침을 하나로 묶는다. 사용자마다 귀가·수면·기상 시각이 달라 자정 기준 하루가 실제 생활과 맞지 않기 때문이다. `CareCycle`이 이를 컬럼으로 고정한다.
2. **미촬영일 승계.** 새 사진이 없으면 최근 솔루션을 이어 쓰고 `inherited`와 `originCaptureDate`를 구분해 노출한다. 새 분석을 한 것처럼 표시하지 않는다.
3. **아침과 저녁의 기준 솔루션이 다르다.** 아침은 `date-1`까지의 최신 솔루션(당일 촬영 전에 수행하므로), 저녁은 `date`까지의 최신 솔루션. 그래서 새 사진을 찍은 날은 이전 솔루션의 아침 + 새 솔루션의 저녁이 함께 존재한다.
4. **정확한 시각을 요구하지 않는다.** `CarePhase.MORNING/EVENING`은 시각이 아니라 생활 순서 구분값이다. `care_completions.updated_at`은 이력 추적용이며 케어 적용 조건이 아니다.
5. **SkinAge 점수는 높을수록 건강.** `CareSolutionService.primaryConcern()`이 `concernAverages`의 **최솟값**을 우선 관리 항목으로 선택한다. 방향을 혼동하면 정반대 추천이 나온다.
6. **안전 우선순위 고정.** 시술 주의사항 → 자가 상태 안전 신호 → 사진 분석 → WHS 진단 → 계절. 계절은 안전·시술 규칙을 절대 덮어쓰지 않는 보조 단계다.
7. **`requiresSafetyAttention` 규칙.** 어느 항목이든 `SEVERE`이거나 통증·열감·붓기가 `MODERATE`이면 true. 의료 진단이 아니라 "일반 케어보다 안전 안내를 우선"하는 스위치다.
8. **원본에 없는 의학적 값은 생성하지 않는다.** WHS metric 점수와 Treatment Context 필드를 전부 nullable로 두고 평균·종합점수를 추론하지 않는다. 도메인 주석에도 명시되어 있다.
9. **AI는 정량 데이터 제공자, 비즈니스 로직은 InnerDerma.** SkinAge는 분석만 담당하고 안전 필터·제품·루틴은 백엔드가 결정한다.
10. **LLM은 나중에, 설명 전용으로만.** 현재 루틴은 결정적 규칙 기반이다.
11. **`Clock` 주입 패턴.** 시각 의존 서비스 전부에 package-private 생성자로 `Clock`을 받아 테스트에서 `Clock.fixed`로 고정한다.
12. **`open-in-view=false` + `@EntityGraph`.** 지연 로딩 엔티티를 Controller DTO로 변환할 때 발생한 오류를 Repository의 `@EntityGraph`로 해결했다.

---

## 10. 사용자가 요구했던 중요 정책·조건

코드만 봐서는 알 수 없는 항목이다. OMX 대화 로그와 `tmp/final_doc_review/` 산출물에서 확인했다.

### 10-1. 이해관계자 코멘트로 확정된 정책 (가장 중요)

`tmp/final_doc_review/comments.json`에 실명 코멘트가 남아 있다.

> **은정 최**, 2026-08-16T01:37
> "영양제는 아마 **의료법에 걸릴 수 있어서** 추천하는게 조심스러운 영역임. 그래서 식재료나 AAC 산하 사업인 **윔스토어**에서 판매하는 제품을 추천하는 방향으로"

이 코멘트가 최종 기획서에 반영된 내용(`tmp/final_doc_review/update_final_plan.py`의 치환 로직으로 확인):

- 13절 제목: "주간 제품 예외와 제품 추천" → "**주간 제품 예외와 제품·식생활 안내**"
- 빈도표의 "영양제 / 제품 기준 / 지정 요일·횟수" 행 → "**식재료·식습관 / 상시 안내 / 수분 섭취·균형 잡힌 식사 등 일반 안내**"
- 안전 원칙: "영양제는 복용 약물·알레르기·기저질환을 고려한다" → "**영양제를 직접 추천하거나 개인별 복용량·횟수·적합성을 판단하지 않는다.** 윔스토어 제품은 제조사 공식 설명·섭취 방법·주의사항을 그대로 제공하며, 의약품 복용 중이거나 알레르기·기저질환이 있는 사용자는 전문가와 상담하도록 안내한다"

`tmp/final_doc_review/artifact.md`에 검수 게이트가 명시되어 있다: "**영양제 추천 표현 잔존 0, 다국어 선택-only 표현 잔존 0**". `a11y.json`은 지적 0건, `final_comments.json`은 코멘트 0건(정리 완료).

**따라서 개인 맞춤 영양제 추천 기능은 만들면 안 된다.** 현재 `Product` 엔티티에 섭취·영양 개념이 없는 것은 우연이 아니라 의료법 리스크 회피 결정의 결과다.

### 10-2. 같은 편집에서 함께 확정된 사항

- **다국어를 선택 기능에서 MVP 필수로 승격** (16.2 신설). 한국어·영어·중국어(간체)·일본어
- 시설 3종 명확화 (WHS = 분석 시설, DERNA/AMRED = 분석+시술 클리닉, WHS 앱 = 기존 앱)
- 이용 경로 3가지 표 신설 + 구현 시 "피부 분석 사용자 / 피부 분석·시술 사용자" 구분, 분석 시설과 시술 시설을 **별도 필드**로 저장
- 품질 실패 사진은 별도 페이지로 이동시키지 않고 **촬영 화면 안에서 인라인 재촬영** 안내
- `UserPreference`(preferredLanguage, locale, timezone) 데이터 모델 추가
- 하루 1회 촬영 제한과 케어 날짜를 **사용자 현지 시간대** 기준으로 계산

### 10-3. OMX 대화에서 확정된 작업 방식

- 기능 1개 = 브랜치 1개 → 테스트 → 커밋 → 푸시. `main` 병합은 별도 판단
- 보고는 짧게. 코드·중간 과정 노출 금지, 완료 결과와 오류 유무만
- MVP는 더미 사용자 1명(`WHS-DEMO-001`)으로 진행, 실제 AAC/WHS 연동 없음
- 사용자는 시술명을 직접 입력하지 않는다. 시설과 시술일을 선택하면 서버가 시설 보유 기록을 조회한다
- AI 결과는 "의료 진단/처방"이 아니라 "피부 상태 분석 및 케어 안내"로 표현

---

## 11. 미완성 기능

### AI 파이프라인 (`docs/AI_IMPLEMENTATION_PROGRESS.md` 기준, 코드로 부재 확인)

| 항목 | 상태 |
|---|---|
| User Skin State Snapshot | 미구현. 엔티티 없음 |
| Trend Engine (IMPROVING/STABLE/WORSENING) | 미구현 |
| Rule Engine 실행기 | **미구현.** `AiRule`은 저장·조회만 되고 조건 JSON을 평가하는 코드가 없다 |
| Solution Object 마스터 스키마 + `applied_rules` | 미구현. 현재 `CareSolution`은 headline + steps JSON 문자열 2개 |
| LLM 설명 계층 | 미구현. LLM 호출 코드 전무 |
| Response Validator | 미구현 |
| 버전 기반 cache key / Idempotency | 미구현 |
| Piece Seoul / WIM Inner Care Knowledge Base | 미구현. 데모 제품 7건만 |
| Golden Test 100+ | 미구현 |

### 운영 기반

- **인증·인가 전무** — 모든 API가 `userCode`를 path로 받고 소유권 검증이 없다. 현재 상태로 노출하면 임의 `userCode`로 타인의 사진 메타데이터·진단·시술·케어 이력 조회와 사진 업로드·수행 체크 쓰기가 가능하다
- DB 마이그레이션 없음 (`ddl-auto=update` 의존, `.sql` 파일 0건, Flyway/Liquibase 미도입)
- OpenAPI/Swagger 없음 (springdoc 의존성 실제로 없음)
- 관리자용 생성·수정 API 없음. 사용자·시설·시술·제품은 `DemoDataInitializer`로만 생성
- 클라우드 스토리지 없음 (서버 로컬 파일시스템)
- Docker/CI 없음 (Dockerfile, `.github/` 모두 없음)
- 다국어 없음 (`feature/i18n-foundation`은 빈 브랜치)
- 이미지 품질 AI 판정 없음 — `QUALITY_CHECK_FAILED`를 부여하는 코드가 없다. 조회 필터에만 존재

### 최종 기획서가 MVP 필수로 요구하는데 코드에 없는 것

| 요구사항 | 코드 상태 |
|---|---|
| 주간 제품 사용 제한 `ProductUsage`(productId, usedAt, weeklyCount, lastUsedAt). 진정 마스크 주2회, 각질 주1회, 앰플 주2~3회 | 전혀 없음 |
| 다국어 4개 언어 + `UserPreference` | 없음 |
| 현지 시간대 기준 촬영 제한 (`SkinCapture.timezone`) | `Asia/Seoul` 하드코딩, 컬럼 없음 |
| 사용자 유형 구분 + 분석 시설/시술 시설 별도 필드 | 없음 |
| 로그인·사용자 코드 | 인증 없음 |
| 제품 상세·구매 연결 | `officialUrl` 전부 null |
| `SkinAnalysis.resultStatus` = IMPROVED/STABLE/WORSENED/NEEDS_ATTENTION | 없음 |
| `generationType` = NEW_ANALYSIS/CARRIED_FORWARD | `boolean inherited`로 단순화 |
| 자가 상태에 진물·출혈·피부 장벽 손상 체감 | `SelfCheck` 8항목에 없음 |

---

## 12. Mock / 임시 구현

| 위치 | 내용 | 주의 |
|---|---|---|
| `DemoDataInitializer` | 더미 사용자 1명, 시설 3곳, 진단 1건(metric 11개), 시술 1건, 제품 7건을 `CommandLineRunner`로 삽입. **프로필 분리 없음** | 운영 프로필에서도 실행된다. 배포 전 격리 필요 |
| `products` 7건 | 브랜드 전부 `[데모] InnerDerma`, `demoProduct=true`, `officialUrl=null` | 실제 Pith/WHS Store 제품이 아니다. 사용자 노출 전 교체 필수 |
| WHS 진단 metric | 11개 항목의 `userScore`/`averageScore`가 **전부 null**, `grade`만 일부 부여 | 실제 WHS 데이터 규격 확정 대기 |
| 데모 시술 기록 | Treatment Context 12개 필드가 전부 null/빈 컬렉션. `care_guide` 문장 하나만 실체 | 시술 Knowledge Base 필요 |
| `AiRuleInitializer` | `R000`(Safety First), `R002`(Image Quality Gate), `R010`(Minimum Intervention) 3건만 시드 | 조건 JSON을 평가하는 엔진이 없어 현재는 조회용 데이터 |
| `CareSolutionService` | 저녁/아침 문구가 Java `switch`에 하드코딩된 한국어 문장 | LLM 계층 도입 시 대체 대상 |
| `SkinCaptureQualityStatus` | 항상 `VALID`로 저장 | 품질 게이트(R002) 미연결 |
| `skinage.base-url` | 기본값 `http://localhost:8000` | 서버 없으면 `ANALYSIS_004` 502 |
| `DailyCareService` MORNING | `inherited`를 `true`로 하드코딩 | 아침은 정의상 항상 승계이나 명시적 계산은 아님 |

---

## 13. 테스트 상태

- 테스트 클래스 **22개**, 테스트 메서드 **54개**, **전부 통과** (2026-08-18 02:09 KST `cleanTest test` 재실행)
- 스타일: Mockito + AssertJ 단위 테스트 중심. 컨트롤러는 `MockMvcBuilders.standaloneSetup` + `GlobalExceptionHandler` 주입. `@SpringBootTest`는 `InnerDermaApplicationTests.contextLoads()` 1건
- `Clock.fixed(Instant.parse("2026-08-17T03:30:00Z"), Asia/Seoul)`로 날짜 경계를 결정적으로 고정
- 가장 두꺼운 테스트는 `CareHistoryServiceTest`(6건) — 같은 날짜에 이전 아침 + 새 저녁 공존, 미촬영일 양쪽 승계까지 검증

### 테스트가 없는 영역

`FacilityService`/`Controller`, `ProductService`/`Controller`, `UserController`, `SkinCaptureController`, `SkinAnalysisController`, `DailyCareController`, `CareHistoryController`, `CareCompletionController`, `ProductRecommendationController`, `AiRuleController`, `WhsSkinDiagnosisService`, `HttpSkinAgeClient`(외부 HTTP 스텁 없음), `GlobalExceptionHandler` 단독 테스트.

---

## 14. 환경설정

### `src/main/resources/application.properties`

```properties
spring.application.name=InnerDerma
spring.datasource.url=jdbc:mysql://localhost:3306/innerderma?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=innerderma_app
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
innerderma.skin-capture.storage-path=${SKIN_CAPTURE_STORAGE_PATH:./data/skin-captures}
skinage.base-url=${SKINAGE_BASE_URL:http://localhost:8000}
```

### `src/test/resources/application.properties`

H2 in-memory(`MODE=MySQL`), `ddl-auto=create-drop`. 로컬 MySQL 비밀번호 없이 테스트 실행 가능.

### 환경 변수

| 변수 | 용도 | 현재 상태 |
|---|---|---|
| `DB_PASSWORD` | MySQL 비밀번호 | **설정됨** (값은 문서에 기록하지 않는다) |
| `SKIN_CAPTURE_STORAGE_PATH` | 사진 저장 경로 | 미설정 → `./data/skin-captures` |
| `SKINAGE_BASE_URL` | SkinAge 서버 | 미설정 → `http://localhost:8000` |

`/data/`, `/.tools/`, `/.omx/`, `tmp/`, `build/`, `.gradle`, `.idea`는 `.gitignore` 대상이다.

---

## 15. 실행 방법

### Windows PowerShell (현재 Kiro 환경)

```powershell
cd C:\dev\InnerDerma
.\gradlew.bat test                      # 테스트
.\gradlew.bat cleanTest test            # 캐시 무시하고 강제 재실행
.\gradlew.bat bootRun                   # 서버 (기본 포트 8080)
```

주의: 이 환경의 셸은 **장시간 실행 명령의 출력을 반환하지 못하는 경우가 있다.** `gradlew test`가 출력 없이 끝난 것처럼 보여도 실제로는 실행된다. `build/test-results/test/*.xml`의 타임스탬프로 확인하거나 백그라운드 프로세스로 실행해 출력을 읽는 방식을 사용할 것.

### WSL (OMX 환경에서 사용했던 방식)

`/mnt/c` 파일시스템의 Gradle 테스트 결과 파일 잠금 문제를 피하기 위한 전용 스크립트가 있다. 빌드 결과는 `/tmp/innerderma-build`에 생성된다.

```bash
cd /mnt/c/dev/InnerDerma
./scripts/gradlew-wsl.sh test
./scripts/gradlew-wsl.sh bootRun
```

`JAVA_HOME` 기본값은 `/opt/temurin-21`이다.

### `bootRun` 실행 시 주의

현재 MySQL `innerderma` DB에는 테이블이 4개만 있다. `ddl-auto=update`이므로 첫 기동 시 **14개 테이블이 자동 생성**되고 `DemoDataInitializer`·`AiRuleInitializer`가 데모 데이터를 삽입한다. DB 상태를 보존해야 한다면 기동 전에 백업하거나 별도 스키마를 사용할 것.

---

## 16. 알려진 오류 및 해결했던 문제

| 문제 | 해결 |
|---|---|
| `open-in-view=false` 상태에서 지연 로딩 엔티티를 Controller DTO로 변환하다 오류 | `WhsSkinDiagnosisRepository`는 `user`, `ProcedureRecordRepository`는 `facility`를 `@EntityGraph`로 함께 조회. 이후 전 도메인에 동일 패턴 적용 |
| 잘못된 날짜 쿼리 값이 500 반환 | `GlobalExceptionHandler`에 `MethodArgumentTypeMismatchException` 핸들러 추가 → `COMMON_001` 400 |
| 잘못된 JSON enum 값이 500 반환 | `HttpMessageNotReadableException` 핸들러 추가 → `COMMON_001` 400 |
| 예기치 않은 예외의 원인 추적 불가 | `GlobalExceptionHandler`에서 `log.error` 기록 |
| WSL `/mnt/c`에서 Gradle 테스트 결과 파일 잠금 | `scripts/gradlew-wsl.sh` + `gradle-wsl.init.gradle`로 빌드 디렉터리를 `/tmp/innerderma-build`로 이전 |

### 현재 남아 있는 오류

**코드·테스트·로그상 미해결 오류는 없다.** 2026-08-18 재실행에서 54개 전부 통과했다.

### TODO / FIXME

**코드와 문서 전체에서 리터럴 `TODO`/`FIXME`/`HACK`은 0건이다.** 진행 상태는 인라인 주석이 아니라 `docs/AI_IMPLEMENTATION_PROGRESS.md`의 21개 항목 상태표와 `docs/BACKEND_ARCHITECTURE.md` §6으로 관리한다. Kiro도 이 방식을 유지하는 것이 기존 흐름과 맞다.

---

## 17. docs 내부 중요 문서의 핵심

| 문서 | 핵심 | 신뢰도 |
|---|---|---|
| `docs/BACKEND_ARCHITECTURE.md` | 기술 구성, 업무 흐름, 엔드포인트 표, 도메인 목록, 남은 운영 기반 6가지 | **높음.** 엔드포인트 표가 코드와 일치 |
| `docs/AI_IMPLEMENTATION_PROGRESS.md` | 21개 항목의 구현/부분/기반/미구현 상태표 + 다음 개발 순서 9단계 | **높음.** 코드와 어긋나지 않음 |
| `docs/InnerDerma_LLM_Fine_Tuning_Rules.md` | 41개 섹션. **R000~R030 규칙 원장.** 추론 파이프라인 13단계, 데이터 우선순위, Rule ID 카테고리 체계, 최종 응답 JSON 스키마(R026), Hallucination 금지(R027), 의료 표현 제한(R028) | 규칙 정의 기준 |
| `docs/InnerDerma_Backend_AI_Development_Guide.md` | 52개 섹션. Rule DB 스키마, Skin State Snapshot, Solution Object, Product DB 스키마, LLM 입력 최소화, 캐시/멱등성, 논리 API 4개, Phase 1~7 개발 순서, 개발 완료 기준 21개, Golden Test 100+ | 구현 명세 기준 |
| `OMX_HANDOFF_CONTEXT.md` | 8/17 시점 인수인계. §14~§23에 기능별 구현 이력이 상세 | **일부 낙후.** §4(테이블 4개), §7(springdoc), §11(Git 상태)은 현재와 불일치 |
| `INNERDERMA_API_SPEC.md` | SkinAge 외부 API 계약. `POST /api/v1/analyze`, 7 zone × 4 concern = 28지표, 점수는 높을수록 건강 | 높음. DTO 패키지 경로만 실제와 다름 |
| `HELP.md` | Spring Initializr 자동 생성물. 프로젝트 결정을 반영하지 않음 | 참고용 아님 |

### Rule ID 체계 (양 문서 공통)

```text
R000-R099  Safety          R500-R599  Priority / Goal
R100-R199  Input / Image   R600-R699  Night Care
R200-R299  Skin State      R700-R799  Morning Care
R300-R399  Trend           R800-R899  Piece Seoul
R400-R499  Treatment       R900-R999  WIM / Inner Care
                           R1000+     Response / UX
```

---

## 18. `.docx` 기획서의 핵심 (Kiro가 직접 읽기 어려운 문서)

`docs/`의 `.docx` 3건은 바이너리라 직접 열 수 없다. 다음 경로로 내용을 확보했다.

| 문서 | 확보 경로 |
|---|---|
| `AAC_WHS_AI_..._기획서.docx` (초안, 2026.08.15) | `scripts/create_aac_service_plan.py` — 생성 스크립트에 본문 전량 포함 |
| `AAC_WHS_AI_..._최종_기획서.docx` (중간본, 2026.08.16) | `scripts/create_final_service_plan.py` — 동일 |
| `AAC_WHS_AI_..._최종_기획서_수정완료.docx` (**최신 최종본**) | `tmp/final_doc_review/final_content.json` — 추출된 본문 전량. 변경 전 상태는 `content.json`, 변경 로직은 `update_final_plan.py`, 편집 계약은 `artifact.md` |

**`tmp/final_doc_review/`는 단순 임시 폴더가 아니다. 기획 변경과 정책 결정의 유일한 근거 자료다. 삭제하지 말 것.**

### 최신 최종본의 요구사항 요약

- 케어 사이클: 자정 기준 하루가 아니라 귀가 후 세안 ~ 다음 날 기상 후 세안
- 촬영: **사용자 현지 날짜** 기준 1일 1회. 세안 후 화장품 전. 같은 조명·거리·각도. 필터 금지. 품질 실패는 촬영 화면 내 인라인 재촬영이며 촬영 횟수에 미포함
- 분석 항목: 색소 불균형, 모공, 주름, 홍조, 피부결. 결과는 `IMPROVED/STABLE/WORSENED/NEEDS_ATTENTION`
- 자가 상태: 통증·열감·당김·건조함 / 가려움·붓기·벗겨짐 / 트러블·진물·출혈 / 피부 장벽 손상 체감. 4단계(없음·약함·보통·심함)
- 캘린더 생성 유형: `NEW_ANALYSIS` / `CARRIED_FORWARD`
- 주간 제품 제한: 보습제·자외선차단제 매일, 진정 마스크 주2회, 각질 주1회, 기능성 앰플 주2~3회, 식재료·식습관 상시 안내
- 데이터 모델: `SkinCapture`(+timezone), `SkinAnalysis`(+resultStatus), `SelfCheck`, `CareCycle`, 솔루션 출처(+generationType), `ProductUsage`, `UserPreference`
- MVP 필수 10개 + **다국어 4개 언어(16.2)**
- 표현 원칙: "AI 피부 진단" → "AI 피부 상태 분석", "치료·처방" → "케어 솔루션·관리 안내", "성분 적합도" → "제품 추천도", "정상입니다" → "일반적인 경과일 수 있습니다"
- 추가 검증 과제 8건 (WHS 앱 실제 범위, 실제 데이터 스키마, 오픈소스 모델 라이선스, 시술 규칙 전문가 검수, 번역 검수, 윔스토어 공식 사업명, 식품 광고 표현 법규, 국제 날짜 변경 정책)

### 초안에만 있고 최종본에서 빠진 내용 (현재 요구사항으로 취급하지 말 것)

제품 추천도(0~100 점수), 반복 주기 / 수행 기간 분리, 개선·유지·악화·위험·기간만료 5분기, 핵심 성과 지표(KPI), 발표 메시지, 다음 피부 확인일.

---

## 19. 문서 간 충돌 (최신 최종본 + 실제 코드를 기준으로 판단할 것)

| 항목 | 낙후된 내용 | 현재 기준 |
|---|---|---|
| 영양제 | 초안·중간본: 영양제 추천·섭취 주기·"영양·헬스케어" 제품군 | **개인 맞춤 영양제 추천 금지.** 식재료·식습관 + 윔스토어 공식 정보만 (의료법 리스크) |
| 다국어 | 초안·중간본: 선택 기능 / `OMX_HANDOFF_CONTEXT.md`: "MVP는 한국어 고정" | **MVP 필수 4개 언어** |
| 촬영 날짜 기준 | `OMX_HANDOFF_CONTEXT.md`: `Asia/Seoul` 고정 | **사용자 현지 날짜·시간대** |
| 브랜드 | AI 마스터 문서: `Piece Seoul`, `WIM Store` | 기획서: **Pith / WHS Store / 윔스토어**. `Piece Seoul`은 기획서 계열에 등장하지 않는 이름 |
| LLM 방식 | AI 마스터 문서: 파인튜닝 데이터셋·카테고리 상세 규정 | 기획서 9.2: "**파인튜닝이 아니라 규칙 기반 컨텍스트 제공 방식**" |
| DB 규모 | `OMX_HANDOFF_CONTEXT.md` §4: "4개 테이블" | 코드 기준 **18개** |
| Git 상태 | `OMX_HANDOFF_CONTEXT.md` §11: `feature/information-collection`, `868e52a` | `feature/treatment-context`, `3ad814a` |
| springdoc | `OMX_HANDOFF_CONTEXT.md` §7: "의존성 존재" | `build.gradle`에 **없음** |
| DTO 패키지 | `INNERDERMA_API_SPEC.md`: `skinanalysis.application.dto` | 실제 `skinanalysis.application` |
| Rule ID 채번 | 카테고리 `R000-R099=Safety` | 순차 규칙이 `R010`(최소 개입), `R011`(Night), `R014`(Piece Seoul)로 Safety 구간 침범. **문서 자체의 모순.** 코드는 `R010`을 의미 기준 `PRIORITY_GOAL`에 배치 |

---

## 20. AGENTS.md 규칙

프로젝트 루트 `AGENTS.md`에 3개 규칙이 있다. 모두 8/17 대화에서 사용자 요청으로 확정된 것이며 세션·컨텍스트 초기화 후에도 유지해야 한다.

1. **모델 라우팅** — 작업 난이도에 따라 동적으로 선택한다. 단순 확인·반복 작업·Git 조회·정형 문서는 저비용 모델 + 낮은 추론, 일반 구현·테스트·디버깅은 중간 모델 + 중간 추론, 복잡한 아키텍처·난이도 높은 디버깅·고위험 변경·최종 검토에만 고성능 모델. 가장 낮은 비용으로 신뢰성 있게 끝낼 수 있는 조합을 우선한다.
2. **긴 요약은 문서로** — 사용자가 요약을 요청했고 결과가 길면 채팅에만 남기지 말고 `docs/` 아래 Markdown으로 만들고 경로를 알려준다. 채팅 응답은 간결하게 요약한다.
3. **파일 변경 보고 간결화** — 수정한 파일명과 결과만 보고한다. 줄 번호, 추가·삭제 줄 수, patch hunk, 줄 단위 변경 내역은 **명시적으로 요청받을 때만** 제공한다.

---

## 21. Kiro가 다음에 해야 할 일

### 즉시 (작고 위험 낮음)

1. **`feature/treatment-context` → `main` fast-forward 병합.** AI Rule 기반 + WHS metric 구조화 + Treatment Context 3개 커밋이 한 번에 정리된다. 브랜치가 늘어난 채 방치되면 이후 작업 기준선이 흔들린다.
2. **문서 정정** — `OMX_HANDOFF_CONTEXT.md`의 springdoc 기술, 테이블 개수(§4), Git 상태(§11), `INNERDERMA_API_SPEC.md`의 DTO 패키지 경로.

### 착수 전 반드시 결정해야 할 사항

3. **Rule ID 채번 규칙 확정.** 카테고리 범위와 순차 번호가 충돌한다. 엔진 구현 후에 정하면 전면 재작업이 된다.
4. **파인튜닝 vs 프롬프트 컨텍스트 방향 확정.** 기획서와 AI 마스터 문서가 서로 다르다.
5. **인증·인가 방침 결정.** 해커톤 데모까지 무인증으로 갈지, 최소한의 소유권 검증을 넣을지. 현재는 `userCode`만 알면 타인 데이터 접근·쓰기가 가능하다.

### AI 파이프라인 (`docs/AI_IMPLEMENTATION_PROGRESS.md` 순서)

6. User Skin State Snapshot + 일일 상태 점수 저장
7. Trend Engine (이전 Snapshot + 오늘 분석)
8. Rule Engine (Safety → Treatment → Priority → Goal, 충돌 해결 포함)
9. Solution Object 마스터 스키마 + `applied_rules` 저장
10. 제품 Knowledge Base로 데모 제품 교체
11. Inner Care Knowledge Base + 알레르기·식이 필터 (**단, 개인 맞춤 영양제 추천 금지 정책 준수**)
12. Response Validator + 버전 기반 cache key
13. 설명 전용 LLM 계층
14. Golden Test 100+ 확장

### 기획서 MVP 필수 미구현분

15. `ProductUsage` 주간 사용 제한
16. 다국어 + `UserPreference`(preferredLanguage, locale, timezone)
17. 현지 시간대 기준 촬영 제한 (`SkinCapture.timezone`)
18. 사용자 유형 구분 + 분석 시설/시술 시설 분리

### 운영 기반

19. `DemoDataInitializer` 프로필 분리 (`@Profile("demo")` 등)
20. DB 마이그레이션 도입 검토. `ddl-auto=update`는 컬럼 삭제·타입 변경을 반영하지 않는다
21. 이미지 품질 게이트(R002) 연결 — `QUALITY_CHECK_FAILED` 부여 경로와 재촬영 흐름. 조회 측은 이미 준비되어 있다

---

## 22. 아직 확인하지 못한 항목

인수인계 시점에 남은 미검증 항목이다.

1. **MySQL 기준 애플리케이션 기동(`bootRun`)** — DB 스키마·데이터 변경을 유발하므로 승인 대기. 현재 MySQL에는 테이블 4개만 존재한다
2. **SkinAge 서버 연동** — `SKINAGE_BASE_URL` 미설정, 실제 분석 서버 미기동
3. `docs/AAC_WHS_AI_..._최종_기획서_수정완료.docx` 파일 자체 — 본문은 `tmp/final_doc_review/final_content.json`으로 확보했으나 표 서식·이미지는 미확인
4. `working_with_comments.docx`(중간 산출물), `gradle-wrapper.jar`, `.tools/`(gh CLI 바이너리) — 바이너리
5. `src/main/java` 파일 총 개수 — 목록의 모든 파일을 읽었으나 개수를 도구로 집계하지는 않았다
