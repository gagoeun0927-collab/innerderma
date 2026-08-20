package com.innerderma.product.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_concern", nullable = false, length = 30)
    private ProductConcern targetConcern;

    @Column(name = "safety_attention_compatible", nullable = false)
    private boolean safetyAttentionCompatible;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "demo_product", nullable = false)
    private boolean demoProduct;

    @Column(name = "official_url", length = 500)
    private String officialUrl;

    @Column(name = "display_priority", nullable = false)
    private int displayPriority;

    // === 새 필드 (KB 통합) ===

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "price")
    private Integer price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "usage_instruction", length = 500)
    private String usage;

    @Column(name = "application_method", length = 500)
    private String applicationMethod;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONG32VARCHAR)
    @Column(name = "verified_claims_json", length = Integer.MAX_VALUE)
    private String verifiedClaimsJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONG32VARCHAR)
    @Column(name = "ingredients_highlight_json", length = Integer.MAX_VALUE)
    private String ingredientsHighlightJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONG32VARCHAR)
    @Column(name = "skin_state_tags_json", length = Integer.MAX_VALUE)
    private String skinStateTagsJson;

    protected Product() {}

    public Product(String productCode, String brand, String name, ProductCategory category,
                   ProductConcern targetConcern, boolean safetyAttentionCompatible,
                   boolean active, boolean demoProduct, String officialUrl, int displayPriority) {
        this.productCode = productCode;
        this.brand = brand;
        this.name = name;
        this.category = category;
        this.targetConcern = targetConcern;
        this.safetyAttentionCompatible = safetyAttentionCompatible;
        this.active = active;
        this.demoProduct = demoProduct;
        this.officialUrl = officialUrl;
        this.displayPriority = displayPriority;
    }

    /** KB 제품 시딩용 풀 생성자 */
    public Product(String productCode, String brand, String name, ProductCategory category,
                   ProductConcern targetConcern, boolean safetyAttentionCompatible,
                   boolean active, boolean demoProduct, String officialUrl, int displayPriority,
                   String source, Integer price, String imageUrl, String usage,
                   String applicationMethod, String verifiedClaimsJson,
                   String ingredientsHighlightJson, String skinStateTagsJson) {
        this(productCode, brand, name, category, targetConcern, safetyAttentionCompatible,
                active, demoProduct, officialUrl, displayPriority);
        this.source = source;
        this.price = price;
        this.imageUrl = imageUrl;
        this.usage = usage;
        this.applicationMethod = applicationMethod;
        this.verifiedClaimsJson = verifiedClaimsJson;
        this.ingredientsHighlightJson = ingredientsHighlightJson;
        this.skinStateTagsJson = skinStateTagsJson;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getBrand() { return brand; }
    public String getName() { return name; }
    public ProductCategory getCategory() { return category; }
    public ProductConcern getTargetConcern() { return targetConcern; }
    public boolean isSafetyAttentionCompatible() { return safetyAttentionCompatible; }
    public boolean isActive() { return active; }
    public boolean isDemoProduct() { return demoProduct; }
    public String getOfficialUrl() { return officialUrl; }
    public int getDisplayPriority() { return displayPriority; }
    public String getSource() { return source; }
    public Integer getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public String getUsage() { return usage; }
    public String getApplicationMethod() { return applicationMethod; }
    public String getVerifiedClaimsJson() { return verifiedClaimsJson; }
    public String getIngredientsHighlightJson() { return ingredientsHighlightJson; }
    public String getSkinStateTagsJson() { return skinStateTagsJson; }
}
