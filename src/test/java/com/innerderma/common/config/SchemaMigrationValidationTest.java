package com.innerderma.common.config;

import com.innerderma.product.domain.ProductRepository;
import com.innerderma.product.domain.ProductTranslationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로덕션과 동일한 조건(실제 MySQL + Flyway 마이그레이션 + ddl-auto=validate)으로
 * 애플리케이션 컨텍스트를 기동해 스키마 드리프트를 잡는다.
 *
 * <p>이 테스트가 존재하는 이유: @Entity에 컬럼을 추가하거나 enum 값을 확장했는데
 * 대응하는 Flyway 마이그레이션을 만들지 않으면, 로컬(ddl-auto=update)에서는 Hibernate가
 * 자동으로 컬럼을 만들어주기 때문에 문제가 드러나지 않고 프로덕션 배포 시점에
 * "Schema-validation: missing column" 또는 마이그레이션 실패로 앱이 기동하지 않는다.
 *
 * <p>즉 이 테스트가 깨지면 = 마이그레이션이 누락됐다는 뜻이다.
 * src/main/resources/db/migration 에 새 V{n}__*.sql 을 추가해야 한다.
 *
 * <p>Docker가 필요하다. Docker를 쓸 수 없는 환경에서는 이 테스트가 실패하므로
 * CI(GitHub Actions)와 Docker가 있는 로컬에서 실행된다.
 */
@Testcontainers
@SpringBootTest
class SchemaMigrationValidationTest {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("innerderma")
            .withUsername("innerderma_app")
            .withPassword("test-password");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // 프로덕션과 동일: Flyway로 스키마를 만들고, Hibernate는 검증만 한다.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductTranslationRepository translationRepository;

    /**
     * 컨텍스트가 뜨는 것 자체가 검증이다.
     * Flyway 마이그레이션이 모두 성공하고, 모든 @Entity가 실제 스키마와 일치해야 통과한다.
     */
    @Test
    void migrationsProduceSchemaThatMatchesEntities() {
        assertThat(productRepository.count()).isPositive();
    }

    /**
     * 시딩이 실제 MySQL 스키마에서 동작하는지 검증한다.
     * category enum 확장이 마이그레이션에 반영되지 않으면 여기서 실패한다.
     */
    @Test
    void kbSeedingWorksOnMysqlSchema() {
        var kbProducts = productRepository.findAll().stream()
                .filter(p -> p.getSource() != null)
                .toList();

        assertThat(kbProducts).hasSize(16);
        assertThat(kbProducts).allSatisfy(p -> {
            assertThat(p.getPrice()).isNotNull();
            assertThat(p.getImageUrl()).startsWith("/product-images/");
        });

        // 섭취류 카테고리(POWDER 등)가 enum에 없으면 시딩 시 데이터 잘림/에러가 발생한다.
        assertThat(kbProducts).anySatisfy(p ->
                assertThat(p.getCategory().name()).isEqualTo("POWDER"));
    }

    @Test
    void translationSeedingWorksOnMysqlSchema() {
        assertThat(translationRepository.count()).isEqualTo(64);
    }
}
