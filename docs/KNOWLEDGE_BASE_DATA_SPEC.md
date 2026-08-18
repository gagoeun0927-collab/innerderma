# InnerDerma Knowledge Base 데이터 수집 명세

이 문서는 **제품 DB**와 **시술(Treatment) Knowledge Base**에 어떤 정보가 필요하고, 수집한 데이터를 어떤 형식으로 전달해야 하는지를 정리합니다. 백엔드 개발자가 아닌 **데이터 수집 담당자**가 읽고 작업할 수 있도록 구체적으로 기술합니다.

---

## 1. Piece Seoul 스킨케어 제품 (피부 외용 제품)

### 1.1 용도

사용자의 피부 상태·시술 이력·안전 규칙에 맞는 **Night Care / Morning Care 제품**을 자동 추천하기 위한 기반 데이터입니다. Rule Engine이 이 DB에서 조건에 맞는 제품을 필터링합니다.

### 1.2 수집해야 하는 항목

| 필드 | 설명 | 예시 | 필수 |
|---|---|---|---|
| `product_id` | 고유 제품 코드 (시스템 식별자) | `PSS_001` | ✅ |
| `brand` | 브랜드명 | `Piece Seoul` | ✅ |
| `name` | 제품명 (정확한 공식 명칭) | `Cica Barrier Cream` | ✅ |
| `category` | 제품 유형 | 아래 목록 참조 | ✅ |
| `tags` | 제품 특성 태그 (복수 가능) | `["barrier", "soothing", "ceramide"]` | ✅ |
| `skin_state_tags` | 이 제품이 적합한 피부 상태 | `["BARRIER_RECOVERY", "HYDRATION", "REDNESS"]` | ✅ |
| `treatment_compatibility` | 이 제품을 사용해도 되는 시술 유형 | `["laser_toning", "hydrafacial"]` | ✅ |
| `restricted_after_treatments` | 이 제품을 **사용하면 안 되는** 시술 유형 + 제한 기간 | `[{"treatment": "deep_peel", "days": 14}]` | ✅ |
| `usage_time` | 사용 시점 | `["night"]` 또는 `["morning"]` 또는 `["night", "morning"]` | ✅ |
| `frequency` | 사용 빈도 | `"daily"` 또는 `"every_other_day"` | ✅ |
| `amount` | 1회 사용량 | `"1~2 pumps"` 또는 `"fingertip amount"` | ✅ |
| `application_method` | 사용 방법 (간결하게) | `"세안 후 토너 다음 단계에 얼굴 전체 도포"` | ✅ |
| `warnings` | 주의사항 | `["눈가 피하기", "자극 시 중단"]` | 해당 시 |
| `verified_claims` | **검증된** 효능/설명 (제조사/전문가 확인된 것만) | `["세라마이드 기반 장벽 강화", "자극 완화"]` | ✅ |
| `ingredients_highlight` | 핵심 성분 (전 성분이 아닌 주요 성분만) | `["ceramide NP", "madecassoside", "panthenol"]` | 권장 |
| `allergens` | 포함된 알레르기 유발 가능 성분 | `["fragrance", "ethanol"]` 또는 `[]` | ✅ |
| `is_active` | 현재 판매/추천 가능 여부 | `true` | ✅ |
| `price` | 가격 (원화 기준, 정수) | `38000` | 권장 |
| `official_url` | 구매/상세 페이지 URL | `"https://..."` | 권장 |
| `image_url` | 제품 이미지 URL | `"https://..."` | 권장 |

### 1.3 category 허용값

```
CLEANSER        — 클렌저/세안제
TONER           — 토너/스킨
SERUM           — 세럼/에센스/앰플
MOISTURIZER     — 크림/로션/수분제
SUNSCREEN       — 자외선 차단제
TARGETED_CARE   — 스팟/집중 케어
MASK            — 마스크팩/워시오프팩
OIL             — 페이스 오일
```

### 1.4 skin_state_tags 허용값

```
BARRIER_RECOVERY   — 장벽 회복이 필요한 상태
HYDRATION          — 수분 부족/건조
REDNESS            — 붉은기
IRRITATION         — 자극/민감
ACNE               — 트러블/여드름
SEBUM              — 유분 과다
PIGMENTATION       — 색소침착/기미
SWELLING           — 부기
STABLE             — 안정적인 피부 (일반 유지 관리)
```

### 1.5 JSON 예시

```json
{
  "product_id": "PSS_001",
  "brand": "Piece Seoul",
  "name": "Cica Barrier Cream",
  "category": "MOISTURIZER",
  "tags": ["barrier", "soothing", "ceramide", "sensitive"],
  "skin_state_tags": ["BARRIER_RECOVERY", "HYDRATION", "IRRITATION"],
  "treatment_compatibility": ["laser_toning", "hydrafacial", "aquapeel"],
  "restricted_after_treatments": [
    {"treatment": "deep_peel", "restrict_days": 14},
    {"treatment": "fractional_laser", "restrict_days": 7}
  ],
  "usage_time": ["night"],
  "frequency": "daily",
  "amount": "fingertip amount",
  "application_method": "세안 후 토너 다음 단계에 얼굴 전체 부드럽게 도포",
  "warnings": ["눈 주위 피하기"],
  "verified_claims": ["세라마이드 기반 장벽 강화", "CICA 성분으로 자극 완화"],
  "ingredients_highlight": ["ceramide NP", "centella asiatica extract", "panthenol"],
  "allergens": [],
  "is_active": true,
  "price": 38000,
  "official_url": "https://pieceseoul.com/products/cica-barrier-cream",
  "image_url": "https://cdn.pieceseoul.com/images/pss_001.jpg"
}
```

---

## 2. WIM Store 이너케어 제품 (섭취형 제품)

### 2.1 용도

사용자의 피부 상태에 맞는 **Inner Care**(건강기능식품, 음료, 보충제 등) 추천용 데이터입니다. 알레르기·식이 제한 필터가 중요합니다.

### 2.2 수집해야 하는 항목

| 필드 | 설명 | 예시 | 필수 |
|---|---|---|---|
| `product_id` | 고유 제품 코드 | `WIM_001` | ✅ |
| `brand` | 브랜드명 | `WIM Store` | ✅ |
| `name` | 제품명 | `콜라겐 부스터 젤리` | ✅ |
| `category` | 제품 유형 | 아래 목록 참조 | ✅ |
| `state_tags` | 이 제품이 도움이 되는 피부 상태 | `["HYDRATION", "BARRIER_RECOVERY"]` | ✅ |
| `dietary_tags` | 식이 특성 | `["vegan", "gluten_free", "sugar_free"]` | ✅ |
| `allergens` | 알레르기 유발 성분 | `["fish_collagen", "soy"]` | ✅ |
| `restrictions` | 섭취 제한 조건 | `["pregnant", "under_18"]` | 해당 시 |
| `usage` | 섭취 방법/용량 | `"1일 1포, 식후 섭취"` | ✅ |
| `verified_claims` | **검증된** 효능 설명 (인허가/전문가 확인) | `["저분자 콜라겐 1000mg 함유"]` | ✅ |
| `ingredients_highlight` | 핵심 성분 | `["fish collagen peptide", "vitamin C", "hyaluronic acid"]` | ✅ |
| `warnings` | 주의사항 | `["과다 섭취 시 설사 가능", "임산부 섭취 전 전문가 상담"]` | 해당 시 |
| `is_active` | 현재 판매/추천 가능 여부 | `true` | ✅ |
| `price` | 가격 (원화 기준) | `45000` | 권장 |
| `official_url` | 구매/상세 URL | `"https://..."` | 권장 |
| `image_url` | 제품 이미지 URL | `"https://..."` | 권장 |

### 2.3 category 허용값

```
SUPPLEMENT      — 건강기능식품/보충제 (캡슐, 정제)
DRINK           — 음료/액상
JELLY           — 젤리/구미
POWDER          — 분말
FOOD            — 식품 (스낵, 바 등)
```

### 2.4 JSON 예시

```json
{
  "product_id": "WIM_001",
  "brand": "WIM Store",
  "name": "콜라겐 부스터 젤리",
  "category": "JELLY",
  "state_tags": ["HYDRATION", "BARRIER_RECOVERY"],
  "dietary_tags": ["gluten_free"],
  "allergens": ["fish_collagen"],
  "restrictions": [],
  "usage": "1일 1포(20g), 식후 섭취",
  "verified_claims": ["저분자 피쉬 콜라겐 1000mg 함유", "비타민C 100mg 함유"],
  "ingredients_highlight": ["fish collagen peptide", "vitamin C", "hyaluronic acid"],
  "warnings": ["어류 알레르기 주의"],
  "is_active": true,
  "price": 45000,
  "official_url": "https://wimstore.com/products/collagen-booster-jelly",
  "image_url": "https://cdn.wimstore.com/images/wim_001.jpg"
}
```

---

## 3. Treatment Knowledge Base (시술 회복 규칙)

### 3.1 용도

사용자가 받은 시술 유형에 따라 **회복 기간 동안 어떤 제품/행동을 제한하고 허용해야 하는지**를 Rule Engine이 판단하기 위한 근거 데이터입니다. 이 데이터가 없으면 시술 후 사후관리 추천이 불가능합니다.

### 3.2 수집해야 하는 항목

| 필드 | 설명 | 예시 | 필수 |
|---|---|---|---|
| `treatment_code` | 시술 고유 코드 | `LASER_TONING` | ✅ |
| `treatment_name` | 시술명 (한국어) | `레이저 토닝` | ✅ |
| `treatment_name_en` | 시술명 (영어) | `Laser Toning` | ✅ |
| `treatment_type` | 시술 유형 분류 | 아래 목록 참조 | ✅ |
| `treatment_area` | 시술 부위 | `["face"]` 또는 `["face", "neck"]` | ✅ |
| `expected_recovery_days_min` | 최소 회복 기간 (일) | `3` | ✅ |
| `expected_recovery_days_max` | 최대 회복 기간 (일) | `7` | ✅ |
| `normal_symptoms` | 회복 기간 중 **정상** 증상 | `["mild redness", "slight warmth"]` | ✅ |
| `warning_symptoms` | 이 증상이 나타나면 **전문가 확인 필요** | `["severe swelling", "pus", "spreading redness"]` | ✅ |
| `aftercare_restrictions` | 회복 기간 중 금지/제한 행동 | `["direct sun exposure", "sauna", "alcohol-based products"]` | ✅ |
| `allowed_product_tags` | 회복 기간 중 사용 **가능한** 제품 태그 | `["soothing", "barrier", "mineral_sunscreen"]` | ✅ |
| `restricted_product_tags` | 회복 기간 중 사용 **금지** 제품 태그 | `["retinol", "AHA", "BHA", "scrub", "peel"]` | ✅ |
| `aftercare_guide` | 사후관리 안내 문장 (사용자에게 보여줄 수 있는) | `"시술 후 3일간 자외선 차단제 필수, 세안 시 미온수 사용"` | ✅ |
| `source` | 데이터 출처 | `"WHS dermatologist"` 또는 `"manufacturer guideline"` | ✅ |
| `version` | 이 규칙의 버전 | `"1.0.0"` | ✅ |

### 3.3 treatment_type 허용값

```
LASER_TONING        — 레이저 토닝
FRACTIONAL_LASER    — 프랙셔널 레이저
IPL                 — IPL (광선 치료)
HYDRAFACIAL         — 하이드라페이셜
AQUAPEEL            — 아쿠아필
DEEP_PEEL           — 딥 필링
CHEMICAL_PEEL       — 케미컬 필링
MICRONEEDLING       — 마이크로니들링/MTS
BOTOX               — 보톡스
FILLER              — 필러
RF_TREATMENT        — 고주파 시술 (써마지/인모드 등)
LED_THERAPY         — LED 테라피
INJECTION           — 주사 시술 (물광, 비타민 등)
OTHER               — 기타
```

### 3.4 JSON 예시

```json
{
  "treatment_code": "LASER_TONING",
  "treatment_name": "레이저 토닝",
  "treatment_name_en": "Laser Toning",
  "treatment_type": "LASER_TONING",
  "treatment_area": ["face"],
  "expected_recovery_days_min": 3,
  "expected_recovery_days_max": 7,
  "normal_symptoms": [
    "mild redness (1-2 days)",
    "slight warmth sensation",
    "minor flaking after day 3"
  ],
  "warning_symptoms": [
    "severe swelling lasting over 3 days",
    "pus or discharge",
    "spreading redness beyond treatment area",
    "intense burning sensation"
  ],
  "aftercare_restrictions": [
    "direct sun exposure without SPF50+",
    "sauna or hot bath for 3 days",
    "alcohol-based toner",
    "retinol products for 7 days",
    "AHA/BHA exfoliation for 7 days",
    "physical scrub for 7 days"
  ],
  "allowed_product_tags": [
    "soothing",
    "barrier",
    "ceramide",
    "centella",
    "mineral_sunscreen",
    "gentle_cleanser"
  ],
  "restricted_product_tags": [
    "retinol",
    "AHA",
    "BHA",
    "scrub",
    "peel",
    "high_concentration_vitamin_c",
    "alcohol"
  ],
  "aftercare_guide": "시술 후 3일간 자외선 차단제(SPF50+) 필수 사용. 세안 시 미온수로 부드럽게. 7일간 각질 제거 제품 사용 금지. 가벼운 붉은기와 각질은 정상적인 회복 과정입니다.",
  "source": "WHS dermatologist",
  "version": "1.0.0"
}
```

---

## 4. 수집 시 주의사항

### 절대 하지 말 것
- 인터넷 일반 블로그에서 검증 없이 효능/주의사항을 가져오기
- 제조사가 공인하지 않은 효능을 `verified_claims`에 넣기
- 실제 판매하지 않는 제품을 `is_active: true`로 등록
- 시술 회복 기간을 추측으로 작성 (반드시 전문가 확인 또는 공식 가이드 근거)

### 반드시 확인할 것
- `verified_claims`: 제조사 공식 페이지, 식약처 인증, 또는 WHS 전문가가 확인한 내용만
- `allergens`: 성분표에서 확인 가능한 알레르기 유발 물질
- `restricted_after_treatments` / `restricted_product_tags`: WHS 피부과 전문의 또는 시술 가이드라인 근거
- `warning_symptoms`: 의료 전문가가 정의한 비정상 증상만 (일반 상식으로 추가 금지)

### 데이터 출처 기록
모든 항목에 데이터를 어디서 가져왔는지 **내부적으로라도 추적**해 주세요. 나중에 정보 업데이트나 검증 시 출처를 알아야 합니다.

---

## 5. 전달 형식

- 파일: **JSON 배열** (`.json`)
- 인코딩: UTF-8
- 파일명 규칙:
  - `piece_seoul_products.json` — Piece Seoul 제품 목록
  - `wim_store_products.json` — WIM Store 제품 목록
  - `treatment_knowledge_base.json` — 시술 회복 규칙 목록

각 파일은 JSON 배열이며 항목 하나가 위의 JSON 예시와 동일한 구조입니다:

```json
[
  { "product_id": "PSS_001", ... },
  { "product_id": "PSS_002", ... }
]
```

---

## 6. 최소 수량 가이드

| 항목 | 최소 수량 | 이유 |
|---|---|---|
| Piece Seoul 제품 | 10개 이상 | Night/Morning 각 단계별 최소 2~3개 후보가 필요 |
| WIM Store 제품 | 5개 이상 | Inner Care 추천에 최소 선택지 필요 |
| Treatment 규칙 | WHS에서 실제로 시행하는 시술 전부 | 시술 후 관리의 핵심 |

모든 카테고리(`category`)와 주요 피부 상태(`skin_state_tags`)에 최소 1개 이상 제품이 있어야 Rule Engine이 매칭할 수 있습니다.

---

## 7. 현재 시스템에 이미 있는 것 (참고)

현재 백엔드에는 **데모 제품 7건**(전부 `demoProduct=true`, `officialUrl=null`)만 있습니다. 기존 필드:
- productCode, brand, name, category(CLEANSER/MOISTURIZER/SUNSCREEN/TARGETED_CARE), targetConcern, safetyAttentionCompatible, active, displayPriority

이번에 수집하는 데이터로 이 데모 제품을 **교체**하게 됩니다. 기존 필드에 더해 `tags`, `skin_state_tags`, `treatment_compatibility`, `usage_time`, `application_method`, `verified_claims`, `allergens` 등이 새로 추가됩니다.

시술 데이터는 현재 `procedure_records` 테이블에 기록 구조는 있으나, **시술 유형별 회복 규칙(Knowledge Base)이 전무**합니다. `care_guide` 문장 하나만 존재합니다.

---

**이 문서에 따라 데이터를 수집해서 JSON 파일로 전달해 주시면, 백엔드에서 Product DB 확장 + Treatment KB 구축 + Rule Engine 제품 매칭을 구현하겠습니다.**
