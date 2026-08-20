package com.innerderma.knowledge.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JSON 파일 기반 WIM Store Product Knowledge Base.
 * concern, allergen, dietary restriction 기반 필터링을 제공한다.
 */
@Component
public class WimStoreKnowledgeBase {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(WimStoreKnowledgeBase.class);

    private static final String DATA_PATH = "knowledge/wim_store_products.json";

    private final ObjectMapper objectMapper;
    private List<WimStoreProduct> products = Collections.emptyList();

    public WimStoreKnowledgeBase(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try {
            InputStream is = new ClassPathResource(DATA_PATH).getInputStream();
            var mapper = objectMapper.rebuild()
                    .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();
            List<WimStoreProductJson> items = mapper.readValue(is,
                    mapper.getTypeFactory().constructCollectionType(List.class, WimStoreProductJson.class));
            this.products = items.stream()
                    .filter(item -> "wim_store".equalsIgnoreCase(item.store))
                    .map(WimStoreProductJson::toProduct)
                    .filter(WimStoreProduct::isActive)
                    .collect(Collectors.toUnmodifiableList());
            log.info("WimStore KB loaded: {} products (of {} JSON entries)", products.size(), items.size());
        } catch (Exception exception) {
            log.error("WimStore KB load failed", exception);
            this.products = Collections.emptyList();
        }
    }

    public List<WimStoreProduct> findAll() {
        return products;
    }

    public List<WimStoreProduct> findByConcern(String taxonomyConcern) {
        return products.stream()
                .filter(p -> p.matchesConcern(taxonomyConcern))
                .toList();
    }

    public List<WimStoreProduct> filter(String concern, List<String> userAllergens,
                                        List<String> userRestrictions) {
        return products.stream()
                .filter(p -> concern == null || p.matchesConcern(concern))
                .filter(p -> !p.hasAllergen(userAllergens))
                .filter(p -> !p.hasRestriction(userRestrictions))
                .toList();
    }

    public int size() {
        return products.size();
    }

    @com.fasterxml.jackson.annotation.JsonAutoDetect(
            fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
    private static class WimStoreProductJson {
        @JsonProperty("product_id") String productId;
        String brand;
        String name;
        String category;
        @JsonProperty("state_tags") List<String> stateTags;
        @JsonProperty("skin_state_tags") List<String> skinStateTags;
        @JsonProperty("dietary_tags") List<String> dietaryTags;
        List<String> allergens;
        List<String> restrictions;
        String usage;
        @JsonProperty("verified_claims") List<String> verifiedClaims;
        @JsonProperty("ingredients_highlight") List<String> ingredientsHighlight;
        List<String> warnings;
        @JsonProperty("is_active") boolean isActive;
        Integer price;
        @JsonProperty("official_url") String officialUrl;
        @JsonProperty("image_url") String imageUrl;
        @JsonProperty("recommend_frequency_days") Integer recommendFrequencyDays;
        @JsonProperty("store") String store;

        WimStoreProduct toProduct() {
            List<String> resolvedStateTags = stateTags != null && !stateTags.isEmpty()
                    ? stateTags
                    : (skinStateTags != null ? skinStateTags : List.of());
            return new WimStoreProduct(productId, brand, name, category,
                    resolvedStateTags,
                    dietaryTags != null ? dietaryTags : List.of(),
                    allergens != null ? allergens : List.of(),
                    restrictions != null ? restrictions : List.of(),
                    usage,
                    verifiedClaims != null ? verifiedClaims : List.of(),
                    ingredientsHighlight != null ? ingredientsHighlight : List.of(),
                    warnings != null ? warnings : List.of(),
                    isActive, price, officialUrl, imageUrl,
                    recommendFrequencyDays != null ? recommendFrequencyDays : 1);
        }
    }
}
