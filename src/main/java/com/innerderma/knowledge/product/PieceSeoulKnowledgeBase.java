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
 * JSON 파일 기반 Piece Seoul Product Knowledge Base.
 * concern, treatment compatibility, usage time, tags 기반 필터링을 제공한다.
 */
@Component
public class PieceSeoulKnowledgeBase {

    private static final String DATA_PATH = "knowledge/piece_seoul_products.json";

    private final ObjectMapper objectMapper;
    private List<PieceSeoulProduct> products = Collections.emptyList();

    public PieceSeoulKnowledgeBase(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try {
            InputStream is = new ClassPathResource(DATA_PATH).getInputStream();
            List<PieceSeoulProductJson> items = objectMapper.readValue(is,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PieceSeoulProductJson.class));
            this.products = items.stream()
                    .map(PieceSeoulProductJson::toProduct)
                    .filter(PieceSeoulProduct::isActive)
                    .collect(Collectors.toUnmodifiableList());
        } catch (IOException exception) {
            this.products = Collections.emptyList();
        }
    }

    public List<PieceSeoulProduct> findAll() {
        return products;
    }

    public List<PieceSeoulProduct> findByConcern(String taxonomyConcern) {
        return products.stream()
                .filter(p -> p.matchesConcern(taxonomyConcern))
                .toList();
    }

    public List<PieceSeoulProduct> findCompatibleWith(String treatmentCode) {
        return products.stream()
                .filter(p -> p.isCompatibleWith(treatmentCode))
                .toList();
    }

    public List<PieceSeoulProduct> findForTime(String time) {
        return products.stream()
                .filter(p -> p.isForTime(time))
                .toList();
    }

    public List<PieceSeoulProduct> filter(String concern, String treatmentCode,
                                          List<String> restrictedTags, String time) {
        return products.stream()
                .filter(p -> concern == null || p.matchesConcern(concern))
                .filter(p -> treatmentCode == null || p.isCompatibleWith(treatmentCode))
                .filter(p -> restrictedTags == null || !p.hasRestrictedTag(restrictedTags))
                .filter(p -> time == null || p.isForTime(time))
                .toList();
    }

    public int size() {
        return products.size();
    }

    private static class PieceSeoulProductJson {
        @JsonProperty("product_id") String productId;
        String brand;
        String name;
        String category;
        List<String> tags;
        @JsonProperty("skin_state_tags") List<String> skinStateTags;
        @JsonProperty("treatment_compatibility") List<String> treatmentCompatibility;
        @JsonProperty("restricted_after_treatments") List<RestrictedTreatmentJson> restrictedAfterTreatments;
        @JsonProperty("usage_time") List<String> usageTime;
        String frequency;
        String amount;
        @JsonProperty("application_method") String applicationMethod;
        List<String> warnings;
        @JsonProperty("verified_claims") List<String> verifiedClaims;
        @JsonProperty("ingredients_highlight") List<String> ingredientsHighlight;
        List<String> allergens;
        @JsonProperty("is_active") boolean isActive;
        Integer price;
        @JsonProperty("official_url") String officialUrl;
        @JsonProperty("image_url") String imageUrl;

        PieceSeoulProduct toProduct() {
            List<PieceSeoulProduct.RestrictedTreatment> restricted = restrictedAfterTreatments == null
                    ? List.of()
                    : restrictedAfterTreatments.stream()
                        .map(r -> new PieceSeoulProduct.RestrictedTreatment(r.treatment, r.restrictDays))
                        .toList();
            return new PieceSeoulProduct(productId, brand, name, category,
                    tags != null ? tags : List.of(),
                    skinStateTags != null ? skinStateTags : List.of(),
                    treatmentCompatibility != null ? treatmentCompatibility : List.of(),
                    restricted, usageTime != null ? usageTime : List.of(),
                    frequency, amount, applicationMethod,
                    warnings != null ? warnings : List.of(),
                    verifiedClaims != null ? verifiedClaims : List.of(),
                    ingredientsHighlight != null ? ingredientsHighlight : List.of(),
                    allergens != null ? allergens : List.of(),
                    isActive, price, officialUrl, imageUrl);
        }
    }

    private static class RestrictedTreatmentJson {
        String treatment;
        @JsonProperty("restrict_days") int restrictDays;
    }
}
