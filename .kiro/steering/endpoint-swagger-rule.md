---
inclusion: always
---

# 엔드포인트 작업 시 Swagger 자동 업데이트 규칙

새로운 API 엔드포인트를 추가하거나 기존 엔드포인트를 수정할 때 반드시 다음을 수행한다:

1. Controller 클래스에 `@Tag(name = "...", description = "...")` 어노테이션이 없으면 추가한다.
2. 각 엔드포인트 메서드에 `@Operation(summary = "...", description = "...")` 어노테이션을 추가한다.
3. 필요 시 `@Parameter`, `@ApiResponse` 어노테이션으로 파라미터와 응답을 문서화한다.

이 규칙은 모든 엔드포인트 작업에 자동 적용된다.
