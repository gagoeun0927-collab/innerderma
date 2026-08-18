# Golden Test Cases

Rule Engine의 결정적 동작을 검증하는 고정 테스트 데이터.

## 파일 형식

각 JSON 파일은 하나의 Golden Test Case를 정의한다:

```json
{
  "id": "GT-001",
  "category": "SAFETY",
  "description": "SEVERE 증상 입력 시 Safety Gate 발화",
  "input": {
    "signals": {
      "requires_safety_attention": true,
      "trend_stable": true,
      "has_severe_symptom": true
    }
  },
  "expected": {
    "fired_rules": ["R000", "R010"],
    "not_fired_rules": ["R002"],
    "actions_contain": {
      "safety_status": "CAUTION",
      "recommendation_mode": "CONSERVATIVE"
    },
    "restrictions_contain": ["NO_AGGRESSIVE_ROUTINE"]
  }
}
```

## 카테고리 (§45 기준)

- NORMAL: 안정적 피부
- RECOVERY: 시술 회복
- WORSENING: 악화 추세
- IMPROVING: 개선 추세
- SAFETY: 위험 신호
- PRODUCT_RESTRICTION: 제품 제한
- LOW_CONFIDENCE: 데이터 부족
- IMAGE_QUALITY: 이미지 품질

## 목표

최소 100개 이상의 Golden Case를 축적하고 Rule Engine 변경 때마다 regression test를 실행한다.
