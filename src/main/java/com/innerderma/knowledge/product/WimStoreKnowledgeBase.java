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
            List<WimStoreProductJson> items = objectMapper.readValue(is,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WimStoreProductJson.class));
            this.products = items.stream()
                    .map(WimStoreProductJson::toProduct)
                    .filter(WimStoreProduct::isActive)
                    .collect(Collectors.toUnmodifiableList());
        } catch (IOException exception) {
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

    private static class WimStoreProductJson {
        @JsonProperty("product_id") String productId;
        String brand;
        String name;
        String category;
        @JsonProperty("state_tags") List<String> stateTags;
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

        WimStoreProduct toProduct() {
            return new WimStoreProduct(productId, brand, name, category,
                    stateTags != null ? stateTags : List.of(),
                    dietaryTags != null ? dietaryTags : List.of(),
                    allergens != null ? allergens : List.of(),
                    restrictions != null ? restrictions : List.of(),
                    usage,
                    verifiedClaims != null ? verifiedClaims : List.of(),
                    ingredientsHighlight != null ? ingredientsHighlight : List.of(),
                    warnings != null ? warnings : List.of(),
                    isActive, price, officialUrl, imageUrl, recommendFrequencyDays);
        }
    }
}
