# InnerDerma OMX 인수인계 컨텍스트

> 작성일: 2026-08-17
> 목적: OMX 환경에서 InnerDerma 백엔드 개발을 이어가기 위한 서비스·기술·진행 상태 요약

## 1. 서비스 한 줄 정의

InnerDerma는 AAC의 기존 **WHS 앱을 대체하는 별도 앱이 아니라**, 해외 고객이 귀국한 뒤에도 최신 피부 상태를 바탕으로 동적인 홈케어 솔루션을 받을 수 있게 하는 사후관리 기능이다.

기본 흐름은 다음과 같다.

```text
WHS 오프라인 피부 진단 결과
+ 귀국 후 스마트폰 피부 사진 분석
+ 사용자가 직접 입력한 체감 상태
+ 클리닉 시술 기록 및 주의사항
= 저녁 관리 + 다음 날 아침 관리로 구성된 케어 솔루션
```

## 2. 서비스/용어 규칙

- **웰니스 하우스 서울(WHS)**: 오프라인 시설. 이번 MVP에서 피부 진단 결과의 유일한 출처.
- **WHS 앱**: 기존 앱. 초기 진단 결과, AI 피부 분석, 시술 내역, 케어 가이드/제품 추천을 제공하는 것으로 이해하고 있다.
- **더나클리닉(DERNA), 엠레드의원(AMRED)**: 시술 기록을 보유하는 클리닉 시설.
- **제안 기능(InnerDerma)**: 귀국 후 사진·자가 상태를 반영해 반복적으로 케어를 조정하는 동적 사후관리 기능.
- AI 결과는 **의료 진단/처방**이 아니라 피부 상태 분석 및 케어 안내로 표현한다.

## 3. MVP에서 확정된 범위

### 사용자와 오프라인 기준 데이터

- 해커톤 MVP는 더미 사용자 1명으로 진행한다.
- 실제 AAC/WHS 데이터 연동은 하지 않고, 동일한 흐름을 더미 데이터로 재현한다.
- 사용자는 시술명을 직접 작성하지 않는다.
  - 최근 방문 시설을 선택하고 시술일을 선택한다.
  - 서버가 시설이 보유한 시술 기록(현재는 더미 데이터)을 조회한다.
- WHS 피부 진단 결과는 WHS에서만 받은 것으로 구현한다.
- 현 단계에서는 세부 피부 점수/실제 WHS 데이터 규격을 확정하지 않는다. WHS 피부 진단은 날짜와 결과 요약 중심의 러프한 구조다.

### 현재 더미 데이터

```text
사용자: WHS-DEMO-001 / 테스트 사용자 / 010-1234-1234
WHS 진단일: 2026-08-15
WHS 진단 요약: 수분이 부족하고 볼 주변 홍조와 거친 피부결이 관찰됨
시술 시설: 더나클리닉(DERNA)
시술일: 2026-08-15
시술명: 진정 및 피부 장벽 관리
케어 가이드: 자극적인 제품을 피하고 보습제를 충분히 사용할 것
```

### 다국어/시간대

- 최종 서비스 타깃은 해외 방문객이며 한국어·영어·중국어(간체)·일본어를 지원하는 것이 기획 방향이다.
- 그러나 현재 해커톤 MVP 구현은 **한국어, `Asia/Seoul`, 한국 날짜 기준**으로 고정한다.
- 추후 확장 시 기기 언어와 기기 시간대를 받아 처리한다. 국적만으로 언어나 시간대를 추론하지 않는다.

## 4. 핵심 도메인 및 관계

현재는 아래 4개 테이블로 러프하게 구현했다.

```text
users
 ├─ whs_skin_diagnoses
 └─ procedure_records ─ facilities
```

### `users`

```text
id, user_code, name, phone_number
```

- 더미 사용자를 식별하는 최소 정보만 저장한다.
- `created_at`, `updated_at`은 현재 MVP 범위에서 제거했다.

### `facilities`

```text
id, facility_code, name
```

- `WHS`: 웰니스 하우스 서울
- `DERNA`: 더나클리닉
- `AMRED`: 엠레드의원

### `whs_skin_diagnoses`

```text
id, user_id, diagnosed_date, result_summary
```

- WHS에서 받은 초기 피부 진단 결과다.
- 지금은 세부 항목을 과도하게 모델링하지 않는다.
- 실제 WHS 결과 구조가 확정되면 건조/유분/색소/모공/주름/홍조/피부결 등으로 확장 가능하다.

### `procedure_records`

```text
id, user_id, facility_id, procedure_date, procedure_name, care_guide
```

- 클리닉이 보유한 시술 기록을 표현한다.
- 시술 후 케어 가이드와 주의사항을 이후 케어 솔루션에 추가하기 위한 기반이다.

## 5. 케어 솔루션 기획 규칙 (아직 미구현)

### 케어 사이클

자정 기준 하루 단위가 아니라 다음을 하나의 사이클로 본다.

```text
귀가 후 세안 → 저녁 집중 관리 → 수면 → 다음 날 기상 후 세안 → 아침 기본 관리
```

예시:

```text
17일 사진 촬영 → 17일 저녁 + 18일 아침 = 17~18일 케어 사이클
```

- 촬영한 날: 새 사진 분석과 새 솔루션 생성
- 촬영하지 않은 날: 가장 최근 솔루션을 이어서 제공하고, 원본 촬영일과 승계 상태를 표시
- 하루 한 번 유효 촬영을 목표로 한다. 추후에는 사용자 현지 날짜 기준으로 제한한다.
- 품질 검사에 실패한 사진은 촬영 횟수 제한에 포함하지 않으며, 촬영 화면 안에서 재촬영을 안내한다.

### 입력 데이터 우선순위

```text
1. WHS 초기 피부 진단 결과
2. 귀국 후 스마트폰 사진 분석 결과
3. 자가 상태: 통증, 열감, 당김, 건조함, 가려움, 붓기, 벗겨짐, 트러블 등
4. 시술 종류/시술일/케어 가이드
5. 보유 제품 및 주간 사용 이력 (추후)
```

### 제품/식생활 원칙

- Pith 및 WHS Store 화장품을 현재 피부 상태와 관리 목적에 따라 추천하는 방향이다.
- 영양제를 개인 맞춤으로 추천하거나 복용량·횟수·적합성을 판단하지 않는다.
- 대신 일반적인 식재료·식습관 안내 및 윔스토어 제품의 공식 정보/구매 경로 연결을 검토한다.
- 시술 후 위험 신호나 통증·열감·붓기 등은 일반 케어보다 안전 안내를 우선한다.

## 6. 사진 AI 분석 연동 계획

AI 사진 분석에서 서비스가 우선 사용할 큰 항목은 다음이다.

```text
색소 불균형, 모공, 주름, 홍조, 피부결
```

워크스페이스 루트에 아직 Git 추적되지 않은 `INNERDERMA_API_SPEC.md`가 있다.

- SkinAge API 명세 문서다.
- 엔드포인트: `POST /api/v1/analyze` (`multipart/form-data`)
- 입력: 얼굴 사진 파일, 선택적 age, 선택적 heatmap 포함 여부
- 출력: 피부 나이, 종합 점수, 7개 안면 구역별 4대 고민(주름, 모공·피부결, 색소, 홍조), 집계/우선 고민, 메타데이터
- SkinAge는 순수 분석 데이터 제공자이며, 제품 추천/케어 솔루션 같은 비즈니스 로직은 InnerDerma 백엔드가 담당한다.

SkinAge의 점수는 **높을수록 건강/결점 없음**이다. InnerDerma의 화면·규칙에서 점수 방향을 혼동하지 않도록 주의한다.

## 7. 현재 백엔드 기술 구성

```text
언어: Java 21
프레임워크: Spring Boot 4.0.8-SNAPSHOT
빌드: Gradle 9.5.1
웹: Spring MVC
DB: MySQL 8.0 (로컬 innerderma DB)
ORM: Spring Data JPA + Hibernate
테스트 DB: H2 in-memory
API 문서 도구: springdoc-openapi 의존성 존재
```

### 주요 설정

`src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/innerderma?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=innerderma_app
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
```

- 비밀번호는 환경 변수 `DB_PASSWORD`로 관리한다. 실제 값을 저장소나 인수인계 문서에 넣지 않는다.
- `ddl-auto=update`이므로 엔티티 기준 테이블/컬럼 추가가 자동 반영된다. 다만 삭제된 컬럼은 자동 삭제되지 않을 수 있다.
- 테스트는 `src/test/resources/application.properties`에서 H2를 사용하므로 로컬 MySQL 비밀번호 없이 실행 가능하다.

## 8. 현재 코드 구조

```text
com.innerderma
├─ common
│  ├─ config       # DemoDataInitializer: 더미 사용자/시설/진단/시술 데이터 삽입
│  ├─ error        # ErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler
│  ├─ health       # 서버 상태 API
│  └─ response     # 공통 성공 응답 ApiResponse
├─ user
│  ├─ api
│  ├─ application
│  └─ domain
├─ facility
│  ├─ api
│  ├─ application
│  └─ domain
├─ skindiagnosis
│  ├─ api
│  ├─ application
│  └─ domain
└─ procedure
   ├─ api
   ├─ application
   └─ domain
```

각 도메인의 기본 패턴:

```text
domain       Entity + Repository (JPA)
application  Service (업무/조회 로직)
api          Controller + Response DTO
```

## 9. 현재 API

모든 성공 응답은 공통 형식이다.

```json
{
  "success": true,
  "data": {}
}
```

| 목적 | 메서드/주소 | 설명 |
|---|---|---|
| 헬스 확인 | `GET /api/innerderma/health` | 서버 동작 확인 |
| 사용자 | `GET /api/users/WHS-DEMO-001` | 더미 사용자 조회 |
| 시설 목록 | `GET /api/facilities` | WHS/DERNA/AMRED 목록 |
| WHS 진단 | `GET /api/users/WHS-DEMO-001/skin-diagnosis` | 해당 사용자의 최신 WHS 진단 요약 |
| 시술 기록 | `GET /api/users/WHS-DEMO-001/procedures?facilityCode=DERNA&date=2026-08-15` | 시설과 시술일로 시술 기록 조회 |

오류 응답 예시:

```json
{
  "success": false,
  "code": "USER_001",
  "message": "사용자를 찾을 수 없습니다.",
  "errors": {}
}
```

오류 코드:

```text
COMMON_001: 잘못된 요청
COMMON_002: 내부 서버 오류
USER_001: 사용자 없음
SKIN_001: WHS 피부 진단 결과 없음
PROCEDURE_001: 시술 기록 없음
```

### 구현 중 해결한 점

- `spring.jpa.open-in-view=false` 상태에서 지연 로딩 엔티티를 Controller DTO로 변환하면서 오류가 발생했다.
- `WhsSkinDiagnosisRepository`는 `user`, `ProcedureRecordRepository`는 `facility`을 `@EntityGraph`로 함께 조회하도록 수정했다.
- 예기치 않은 예외는 `GlobalExceptionHandler`에서 로그를 남기도록 했다.

## 10. 실행/검증 방법

OMX의 WSL 환경에서는 `/mnt/c` 파일시스템의 Gradle 테스트 결과 파일 잠금 문제를 피하기 위해
전용 실행 스크립트를 사용한다. 빌드 결과는 Linux 임시 경로인 `/tmp/innerderma-build`에 생성된다.

```bash
cd /mnt/c/dev/InnerDerma
./scripts/gradlew-wsl.sh test
./scripts/gradlew-wsl.sh bootRun
```

현재 OMX WSL에는 Temurin Java 21이 `/opt/temurin-21`에 설치되어 있다. 다른 WSL 환경에서
스크립트를 사용할 때는 Java 21을 설치하거나 `JAVA_HOME`을 Java 21 경로로 지정한다.

Windows PowerShell에서는 기존 명령을 그대로 사용한다.

```powershell
cd C:\dev\InnerDerma
.\gradlew.bat clean test
.\gradlew.bat bootRun
```

- 기본 포트는 `8080`이다.
- `8080 was already in use` 오류가 나면 해당 포트를 점유한 프로세스를 종료한 뒤 재실행한다.
- `bootRun`은 서버가 실행 중인 동안 터미널을 점유하는 것이 정상이며, `Ctrl+C`로 종료한다.

## 11. Git 상태 (인수 시점)

```text
현재 브랜치: feature/information-collection
현재 커밋: 868e52a feat: 정보수집 기능 기본 구조 추가
원격 추적: origin/feature/information-collection
main 최신 커밋: a9c1ae7
```

현재 Git 미추적 파일:

```text
.omx/
INNERDERMA_API_SPEC.md
[수정본]AAC_WHS_AI_피부_사후관리_서비스_최종_기획서.docx
```

- `.omx/`의 목적과 추적 여부는 OMX 환경 정책을 확인한 뒤 결정한다.
- API 명세와 최종 기획서는 필요하다면 별도 문서 커밋으로 관리한다.
- 이 인수인계 파일도 새 파일이므로 필요 시 함께 커밋한다.

## 12. 다음 개발 우선순위

현재 오프라인 기준 정보 조회는 구현됐다. 다음은 새 브랜치에서 진행하는 것을 권장한다.

1. `skin-capture`: 피부 사진 촬영 기록 엔티티/API
   - 촬영 시각, 촬영 날짜, 이미지 위치, 품질 상태
   - MVP는 한국 날짜 기준 하루 1회
2. `self-check`: 자가 상태 입력
   - 통증, 열감, 당김, 건조함 등
3. `skin-analysis`: SkinAge API 연동 또는 우선 더미 분석 결과
   - 색소/모공/주름/홍조/피부결 중심으로 매핑
4. `care-cycle`: 저녁 + 다음 날 아침 케어 사이클, 미촬영 승계 로직
5. `care-solution`: 규칙 기반 안전 필터 후 LLM으로 사용자 설명/루틴 생성
6. 제품 추천, 캘린더/기록, 다국어, 실제 AAC/WHS 연동 순으로 확장

## 13. 개발 시 지켜야 할 핵심 원칙

- 실제 WHS/AAC 내부 데이터 구조는 아직 알 수 없으므로 더미 데이터와 느슨한 구조를 우선한다.
- 현재 MVP는 사용자 1명·한국어·한국 시간 기준으로 개발한다.
- 피부 분석과 시술 데이터는 사용자가 직접 작성하는 것이 아니라, 시설 기록을 조회하는 흐름을 유지한다.
- AI 모델은 정량적 피부 상태 데이터 제공 역할만 맡고, 케어 추천의 안전 규칙과 제품·행동 추천은 InnerDerma 서비스 로직이 담당한다.
- 기능 하나 단위로 브랜치 → 테스트 → 커밋 → 푸시 → PR → main 병합 흐름을 사용한다.

## 14. 2026-08-17 후속 개발 기록: 피부 사진 촬영

다음 우선순위였던 `skin-capture` 1차 백엔드를 구현했다.

- `POST /api/users/{userCode}/skin-captures` (`multipart/form-data`, part 이름 `file`)
  - JPEG/PNG/WebP, 최대 10MB
  - MIME 타입과 파일 시그니처 검증
  - 한국 시간 기준 유효 촬영 하루 1회
  - 로컬 저장 경로는 `SKIN_CAPTURE_STORAGE_PATH`로 변경 가능
- `GET /api/users/{userCode}/skin-captures/latest`
  - 사용자의 가장 최근 촬영 메타데이터 조회
- `skin_captures` 엔티티에 촬영 날짜/시각, 이미지 위치, 원본 파일명, MIME 타입, 크기, 품질 상태 저장
- 런타임 이미지가 저장되는 `/data/`는 Git 추적에서 제외
- 오류 코드 `CAPTURE_001`~`CAPTURE_004` 추가
- 서비스 테스트 및 전체 Spring 컨텍스트 테스트 통과

현재 품질 상태 `VALID`는 파일 형식/크기 검사를 통과한 MVP 상태다. 얼굴 가림, 해상도,
조명 같은 AI 품질 판정이 연결되면 `QUALITY_CHECK_FAILED`와 재촬영 흐름을 확장해야 한다.
다음 개발 우선순위는 `self-check` 자가 상태 입력이다.

## 15. 기획 변경: 계절 컨텍스트

계절 정보는 자가 상태 입력에 포함하지 않고, 향후 케어 솔루션을 생성할 때만
보조 조정값으로 사용한다. 실시간 날씨 API 연동은 현재 범위에서 제외한다.

```text
WHS 초기 피부 진단 + 최신 사진 분석 + 자가 상태 + 시술 기록 + 계절
= 저녁 + 다음 날 아침 케어 솔루션
```

적용 우선순위는 다음과 같다.

1. 시술 후 주의사항과 안전 규칙
2. 통증·열감·붓기 등 자가 상태
3. 최신 사진 분석 결과
4. WHS 초기 진단
5. 계절에 따른 케어 강도 보조 조정

- 봄: 외부 자극과 민감도 고려
- 여름: 피지·트러블·자외선 관리
- 가을: 건조 전환기 보습
- 겨울: 피부 장벽과 집중 보습
- 계절이 피부 상태나 시술 안전 규칙을 덮어쓰지 않도록 한다.
- 현재 MVP의 계절 판단 방식은 케어 솔루션 구현 시 확정한다.
- 구현 순서는 `self-check → skin-analysis → care-cycle → care-solution`이다.

## 16. 2026-08-17 후속 개발 기록: 자가 피부 상태

`self-check` 1차 백엔드를 구현했다.

- `POST /api/users/{userCode}/self-checks`
- `GET /api/users/{userCode}/self-checks/latest`
- 통증, 열감, 당김, 건조함, 가려움, 붓기, 벗겨짐, 트러블을
  `NONE/MILD/MODERATE/SEVERE` 4단계로 저장
- 사용자 메모는 선택이며 최대 500자
- 하나라도 `SEVERE`이거나 통증·열감·붓기가 `MODERATE`이면
  `requiresSafetyAttention=true`로 반환
- 이 플래그는 의료 진단이 아니라 일반 케어보다 안전 안내를 우선하기 위한 규칙이다.
- 잘못된 JSON enum 값도 500이 아닌 `COMMON_001` 400 응답으로 처리한다.
