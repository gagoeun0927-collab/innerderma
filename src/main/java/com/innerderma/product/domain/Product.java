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
}
