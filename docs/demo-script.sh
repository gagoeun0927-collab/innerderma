#!/bin/bash
# InnerDerma E2E Demo Script
# Prerequisites: server running on localhost:8080, OPENAI_API_KEY set
# Usage: bash docs/demo-script.sh

BASE_URL="http://localhost:8080"
USER_CODE="WHS-DEMO-001"

echo "=== InnerDerma E2E Demo ==="
echo ""

# 1. JWT 토큰 발급
echo "1. JWT 토큰 발급"
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/token?userCode=$USER_CODE" | jq -r '.data.token')
echo "   Token: ${TOKEN:0:30}..."
echo ""

AUTH="Authorization: Bearer $TOKEN"

# 2. 사용자 정보 조회
echo "2. 사용자 정보 조회"
curl -s -H "$AUTH" "$BASE_URL/api/users/$USER_CODE" | jq '.data'
echo ""

# 3. 언어 설정 변경 (한국어)
echo "3. 언어 설정 → ko"
curl -s -X PATCH -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"locale":"ko"}' \
  "$BASE_URL/api/users/$USER_CODE/preference" | jq '.data'
echo ""

# 4. 자가문진 등록
echo "4. 자가문진 등록"
curl -s -X POST -H "$AUTH" -H "Content-Type: application/json" \
  -d '{
    "pain":"NONE","heatSensation":"MILD","tightness":"MODERATE",
    "dryness":"SEVERE","itching":"MILD","swelling":"NONE",
    "peeling":"MILD","breakout":"NONE",
    "oozing":"NONE","bleeding":"NONE","barrierDamage":"NONE"
  }' \
  "$BASE_URL/api/users/$USER_CODE/self-checks" | jq '.data | {id, checkedAt, pain, dryness}'
echo ""

# 5. 피부 상태 스냅샷 생성
echo "5. 피부 상태 스냅샷 생성 (자가문진 기반)"
curl -s -X POST -H "$AUTH" "$BASE_URL/api/users/$USER_CODE/skin-state-snapshots" | jq '.data | {id, snapshotDate, dominantSymptom, scoringVersion}'
echo ""

# 6. AI Care 생성 (핵심 파이프라인)
echo "6. AI Care 생성 (Rule Engine → Product Matcher → LLM)"
curl -s -X POST -H "$AUTH" "$BASE_URL/api/users/$USER_CODE/ai-care?locale=ko" | jq '.'
echo ""

# 7. AI Care 재호출 (캐시 적중 확인)
echo "7. AI Care 재호출 (캐시 적중 → LLM 미호출)"
curl -s -X POST -H "$AUTH" "$BASE_URL/api/users/$USER_CODE/ai-care?locale=ko" | jq '{validated: .data.validated, cached: "same-day cache hit"}'
echo ""

# 8. AI 규칙 목록 조회
echo "8. AI 규칙 목록"
curl -s -H "$AUTH" "$BASE_URL/api/ai-rules" | jq '.data | length'
echo "   rules loaded"
echo ""

# 9. 제품 카탈로그 조회
echo "9. 제품 카탈로그"
curl -s "$BASE_URL/api/products" | jq '.data | length'
echo "   products"
echo ""

# 10. Health Check
echo "10. Health Check"
curl -s "$BASE_URL/api/innerderma/health" | jq '.'
echo ""

echo "=== Demo Complete ==="
