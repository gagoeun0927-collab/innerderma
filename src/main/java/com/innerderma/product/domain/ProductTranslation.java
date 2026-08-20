package com.innerderma.product.domain;

import jakarta.persistence.*;

/**
 * 상품별 다국어 번역 데이터.
 * product_code + locale 조합으로 유니크하며, locale은 en/ja/zh 등 IETF 언어 태그.
 */
@Entity
@Table(name = "product_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_code", "locale"}))
public class ProductTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(name = "usage_text", length = 1000)
    private String usage;

    /** JSON array of feature strings (verified_claims 번역) */
    @Lob
    @Column(name = "features_json")
    private String featuresJson;

    /** 주의사항 (warnings + allergens 통합 번역) */
    @Column(length = 2000)
    private String caution;

    protected ProductTranslation() {}

    public ProductTranslation(String productCode, String locale, String name,
                              String usage, String featuresJson, String caution) {
        this.productCode = productCode;
        this.locale = locale;
        this.name = name;
        this.usage = usage;
        this.featuresJson = featuresJson;
        this.caution = caution;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getLocale() { return locale; }
    public String getName() { return name; }
    public String getUsage() { return usage; }
    public String getFeaturesJson() { return featuresJson; }
    public String getCaution() { return caution; }
}
