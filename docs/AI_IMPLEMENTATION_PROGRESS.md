# InnerDerma AI 구현 진행 현황

## 기준 문서

- `docs/InnerDerma_Backend_AI_Development_Guide.md`
- `docs/InnerDerma_LLM_Fine_Tuning_Rules.md`

두 문서를 AI 기능 개발의 Master Specification으로 사용한다. 전체 문서를 매 요청마다 LLM에 전달하지 않고, 실행 가능한 DB 규칙과 코드로 변환한다.

## 현재 구현 상태

| 영역 | 상태 | 현재 내용 |
|---|---|---|
| WHS 진단 | 구현됨 | Baseline 진단을 정규화된 항목별 metric(원본 사용자·평균 점수, 등급)으로 저장·조회 |
| WHS 시술 / Treatment Context | 구현됨 | 기존 시술 기록 호환을 유지하며 검증된 시술 컨텍스트와 지정일 기준 경과일 조회 제공 |
| 얼굴 촬영 | 구현됨 | 일일 파일 업로드, 품질 상태, 오늘 촬영 여부 |
| SkinAge 분석 | 구현됨 | 외부 분석 API 연동과 결과 저장 |
| 자가 문진 | 구현됨 | 주요 증상 등급과 Safety Attention 계산 |
| 케어 사이클 | 구현됨 | 촬영일 저녁과 다음 날 아침 연결, 미촬영일 승계 |
| 기존 케어 솔루션 | 구현됨 | 규칙 기반 Night/Morning 단계 생성 |
| 기존 제품 추천 | 부분 구현 | 활성 제품과 안전 호환성 기반 추천 |
| 케어 수행 이력 | 구현됨 | 단계별 완료 기록, 기간 이력, 수행률 |
| Rule DB | 기반 구현 | Rule ID, 카테고리, 우선순위, JSON 조건·행동·제한, 버전 관리 |
| Safety Rule | 기반 구현 | R000 초기 규칙 저장 |
| Image Quality Rule | 기반 구현 | R002 초기 규칙 저장 |
| Minimum Intervention | 기반 구현 | R010 초기 규칙 저장 |
| User Skin State Snapshot | 미구현 | Baseline, Current, Trend, Treatment Context 압축 필요 |
| Trend Engine | 미구현 | 상태별 Improving/Stable/Worsening 계산 필요 |
| 전체 Rule Engine | 미구현 | 우선순위와 충돌 해결 실행기 필요 |
| Piece Seoul KB | 미구현 | 현재 데모 제품 구조를 검증된 카탈로그로 확장 필요 |
| WIM Inner Care KB | 미구현 | 섭취 제품, 알레르기, 제한 모델 필요 |
| Solution Object | 부분 구현 | 기존 응답은 있으나 Master Schema와 Applied Rules 확장 필요 |
| LLM 설명 계층 | 미구현 | Rule 결과를 변경하지 않는 설명 전용 호출 필요 |
| Response Validator | 미구현 | JSON Schema와 제품·Safety 불변성 검증 필요 |
| Cache/Idempotency | 부분 구현 | DB 재사용은 있으나 버전 기반 cache key 필요 |
| Golden Test | 미구현 | 100개 이상 Rule 중심 시나리오 필요 |

## 이번 단계에서 추가된 기반

- WHS 진단 metric 구조: 피부 나이, 부위별 주름, 색소, 균일도, 여드름, 블랙헤드, 다크서클, 눈처짐, 모공을 각각 저장
- WHS 점수는 제공된 원본만 nullable로 보존하며 평균·종합점수를 임의 생성하지 않음
- `EXCELLENT` / `NORMAL` / `NEEDS_IMPROVEMENT` 등급을 정규화된 Enum으로 관리
- 버전별 AI 규칙을 저장하는 `AiRule` 도메인
- Safety, 이미지 품질, 피부 상태, 추세, 시술, 목표, Night/Morning, Piece Seoul, WIM, UX 규칙 카테고리
- 활성 규칙을 우선순위 순서로 조회하는 서비스
- 활성 규칙 조회 API: `GET /api/ai-rules`
- 초기 Master Rule 데이터: `R000`, `R002`, `R010`
- 동일 Rule ID와 Version 중복 생성을 막는 제약
- Treatment Context: 시술 코드·유형·부위, 회복일 범위, 정상/경고 증상, 사후 제한, 허용/제한 제품 태그, 출처·규칙 버전을 nullable/빈 컬렉션 기반으로 구조화
- `GET /api/users/{userCode}/procedures/treatment-context?date={date}`: 지정일 이하의 가장 최신 시술만 선택하고 `daysSinceTreatment`를 계산; 미래 시술은 선택하지 않음
- 데모 시술에는 기존 관리 가이드만 유지하고, 제공되지 않은 의학적 컨텍스트는 null 또는 빈 컬렉션으로 보존

## 다음 개발 순서

1. User Skin State Snapshot과 일일 상태 점수 저장
2. 이전 Snapshot과 오늘 분석을 이용한 Trend Engine 구현
3. Safety → Treatment → Priority → Goal 순서의 Rule Engine 구현
4. Night/Morning Solution Object와 Applied Rule 저장
5. Piece Seoul 검증 상품 Knowledge Base 확장
6. WIM Inner Care Knowledge Base와 알레르기 필터 구현
7. 응답 검증기, 버전 기반 중복 호출 방지, 캐시 구현
8. 설명 전용 LLM 계층 연결
9. Golden Test Set 확장

## 원칙

- Safety가 항상 가장 높은 우선순위를 가진다.
- LLM은 제품과 의학적 사실을 새로 만들지 않는다.
- Rule Engine과 Product Matcher가 결정을 완료한 후 LLM은 설명만 생성한다.
- 전체 사용자 이력 대신 Snapshot과 Trend Summary를 사용한다.
- 같은 입력과 버전으로 생성된 일일 솔루션은 재사용한다.
