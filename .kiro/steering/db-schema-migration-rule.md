# DB 스키마 변경 규칙 (엔티티 ↔ Flyway 일치)

## 배경 (실제 발생한 사고)

`Product` 엔티티에 KB 컬럼 8개를 추가하고 `ProductCategory` enum을 13개로 확장했으나
Flyway 마이그레이션을 만들지 않았다. 로컬은 `ddl-auto=update`라서 Hibernate가 컬럼을
자동 생성해 문제가 드러나지 않았고, 프로덕션은 `ddl-auto=validate` + Flyway라서
컬럼이 없는 상태였다. 그 결과:

1. `UPDATE products SET image_url = ...` 마이그레이션이 없는 컬럼을 참조해 실패
2. Flyway 이력에 `success = 0`으로 남아 이후 모든 재기동이 validate 단계에서 차단
3. 프로덕션 502 (앱 기동 불가)

## 필수 규칙

### 1. @Entity 변경 시 Flyway 마이그레이션을 같은 커밋에 포함한다

다음 변경은 **전부** 마이그레이션이 필요하다:

- 컬럼 추가/삭제/타입 변경
- `@Enumerated(EnumType.STRING)` enum에 값 추가 (MySQL은 `ENUM` 컬럼이므로 `MODIFY COLUMN` 필요)
- 테이블 추가 (`@Entity` 신규 생성)
- 유니크 제약/인덱스 변경
- nullable 변경

마이그레이션 없이 엔티티만 바꾸면 프로덕션 기동이 깨진다. 예외 없다.

### 2. 데이터 변경(UPDATE) 마이그레이션은 스키마 변경 뒤에 둔다

같은 파일 안에서도 `ALTER TABLE`(컬럼 추가) → `UPDATE`(데이터 갱신) 순서를 지킨다.
존재하지 않는 컬럼을 참조하는 UPDATE는 마이그레이션 전체를 실패시킨다.

### 3. `@Lob String`에는 `length = Integer.MAX_VALUE`를 명시한다

Hibernate는 길이 기본값(255) 때문에 `@Lob String`에 대해 MySQL `tinytext`를 기대한다.
마이그레이션이 `LONGTEXT`면 validate가 실패한다. 반드시:

```java
@Lob
@Column(name = "xxx_json", length = Integer.MAX_VALUE)
private String xxxJson;
```

마이그레이션은 `LONGTEXT`로 선언한다.

### 4. 마이그레이션 파일 규칙

- 위치: `src/main/resources/db/migration`
- 이름: `V{n}__{snake_case_description}.sql` (n은 기존 최대값 + 1)
- **이미 프로덕션에 적용된(success=1) 마이그레이션 파일은 절대 수정하지 않는다.**
  Flyway checksum 검증이 깨진다. 수정이 필요하면 새 버전을 추가한다.
- 실패한(success=0) 마이그레이션은 아직 적용되지 않았으므로 해당 파일 수정이 가능하다.
- MySQL 문법으로 작성한다 (`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`).

## 검증 방법

`SchemaMigrationValidationTest`가 실제 MySQL 컨테이너에서
Flyway 마이그레이션 → `ddl-auto=validate` → 시딩까지 프로덕션과 동일 조건으로 검증한다.

```
./gradlew test --tests "com.innerderma.common.config.SchemaMigrationValidationTest"
```

- **이 테스트가 깨지면 = 마이그레이션 누락**이다. 배포하지 말고 마이그레이션을 추가한다.
- Docker가 필요하다. Windows에서 Docker Desktop 사용 시:
  `$env:DOCKER_HOST="npipe:////./pipe/dockerDesktopLinuxEngine"`
- 이 테스트는 CI(GitHub Actions)에서 `./gradlew build` 시 자동 실행되므로
  마이그레이션 누락 상태로는 배포가 진행되지 않는다.

## 프로덕션 Flyway 사고 대응 절차

마이그레이션 실패로 앱이 기동하지 않을 때:

1. 원인 확인
   ```bash
   docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml logs --tail=50 app-api
   ```
2. Flyway 이력 확인 (root 계정 사용)
   ```bash
   docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml exec mysql \
     mysql -u root -p -e "SELECT installed_rank, version, description, success FROM innerderma.flyway_schema_history ORDER BY installed_rank;"
   ```
3. 실패 기록 삭제 (repair) — **부분 적용된 DDL이 있으면 먼저 수동 정리**
   ```bash
   docker compose --env-file /opt/innerderma.env -f docker-compose.prod.yml exec mysql \
     mysql -u root -p -e "DELETE FROM innerderma.flyway_schema_history WHERE version = '{N}' AND success = 0;"
   ```
4. 마이그레이션 SQL을 수정해 커밋/푸시 → CI 재배포

## 로컬 환경

로컬은 `ddl-auto=update`로 두어 개발 편의를 유지하되, **커밋 전 반드시
`SchemaMigrationValidationTest`를 실행**해 프로덕션 조건을 검증한다.
로컬에서 통과했다는 것만으로 프로덕션 안전을 보장하지 않는다.
