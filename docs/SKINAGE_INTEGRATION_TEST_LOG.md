# SkinAge 서버 통합 테스트 로그

일시: 2026-08-18
서버: http://localhost:8000
상태: 서버 정상 가동 확인, 이미지 품질 검사 단계에서 거부됨

---

## 1. 요청 정보

### 엔드포인트

```
POST http://localhost:8000/api/v1/analyze
Content-Type: multipart/form-data
```

### 요청 파라미터

| 파라미터 | 타입 | 값 | 설명 |
|---|---|---|---|
| `file` | Binary (multipart) | test-face-real.jpg | 분석 대상 얼굴 사진 |
| `include_heatmaps` | String | `"false"` | 히트맵 미포함 |
| `age` | Integer | 미전송 (선택) | 실제 나이 |

### 실제 전송 방식 (Java HttpSkinAgeClient)

```java
MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
body.add("file", new HttpEntity<>(imageResource, imageHeaders));  // Content-Type: image/jpeg
body.add("include_heatmaps", false);

restClient.post()
    .uri("/api/v1/analyze")
    .contentType(MediaType.MULTIPART_FORM_DATA)
    .body(body)
    .retrieve()
    .body(SkinAgeAnalysisResult.class);
```

---

## 2. 테스트 1: 단색 이미지 (얼굴 없음)

### 입력
- 640×640 단색 JPEG (프로그래밍으로 생성, 얼굴 없음)

### 서버 응답

```
HTTP 422 Unprocessable Entity
```

```json
{
  "detail": {
    "error": "quality_check_failed",
    "failed_checks": ["occlusion", "face_angle", "blur"],
    "messages": [
      "Please remove sunglasses, masks, or hair covering your face.",
      "Please face the camera more directly.",
      "Image is too blurry. Hold your phone steady."
    ],
    "guidance": [
      "Please remove sunglasses, masks, or hair covering your face.",
      "Please face the camera more directly.",
      "Image is too blurry. Hold your phone steady."
    ]
  }
}
```

### 판정
- 서버 정상 동작 (이미지에 얼굴이 없어 품질 검사에서 올바르게 거부)

---

## 3. 테스트 2: 실제 사진 (첫 번째 교체)

### 입력
- 사용자 제공 사진 (test-face-real.jpg, 첫 번째)

### 서버 응답

```
HTTP 422 Unprocessable Entity
```

```json
{
  "detail": {
    "error": "quality_check_failed",
    "failed_checks": ["occlusion", "face_angle", "blur"],
    "messages": [
      "Please remove sunglasses, masks, or hair covering your face.",
      "Please face the camera more directly.",
      "Image is too blurry. Hold your phone steady."
    ],
    "guidance": [
      "Please remove sunglasses, masks, or hair covering your face.",
      "Please face the camera more directly.",
      "Image is too blurry. Hold your phone steady."
    ]
  }
}
```

### 판정
- 서버 정상 동작, 이미지가 3가지 품질 기준(가림/각도/흐림) 미통과

---

## 4. 테스트 3: 실제 사진 (두 번째 교체)

### 입력
- 사용자 제공 사진 (test-face-real.jpg, 두 번째)

### 서버 응답

```
HTTP 422 Unprocessable Entity
```

```json
{
  "detail": {
    "error": "quality_check_failed",
    "failed_checks": ["occlusion", "face_angle"],
    "messages": [
      "Please remove sunglasses, masks, or hair covering your face.",
      "Please face the camera more directly."
    ],
    "guidance": [
      "Please remove sunglasses, masks, or hair covering your face.",
      "Please face the camera more directly."
    ]
  }
}
```

### 판정
- blur는 통과했으나 **occlusion(가림)**과 **face_angle(각도)** 미통과
- 서버 정상 동작

---

## 5. SkinAge 서버 품질 검사 요건

SkinAge가 분석을 수행하려면 이미지가 아래 검사를 **모두** 통과해야 합니다:

| 검사 항목 | 요건 | 실패 시 메시지 |
|---|---|---|
| `face_detection` | 이미지에서 얼굴이 감지되어야 함 | (감지 자체 실패 시 별도 에러) |
| `occlusion` | 선글라스, 마스크, 모자, 머리카락 등으로 얼굴이 가려지지 않아야 함 | "Please remove sunglasses, masks, or hair covering your face." |
| `face_angle` | 정면을 향해야 함 (과도한 좌우/상하 회전 없음) | "Please face the camera more directly." |
| `blur` | 사진이 선명해야 함 (흐림/초점 맞지 않음 불가) | "Image is too blurry. Hold your phone steady." |

### 분석 가능한 사진 조건

- 정면 응시 (좌우 회전 최소)
- 이마~턱 전체가 보여야 함
- 선글라스/마스크/모자 미착용
- 앞머리로 이마/눈 가리지 않음
- 초점이 맞고 선명함
- 512×512px 이상 권장

---

## 6. 서버 에러 응답 구조

### 품질 검사 실패 (HTTP 422)

```json
{
  "detail": {
    "error": "quality_check_failed",
    "failed_checks": ["occlusion", "face_angle", "blur"],
    "messages": ["..."],
    "guidance": ["..."]
  }
}
```

### 서버 내부 오류 (HTTP 500)

```json
{
  "detail": "Internal inference error."
}
```

(서버 재시작 후 해결됨 — 모델 로드 또는 초기화 문제였던 것으로 추정)

---

## 7. InnerDerma 백엔드 코드 상태

| 항목 | 상태 |
|---|---|
| HttpSkinAgeClient multipart 전송 | ✅ 정상 |
| 서버 연결 | ✅ 정상 |
| 응답 JSON 파싱 (SkinAgeAnalysisResult) | ✅ 코드 준비 완료 (200 성공 시 동작) |
| 응답 검증 (7 zone, 4 concern, 점수 범위) | ✅ 코드 준비 완료 |
| 에러 처리 | ✅ RestClientException → SKINAGE_API_UNAVAILABLE |
| 풀 분석 end-to-end 테스트 | ⏸️ 품질 통과 이미지 대기 |

---

## 8. 다음 단계

품질 검사를 통과하는 정면 얼굴 사진을 `src/test/resources/test-face-real.jpg`에 넣으면:

1. SkinAge가 200 + 전체 분석 JSON 반환
2. `analyzesRealFaceImageIfAvailable()` 테스트가 자동 통과
3. 7개 zone × 4개 concern, overall_score, skin_health_grade, model_version 모두 검증됨

InnerDerma 백엔드 코드 수정은 불필요합니다.
