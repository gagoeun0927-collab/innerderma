# InnerDerma Backend AI Architecture & LLM Fine-Tuning / Rule Implementation Guide

## 0. 문서 목적

이 문서는 InnerDerma의 백엔드 개발 에이전트가 AI 기반 피부 사후관리 서비스를 실제 구현할 수 있도록 다음 내용을 하나의 개발 명세로 통합한다.

- 서비스의 AI 추론 구조
- LLM과 Rule Engine의 역할 분리
- 파인튜닝의 목적과 범위
- Master Rule / Rule DB 설계
- 사용자 Skin State 및 Trend 저장 방식
- WHS 오프라인 진단/시술 데이터 처리
- Daily Face Analysis 처리
- Night / Morning / Inner Care 솔루션 생성
- Piece Seoul / WIM Store 상품 매칭
- LLM 호출량 및 크레딧 최소화
- 안전성 및 Hallucination 방지
- API / DB / JSON Schema 권장 구조
- 개발 우선순위와 완료 기준

> 핵심 원칙:
>
> **전체 Rule과 전체 사용자 이력을 매번 LLM에 보내지 않는다.**
>
> `Raw Data → Structured State → Rule Engine → Solution Object → LLM Response`
> 구조를 사용한다.
>
> LLM은 의학적 판단이나 상품 선택의 유일한 주체가 아니다. 검증된 Rule과 Knowledge Base가 실제 의사결정을 제한하고, LLM은 필요한 경우 이를 자연어로 설명한다.

---

# 1. 서비스 개요

InnerDerma는 WHS(Wellness House Seoul)를 방문하여 피부 진단 또는 피부 진단 + 시술을 받은 외국인 사용자가 귀국 후에도 지속적인 사후 피부관리를 받을 수 있도록 하는 서비스다.

사용자는 매일 귀가 후 세안한 상태에서 얼굴 사진을 촬영한다.

AI는 다음 정보를 종합한다.

1. WHS 오프라인 피부 진단 데이터
2. WHS 시술 데이터
3. 시술 후 경과일
4. 오늘 촬영한 얼굴 이미지
5. 오늘의 사용자 문진/증상
6. 이전 Daily Skin State
7. 피부 상태 변화 Trend
8. 검증된 Piece Seoul 제품 DB
9. 검증된 WIM Store 식품 DB

최종적으로 다음 세 가지를 생성한다.

- **Night Care**: 당일 밤 세안 후 수행할 루틴
- **Morning Care**: 다음날 아침 수행할 루틴
- **Inner Care**: 현재 상태를 고려한 섭취 관련 솔루션

---

# 2. 반드시 지켜야 하는 핵심 아키텍처

```text
                    USER
                      │
                      ↓
              Today's Face Photo
                      │
                      ↓
              Image Analysis Layer
                      │
                      ↓
              Current Skin State
                      │
                      ↓
        ┌──────────────────────────┐
        │      User Skin State DB  │
        │                          │
        │ Baseline                 │
        │ Current State            │
        │ Trend                    │
        │ Treatment Context        │
        │ Recent History Summary   │
        └────────────┬─────────────┘
                     │
                     ↓
              ┌──────────────┐
              │  Rule Engine │
              └──────┬───────┘
                     │
              Applicable Rules
                     │
                     ↓
             Product Matching
              ┌──────┴──────┐
              ↓             ↓
        Piece Seoul      WIM Store
              │             │
              └──────┬──────┘
                     ↓
              Solution Object
                     │
                     ↓
                    LLM
                     │
                     ↓
             User-facing Response
```

## 절대 금지 구조

```text
❌ 전체 Master Rule MD
+
❌ 전체 WHS 기록
+
❌ 전체 30일/100일 사진 분석 기록
+
❌ 전체 Product DB
→ LLM
```

이 구조는 토큰/크레딧 낭비가 크고 컨텍스트가 불필요하게 커지므로 사용하지 않는다.

---

# 3. AI 역할 분리

## 3.1 LLM이 담당

- 비정형 사용자 입력 해석
- 구조화된 분석 결과를 자연어로 변환
- 사용자 친화적인 설명
- Night / Morning / Inner Care의 설명 생성
- 추천 이유를 이해하기 쉽게 표현
- 다국어 응답이 필요한 경우 번역/표현

## 3.2 Rule Engine이 담당

- Safety 판단
- 시술 후 경과일에 따른 제한
- 피부 상태 우선순위
- Today's Goal 결정
- Night / Morning의 기본 목적
- 제품 적합성 필터
- Piece Seoul 제품 선택 조건
- WIM Store 제품 선택 조건
- 알레르기/식이 제한
- 전문가 확인이 필요한 조건
- 제품 사용법 및 주의사항의 출처 제한

## 3.3 DB / Knowledge Base가 담당

- WHS 진단 데이터
- WHS 시술 데이터
- Treatment Recovery Rule
- Piece Seoul Product 정보
- WIM Store Product 정보
- Product Usage
- Product Restrictions
- Verified Ingredient / Benefit 정보
- Inner Care 관련 검증된 규칙

---

# 4. 파인튜닝의 역할

파인튜닝은 모든 Rule을 LLM 내부에 넣기 위한 목적이 아니다.

### 파인튜닝의 목적

LLM이 다음 행동을 일관되게 수행하도록 학습한다.

1. 구조화된 Solution Object를 해석한다.
2. 존재하지 않는 정보를 만들지 않는다.
3. 제공된 제품 ID/제품명만 사용한다.
4. Night와 Morning을 구분한다.
5. 추천 이유를 간결하게 설명한다.
6. 안전 경고가 있으면 일반 추천보다 경고를 우선한다.
7. 사용자에게 과도한 루틴을 제시하지 않는다.
8. 확정적인 의료 진단 표현을 사용하지 않는다.
9. 정해진 JSON Schema를 준수한다.
10. 브랜드/상품 정보는 Knowledge Base에서 전달된 값만 사용한다.

### 파인튜닝에 넣지 않을 것

- 전체 상품 DB
- 사용자별 장기 기록
- 자주 변경되는 상품 정보
- 시술별 최신 Rule
- 실시간 재고
- 매일 변하는 피부 상태
- 변경 가능한 사용법

이런 데이터는 DB/Rule Engine에서 조회한다.

---

# 5. Master Rule의 위치

기존 `InnerDerma_LLM_Fine_Tuning_Rules.md`는 **Master Specification**으로 취급한다.

이 파일을 매번 LLM에게 전송하지 않는다.

권장 구조:

```text
docs/
└── InnerDerma_LLM_Fine_Tuning_Rules.md

backend/
├── ai/
│   ├── rule-engine/
│   ├── prompt/
│   ├── schemas/
│   └── services/
├── knowledge/
│   ├── treatments/
│   ├── products/
│   └── inner-care/
└── database/
```

Master Rule은 개발 기준 문서이며, 실제 실행은 Rule DB / 코드 / 설정값으로 변환한다.

---

# 6. Rule ID 체계

Rule은 ID를 사용하여 관리한다.

```text
R000-R099   Safety
R100-R199   Input / Image
R200-R299   Skin State
R300-R399   Trend
R400-R499   Treatment
R500-R599   Priority / Goal
R600-R699   Night Care
R700-R799   Morning Care
R800-R899   Piece Seoul
R900-R999   WIM / Inner Care
R1000+      Response / UX
```

예:

```text
R001 = 기본 Safety Gate
R101 = 이미지 품질 검사
R204 = Skin State Priority
R307 = Trend Worsening
R412 = Treatment Recovery Restriction
R604 = Night Recovery Routine
R803 = Piece Seoul Eligibility
R901 = WIM Allergy Filter
```

실제 ID는 구현 시 세부 규칙에 맞게 확장한다.

---

# 7. Rule DB Schema

권장 Rule 구조:

```json
{
  "rule_id": "R604",
  "category": "NIGHT_CARE",
  "name": "Barrier Recovery Night Routine",
  "priority": 80,
  "conditions": {
    "primary_state": ["BARRIER_RECOVERY"],
    "trend": ["STABLE", "WORSENING"],
    "safety_status": ["NORMAL"]
  },
  "actions": {
    "goal": "BARRIER_SUPPORT",
    "routine_type": "RECOVERY"
  },
  "restrictions": [],
  "explanation_template": "현재 피부 상태를 고려해 오늘 밤은 장벽 회복을 중심으로 관리합니다.",
  "version": "1.0.0",
  "enabled": true
}
```

Rule은 반드시 버전 관리한다.

---

# 8. Rule 실행 방식

## Step 1 — 입력 구조화

Raw input:

```json
{
  "image_analysis": {...},
  "questionnaire": {...},
  "treatment_history": {...}
}
```

↓

Structured State:

```json
{
  "current_state": {...},
  "trend": {...},
  "treatment_context": {...},
  "safety_status": "NORMAL",
  "confidence": 0.91
}
```

## Step 2 — 적용 Rule 검색

Structured State를 조건으로 Rule DB를 조회한다.

```text
Safety Rules
→ Treatment Rules
→ Priority Rules
→ Routine Rules
→ Product Rules
```

## Step 3 — Rule 충돌 해결

우선순위:

```text
Safety
>
Treatment Restriction
>
Current State
>
Trend
>
General Care
```

같은 우선순위에서는 더 구체적인 Rule을 우선한다.

---

# 9. User Skin State 설계

매일 과거 전체 데이터를 다시 LLM에 보내지 않기 위해 사용자별 Snapshot을 저장한다.

권장 Schema:

```json
{
  "user_id": "USER_ID",
  "baseline": {
    "initial_skin_profile": {},
    "initial_whs_diagnosis_id": ""
  },
  "current": {
    "dryness": 0,
    "redness": 0,
    "irritation": 0,
    "acne": 0,
    "sebum": 0,
    "hydration": 0,
    "pigmentation": 0
  },
  "trend": {
    "dryness": "STABLE",
    "redness": "IMPROVING",
    "irritation": "UNKNOWN"
  },
  "treatment_context": {
    "treatment_id": "",
    "days_since_treatment": 0
  },
  "recent_summary": {
    "period_days": 7,
    "major_change": "",
    "confidence": 0.0
  },
  "updated_at": ""
}
```

---

# 10. History Compression

## 금지

```text
Day 1
Day 2
Day 3
...
Day 100
→ 매일 LLM에 전송
```

## 권장

```text
Raw Daily Data
↓
Daily Snapshot
↓
Trend Aggregator
↓
7-day / 30-day Summary
↓
User Skin State
```

LLM에는 원본 전체가 아니라 다음만 전달한다.

```text
Baseline
+
Current
+
Trend
+
Recent Summary
+
Treatment Context
```

필요한 경우에만 특정 과거 기록을 추가 조회한다.

---

# 11. 피부 상태 Taxonomy

Primary / Secondary 상태를 분리한다.

```text
BARRIER_RECOVERY
HYDRATION
REDNESS
IRRITATION
ACNE
SEBUM
PIGMENTATION
SWELLING
STABLE
```

상태는 동시에 여러 개 존재할 수 있다.

예:

```json
{
  "primary": "BARRIER_RECOVERY",
  "secondary": ["HYDRATION", "REDNESS"]
}
```

---

# 12. Skin Score

가능하면 0~5 범위로 통일한다.

```text
0 = 없음
1 = 매우 낮음
2 = 낮음
3 = 중간
4 = 높음
5 = 매우 높음
```

Score와 Trend를 반드시 분리한다.

```json
{
  "dryness": {
    "score": 3,
    "trend": "WORSENING"
  }
}
```

---

# 13. Trend Rule

Trend:

```text
IMPROVING
STABLE
WORSENING
UNKNOWN
```

예:

```text
Day 1 = 2
Day 2 = 2
Day 3 = 3
Day 4 = 4

→ score = 4
→ trend = WORSENING
```

개선 중인 상태에서는 불필요하게 제품을 추가하지 않는다.

악화 중인 상태에서는 먼저 Safety / Treatment Context를 재평가한다.

---

# 14. Treatment Context

모든 시술 데이터에는 최소한 다음 정보가 필요하다.

```json
{
  "treatment_id": "",
  "treatment_type": "",
  "treatment_date": "",
  "treatment_area": "",
  "days_since_treatment": 0,
  "expected_recovery_window": "",
  "normal_symptoms": [],
  "warning_symptoms": [],
  "aftercare_restrictions": [],
  "allowed_product_tags": [],
  "restricted_product_tags": [],
  "source": "WHS",
  "version": "1.0.0"
}
```

시술 관련 정보는 전문가 검증 Knowledge Base에서 관리한다.

---

# 15. Safety Gate

모든 솔루션 생성 전에 실행한다.

```text
IF
  severe_or_unusual_symptom
  OR rapidly_worsening
  OR professional_review_required
THEN
  safety_status = "CAUTION"
```

CAUTION 상태에서는:

- 일반적인 제품 구매 유도 최소화
- 공격적인 루틴 추가 금지
- 전문가 확인 안내
- 확정적 진단 금지

Safety가 항상 다른 Rule보다 우선한다.

---

# 16. Image Quality Gate

```json
{
  "face_detected": true,
  "blur_score": 0.0,
  "lighting_score": 0.0,
  "occlusion_score": 0.0,
  "quality": "GOOD"
}
```

조건:

```text
face_not_detected
OR
image_blurry
OR
lighting_insufficient
OR
face_partially_occluded
```

이면 재촬영을 요청할 수 있다.

품질이 낮으면 Confidence를 낮추고 확정적인 상태 판단을 하지 않는다.

---

# 17. Today's Primary Goal

여러 상태 중 하루의 핵심 목표 하나를 선택한다.

기본 우선순위:

```text
Safety
↓
Post-treatment Recovery
↓
Irritation / Redness
↓
Barrier
↓
Hydration
↓
Acne / Sebum
↓
Pigmentation
↓
Maintenance
```

예:

```json
{
  "primary_state": "BARRIER_RECOVERY",
  "goal": "BARRIER_SUPPORT"
}
```

---

# 18. Night Care

Night Care는 사용자가 귀가 후 세안하고 촬영한 직후 실행할 루틴이다.

목적:

```text
RECOVERY
CALMING
HYDRATION
BARRIER_SUPPORT
```

권장 최대 3~4단계.

LLM은 최종 출력에서 각 단계의:

```text
WHAT
HOW
WHEN
WHY
```

를 명확하게 표현한다.

---

# 19. Morning Care

Morning Care는 다음날 아침 루틴이다.

목적:

```text
HYDRATION
PROTECTION
MAINTENANCE
```

권장 최대 2~3단계.

Night Care와 동일한 루틴을 단순 반복하지 않는다.

---

# 20. Piece Seoul Product Matching

스킨케어 제품은 Piece Seoul DB에서만 선택한다.

## 절대 규칙

```text
LLM이 제품명을 새로 생성하지 않는다.
```

제품 선택:

```text
Current State
+
Treatment Compatibility
+
Safety
+
Goal
+
Product Eligibility
↓
Candidate Products
↓
Final Product
```

---

# 21. Product DB Schema

```json
{
  "product_id": "PSS_001",
  "brand": "Piece Seoul",
  "name": "",
  "category": "",
  "tags": [],
  "skin_state_tags": [],
  "treatment_compatibility": [],
  "restricted_after_treatments": [],
  "usage_time": [],
  "frequency": "",
  "amount": "",
  "application_method": "",
  "warnings": [],
  "verified_claims": [],
  "is_active": true,
  "version": "1.0.0"
}
```

제품의 사용법, 효능, 주의사항은 이 DB의 검증된 값만 사용한다.

---

# 22. WIM Store Product Matching

Inner Care 제품은 WIM Store DB에서만 선택한다.

필터 순서:

```text
Current State
↓
Dietary Profile
↓
Allergy Filter
↓
Restriction Filter
↓
Professional Restriction
↓
Eligibility
↓
Recommendation
```

---

# 23. WIM Product DB Schema

```json
{
  "product_id": "WIM_001",
  "name": "",
  "category": "",
  "state_tags": [],
  "dietary_tags": [],
  "allergens": [],
  "restrictions": [],
  "usage": "",
  "verified_claims": [],
  "warnings": [],
  "is_active": true,
  "version": "1.0.0"
}
```

---

# 24. "피해야 할 것" Rule

특정 식품을 피하도록 안내하려면 반드시 검증된 Rule이 있어야 한다.

```text
IF verified_avoid_rule = TRUE
THEN show avoid item
ELSE do not generate avoid claim
```

LLM의 일반 상식만으로 특정 식품이 피부를 악화시킨다고 단정하지 않는다.

---

# 25. Confidence Rule

```text
confidence >= 0.80
→ 일반적인 개인화 솔루션

0.60 <= confidence < 0.80
→ 보수적인 솔루션

confidence < 0.60
→ 제한적인 정보 또는 재촬영
```

Confidence가 낮을수록 제품 추천의 구체성을 줄인다.

---

# 26. 최소 개입 원칙

권장 최대:

```text
Night Care = 3~4개
Morning Care = 2~3개
Inner Care = 기본 1개
```

많은 제품을 추천하는 것이 좋은 솔루션이라는 전제를 사용하지 않는다.

---

# 27. Solution Object

Rule Engine의 최종 결과는 자연어가 아니라 구조화된 JSON으로 만든다.

권장 Schema:

```json
{
  "status": "NORMAL",
  "summary": {
    "primary_state": "BARRIER_RECOVERY",
    "secondary_states": ["HYDRATION"],
    "goal": "BARRIER_SUPPORT",
    "confidence": 0.91
  },
  "night": {
    "purpose": "RECOVERY",
    "steps": [
      {
        "step": 1,
        "product_id": "PSS_001",
        "usage_from_db": true
      }
    ]
  },
  "morning": {
    "purpose": "PROTECTION",
    "steps": []
  },
  "inner_care": {
    "recommended": [],
    "avoid": []
  },
  "caution": null,
  "applied_rules": ["R204", "R412", "R604", "R803"]
}
```

이 객체를 LLM의 입력으로 사용한다.

---

# 28. LLM 입력 최소화

최종 LLM에 가능한 한 다음 정보만 전달한다.

```text
1. 현재 상태
2. 최근 Trend Summary
3. Treatment Context의 필요한 값
4. Safety 결과
5. Rule Engine이 결정한 Goal
6. 선택된 Product ID / 제품 정보
7. 사용법
8. 추천 이유에 필요한 Verified Claim
9. 출력 언어
10. 출력 Schema
```

다음은 전달하지 않는다.

```text
❌ Master Rule 전체
❌ 전체 Rule DB
❌ 전체 Product DB
❌ 전체 사용자 History
❌ 전체 WHS 기록
❌ 모든 과거 이미지
```

---

# 29. Prompt Caching

LLM Provider가 Prompt/Context Caching을 지원한다면 사용한다.

프롬프트 구조:

```text
[CACHEABLE PREFIX]
System Behavior
+
Output Schema
+
변하지 않는 서비스 정책

-------------------------

[DYNAMIC SUFFIX]
Current Skin State
+
Trend
+
Treatment Context
+
Solution Object
```

고정 부분은 앞쪽에 배치하고 매일 변하는 데이터는 뒤쪽에 배치한다.

단, 캐싱은 보조 최적화다.

최우선 최적화는 **불필요한 데이터를 LLM에 보내지 않는 것**이다.

---

# 30. LLM 호출 최적화

가능하면 다음 구조를 사용한다.

```text
Image / Data Analysis
        ↓
Structured State
        ↓
Rule Engine
        ↓
Solution Object
        ↓
LLM
```

즉, LLM을 매 단계의 판단에 호출하지 않는다.

특히 다음 판단은 코드/DB/Rule Engine으로 처리한다.

```text
제품 존재 여부
제품 사용법
제품 활성 여부
알레르기 필터
시술 후 제한
Rule 적용
Routine 단계 수 제한
```

---

# 31. User Skin State Snapshot

매일 분석 후 DB에 Snapshot을 저장한다.

```json
{
  "baseline": {},
  "current": {},
  "trend": {},
  "treatment_context": {},
  "recent_summary": {},
  "confidence": 0.0,
  "updated_at": ""
}
```

다음날에는 전체 과거를 다시 분석하지 않고:

```text
Previous Snapshot
+
Today's Analysis
```

를 이용해 새로운 Snapshot을 계산한다.

---

# 32. 추천 API 구조

백엔드 프레임워크에 관계없이 아래 논리적 API를 구현한다.

## POST /api/skin/analyze

입력:

```json
{
  "user_id": "",
  "image": "",
  "questionnaire": {}
}
```

출력:

```json
{
  "analysis_id": "",
  "current_state": {},
  "image_quality": {},
  "confidence": 0.0
}
```

---

## GET /api/skin/state/{userId}

현재 Snapshot 반환.

```json
{
  "baseline": {},
  "current": {},
  "trend": {},
  "treatment_context": {},
  "recent_summary": {}
}
```

---

## POST /api/solution/generate

입력:

```json
{
  "user_id": "",
  "analysis_id": ""
}
```

처리:

```text
State Load
→ Safety
→ Treatment Rule
→ Priority
→ Goal
→ Product Matching
→ Solution Object
→ LLM
```

출력:

```json
{
  "solution_id": "",
  "night": {},
  "morning": {},
  "inner_care": {},
  "caution": null
}
```

---

## GET /api/solution/today/{userId}

오늘 생성된 솔루션을 반환한다.

가능하면 이미 생성된 솔루션을 재사용하여 불필요한 LLM 호출을 막는다.

---

# 33. Idempotency / 중복 호출 방지

같은 사용자가 같은 날짜에 동일한 입력으로 요청하면 새 LLM 호출을 만들지 않는다.

권장:

```text
solution_cache_key =
user_id
+
date
+
analysis_version
+
treatment_context_version
+
rule_version
+
product_catalog_version
```

동일 key가 존재하면 기존 Solution을 반환한다.

---

# 34. 캐시 전략

최소한 다음을 캐싱한다.

```text
1. Daily Skin Analysis
2. User Skin State Snapshot
3. Today's Solution
4. Product Catalog
5. Treatment Rules
6. Rule Results
```

특히 오늘의 솔루션은 하루 동안 반복 조회되어도 LLM을 다시 호출하지 않는다.

---

# 35. 데이터 변경 시 재생성 조건

다음 경우에만 Solution을 재생성한다.

```text
- 새로운 얼굴 분석 결과
- 사용자가 중요한 문진을 수정
- 새로운 시술 정보 등록
- Treatment Rule version 변경
- Product compatibility 변경
- Safety status 변경
```

단순히 앱 화면을 다시 열었다고 LLM을 호출하지 않는다.

---

# 36. LLM 응답 Schema

최종 LLM은 반드시 JSON Schema를 준수한다.

```json
{
  "headline": "",
  "skin_state_summary": "",
  "today_goal": "",
  "night": {
    "purpose": "",
    "steps": []
  },
  "morning": {
    "purpose": "",
    "steps": []
  },
  "inner_care": {
    "recommended": [],
    "avoid": []
  },
  "caution": ""
}
```

LLM은 전달받은 Solution Object를 변경하거나 새로운 상품을 추가해서는 안 된다.

---

# 37. LLM System Prompt 핵심

파인튜닝 모델 또는 일반 LLM의 System Prompt는 다음 원칙을 포함한다.

```text
You are the user-facing explanation layer of InnerDerma.

You do not independently diagnose medical conditions.
You do not invent products, treatments, usage instructions, ingredients, benefits, or restrictions.
You may only use the structured solution and verified knowledge provided by the backend.
You must preserve the backend's safety status, selected products, routine order, and usage instructions.
You must not add products that are not present in the input.
You must not override Rule Engine decisions.
You must explain the recommendation clearly and concisely.
You must distinguish Night Care from Morning Care.
If safety status is CAUTION, prioritize the caution message over product promotion.
Return the required JSON structure.
```

---

# 38. Hallucination 방지

LLM이 다음을 생성하면 실패로 처리한다.

```text
- DB에 없는 product_id
- DB에 없는 product_name
- DB와 다른 usage
- DB에 없는 효능
- DB에 없는 treatment
- 새로운 medical diagnosis
- Rule Engine에서 선택하지 않은 상품
```

Response Validator에서 검증한다.

```text
LLM Response
↓
JSON Schema Validation
↓
Product ID Validation
↓
Usage Validation
↓
Safety Validation
↓
PASS / FAIL
```

FAIL이면 사용자에게 직접 보여주지 않는다.

---

# 39. Response Validator

권장 검증 항목:

```text
1. JSON Schema
2. Product ID exists
3. Product active
4. Usage matches DB
5. Routine step count
6. Safety status
7. Applied rule compatibility
8. No unknown product
9. No forbidden claims
```

---

# 40. 파인튜닝 데이터 형식

파인튜닝 데이터는 단순한 FAQ가 아니라 구조화된 의사결정 예제로 만든다.

권장 형태:

```json
{
  "messages": [
    {
      "role": "system",
      "content": "InnerDerma user-facing explanation policy..."
    },
    {
      "role": "user",
      "content": {
        "solution": {
          "summary": {},
          "night": {},
          "morning": {},
          "inner_care": {}
        }
      }
    },
    {
      "role": "assistant",
      "content": {
        "headline": "",
        "skin_state_summary": "",
        "today_goal": "",
        "night": {},
        "morning": {},
        "inner_care": {},
        "caution": ""
      }
    }
  ]
}
```

---

# 41. 파인튜닝 데이터가 학습해야 하는 것

### 학습 대상

```text
Solution Object
→ Natural Language Response
```

### 학습 대상이 아닌 것

```text
Raw Photo
→ Medical Diagnosis
```

또는:

```text
전체 Product DB
→ Product Selection
```

제품 선택은 Rule Engine이 담당한다.

---

# 42. 파인튜닝 데이터 카테고리

반드시 다양한 사례를 포함한다.

## Normal

- 안정적인 피부
- 건조
- 수분 부족
- 붉어짐
- 트러블
- 유분 증가

## Recovery

- 시술 직후
- 회복 중
- 회복 완료
- 회복 중 개선

## Trend

- Improving
- Stable
- Worsening
- Unknown

## Safety

- 위험 신호
- 전문가 확인 필요
- 이미지 품질 불량
- 데이터 부족

## Product

- 적합
- 부적합
- 시술 후 제한
- 정보 부족

## Inner Care

- 정상 추천
- 알레르기
- 식이 제한
- 정보 부족

---

# 43. 개발 단계

## Phase 1 — DB / Knowledge Base

먼저 다음을 구현한다.

```text
WHS Diagnosis
WHS Treatment
Treatment Rules
Piece Seoul Products
WIM Products
Rule DB
```

이 단계에서는 LLM을 연결하지 않아도 된다.

---

## Phase 2 — Skin State Engine

구현:

```text
Image Analysis Result
→ Current State
→ Trend
→ Treatment Context
→ Snapshot
```

---

## Phase 3 — Rule Engine

구현 순서:

```text
Safety
→ Treatment
→ Priority
→ Goal
→ Night
→ Morning
→ Product
→ Inner Care
```

---

## Phase 4 — Solution Object

Rule Engine의 결과를 완전한 JSON Object로 만든다.

LLM 없이도 테스트 가능해야 한다.

---

## Phase 5 — LLM

Solution Object를 LLM에 전달한다.

LLM은 설명만 생성한다.

---

## Phase 6 — Validator

LLM 응답을 검증한다.

---

## Phase 7 — Cache / Optimization

구현:

```text
Daily Analysis Cache
Solution Cache
Product Cache
Rule Cache
Prompt Cache
```

---

# 44. 테스트 전략

Rule Engine은 LLM 없이도 테스트할 수 있어야 한다.

예:

```text
INPUT:
dryness=4
redness=2
trend=dryness_worsening
days_since_treatment=7

EXPECTED:
goal=BARRIER_SUPPORT
night.purpose=RECOVERY
```

또 다른 테스트:

```text
INPUT:
rapidly_worsening=true

EXPECTED:
safety=CAUTION
no_aggressive_product_addition=true
```

또 다른 테스트:

```text
INPUT:
allergy=["X"]
product allergen=["X"]

EXPECTED:
product=REJECTED
```

---

# 45. Golden Test Set

최소한 다음 유형의 고정 테스트 데이터를 만든다.

```text
10 Normal
10 Recovery
10 Worsening
10 Improving
10 Treatment-specific
10 Safety
10 Product restriction
10 WIM restriction
10 Low confidence
10 Image quality failure
```

총 100개 이상의 Golden Case를 만들고 Rule Engine 변경 때마다 regression test를 실행한다.

---

# 46. 비용 최적화 체크리스트

반드시 다음을 적용한다.

```text
[ ] 전체 Rule을 LLM에 보내지 않는다.
[ ] 전체 Product DB를 LLM에 보내지 않는다.
[ ] 전체 User History를 LLM에 보내지 않는다.
[ ] 매일 Snapshot을 생성한다.
[ ] Rule Engine에서 먼저 판단한다.
[ ] Product Matching을 코드/DB에서 먼저 한다.
[ ] 동일 날짜 Solution을 캐싱한다.
[ ] 동일 입력 중복 호출을 막는다.
[ ] 고정 Prompt는 caching한다.
[ ] LLM은 최종 자연어 생성에 집중시킨다.
[ ] 짧은 출력 Schema를 사용한다.
[ ] 불필요한 Chain-of-Thought 출력을 요청하지 않는다.
```

---

# 47. 비용 최적화의 최종 목표

매일 1명의 사용자에 대해:

```text
Raw Data
→ Image Analysis
→ State Snapshot
→ Rule Engine
→ Solution Object
→ 최소 1회의 LLM
```

을 기본 구조로 한다.

가능한 경우 Solution이 이미 존재하면:

```text
LLM 호출 = 0
```

으로 처리한다.

즉 사용자가 앱을 열 때마다 LLM을 호출하는 구조가 아니라:

> **새로운 상태가 발생했을 때만 AI 처리를 수행한다.**

---

# 48. 권장 데이터 흐름

```text
[WHS]
  │
  ├── Diagnosis
  └── Treatment
        │
        ↓
[Treatment Knowledge Base]
        │
        ↓
[User Baseline]

매일:

[Face Photo]
      +
[Questionnaire]
      +
[Previous Snapshot]
      ↓
[Analysis]
      ↓
[Current State]
      ↓
[Trend]
      ↓
[Safety Gate]
      ↓
[Rule Engine]
      ↓
[Product Matching]
      ↓
[Solution Object]
      ↓
[LLM]
      ↓
[Validator]
      ↓
[Daily Solution]
      ↓
[App UI]
```

---

# 49. 개발 에이전트에 대한 구현 지시

## 반드시 지켜야 한다.

### 1.

Master Rule을 매번 LLM Prompt에 넣는 방식으로 구현하지 말 것.

### 2.

Rule을 코드/DB에서 실행 가능한 구조로 변환할 것.

### 3.

LLM 호출 전에 Rule Engine이 먼저 실행될 것.

### 4.

Product Matching은 DB에서 수행할 것.

### 5.

LLM이 Product Selection을 독립적으로 변경하지 못하도록 할 것.

### 6.

User History는 Snapshot/Trend로 압축할 것.

### 7.

Daily Solution은 캐싱할 것.

### 8.

LLM 응답은 JSON Schema로 검증할 것.

### 9.

Safety Rule은 항상 최우선으로 실행할 것.

### 10.

의학적/건강 관련 사실은 검증된 Knowledge Base를 벗어나 생성하지 않을 것.

---

# 50. 최종 시스템 요약

InnerDerma의 AI는 다음 구조를 따른다.

```text
             RAW DATA
                 │
                 ↓
       ┌─────────────────┐
       │ Analysis Layer  │
       └────────┬────────┘
                ↓
        STRUCTURED STATE
                │
                ↓
       ┌─────────────────┐
       │  State / Trend  │
       └────────┬────────┘
                ↓
       ┌─────────────────┐
       │   Rule Engine   │
       └────────┬────────┘
                ↓
       ┌─────────────────┐
       │ Product Matcher │
       └────────┬────────┘
                ↓
        SOLUTION OBJECT
                │
                ↓
       ┌─────────────────┐
       │       LLM       │
       │ Explanation Only│
       └────────┬────────┘
                ↓
           VALIDATOR
                │
                ↓
             APP UI
```

핵심 철학:

> **LLM이 모든 것을 판단하도록 만들지 않는다.**
>
> **Rule Engine이 무엇을 해야 하는지를 결정하고, LLM은 그것을 사용자가 이해하기 좋은 솔루션으로 전달한다.**

이 구조를 사용하면 Rule의 정보량은 계속 확장할 수 있으면서도, 매일 LLM에 전달되는 Context와 크레딧 사용량은 최소화할 수 있다.

---

# 51. 개발 완료 기준

백엔드 개발이 완료되었다고 판단하려면 다음이 가능해야 한다.

```text
[ ] WHS 진단 데이터를 저장할 수 있다.
[ ] WHS 시술 데이터를 저장할 수 있다.
[ ] 시술 후 경과일을 자동 계산한다.
[ ] Daily Face Analysis 결과를 저장한다.
[ ] User Skin State Snapshot을 생성한다.
[ ] Trend를 계산한다.
[ ] Safety Gate를 실행한다.
[ ] Rule ID를 선택한다.
[ ] Today's Goal을 결정한다.
[ ] Piece Seoul 제품을 Rule에 따라 필터링한다.
[ ] WIM 제품을 Rule에 따라 필터링한다.
[ ] Solution Object를 생성한다.
[ ] 동일 Solution에 대한 중복 LLM 호출을 막는다.
[ ] LLM은 Solution Object를 자연어로 변환한다.
[ ] LLM 응답을 Schema Validation한다.
[ ] 존재하지 않는 상품을 차단한다.
[ ] Safety 상태를 LLM이 임의로 변경하지 못한다.
[ ] Night / Morning / Inner Care를 분리한다.
[ ] 100개 이상의 Golden Test를 통과한다.
[ ] Master Rule 변경 시 Rule Version을 관리한다.
[ ] Product 변경 시 Product Version을 관리한다.
```

---

# 52. 개발 우선순위 한 줄 요약

```text
DB
→ Knowledge Base
→ State Engine
→ Trend Engine
→ Safety Gate
→ Rule Engine
→ Product Matcher
→ Solution Object
→ LLM
→ Validator
→ Cache
→ API
→ App Integration
```

**이 순서를 기준으로 개발한다.**

