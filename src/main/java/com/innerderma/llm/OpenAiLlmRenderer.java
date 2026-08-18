package com.innerderma.llm;

import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.knowledge.product.PieceSeoulProduct;
import com.innerderma.knowledge.product.ProductMatchResult;
import com.innerderma.knowledge.product.WimStoreProduct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions API를 사용하는 LLM 렌더러.
 *
 * <p>Solution Object를 구조화된 JSON으로 system/user 프롬프트에 넣고,
 * LLM이 지정된 locale의 자연어로 응답하도록 한다.
 * LLM은 Rule Engine 결정을 변경하거나 새 제품을 추가할 수 없다(§37).
 */
@Component
public class OpenAiLlmRenderer implements LlmRenderer {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiLlmRenderer(
            @Value("${openai.api-key:}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    // 테스트용 생성자
    OpenAiLlmRenderer(String apiKey, ObjectMapper objectMapper, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public LlmResponse render(SolutionObject solution, ProductMatchResult products, String locale) {
        if (apiKey == null || apiKey.isBlank()) {
            // API key 미설정 시 fallback: 구조만 채운 기본 응답
            return buildFallbackResponse(solution, products, locale);
        }

        String systemPrompt = buildSystemPrompt(locale);
        String userPrompt = buildUserPrompt(solution, products);

        try {
            String responseBody = callOpenAi(systemPrompt, userPrompt);
            return parseResponse(responseBody);
        } catch (Exception exception) {
            // LLM 호출 실패 시 fallback
            return buildFallbackResponse(solution, products, locale);
        }
    }

    private String buildSystemPrompt(String locale) {
        return """
                You are the user-facing explanation layer of InnerDerma skincare service.
                
                Rules:
                - You do not independently diagnose medical conditions.
                - You do not invent products, treatments, usage instructions, ingredients, benefits, or restrictions.
                - You may only use the structured solution and product information provided.
                - You must preserve the safety status, selected products, routine order, and usage instructions.
                - You must not add products that are not present in the input.
                - You must not override Rule Engine decisions.
                - You must explain the recommendation clearly and concisely.
                - You must distinguish Night Care from Morning Care.
                - If safety status is CAUTION, prioritize the caution message over product promotion.
                - Return ONLY valid JSON matching the required schema.
                - Respond in language: %s
                
                Output JSON schema:
                {
                  "headline": "string",
                  "skinStateSummary": "string",
                  "todayGoal": "string",
                  "night": { "purpose": "string", "steps": [{"step": int, "productId": "string", "productName": "string", "usage": "string", "reason": "string"}] },
                  "morning": { "purpose": "string", "steps": [{"step": int, "productId": "string", "productName": "string", "usage": "string", "reason": "string"}] },
                  "innerCare": { "recommended": [{"productId": "string", "productName": "string", "usage": "string", "reason": "string"}], "avoid": [] },
                  "caution": "string or null"
                }
                """.formatted(locale);
    }

    private String buildUserPrompt(SolutionObject solution, ProductMatchResult products) {
        StringBuilder sb = new StringBuilder();
        sb.append("Solution Object:\n");
        sb.append("  actions: ").append(solution.actions()).append("\n");
        sb.append("  restrictions: ").append(solution.restrictions()).append("\n");
        sb.append("  appliedRules: ").append(solution.appliedRules()).append("\n");
        sb.append("  primaryConcern: ").append(products.primaryConcern()).append("\n\n");

        sb.append("Night Products:\n");
        for (int i = 0; i < products.nightProducts().size(); i++) {
            PieceSeoulProduct p = products.nightProducts().get(i);
            sb.append("  ").append(i + 1).append(". ").append(p.productId())
                    .append(" | ").append(p.name())
                    .append(" | usage: ").append(p.applicationMethod())
                    .append(" | claims: ").append(p.verifiedClaims()).append("\n");
        }

        sb.append("\nMorning Products:\n");
        for (int i = 0; i < products.morningProducts().size(); i++) {
            PieceSeoulProduct p = products.morningProducts().get(i);
            sb.append("  ").append(i + 1).append(". ").append(p.productId())
                    .append(" | ").append(p.name())
                    .append(" | usage: ").append(p.applicationMethod())
                    .append(" | claims: ").append(p.verifiedClaims()).append("\n");
        }

        sb.append("\nInner Care Products:\n");
        for (int i = 0; i < products.innerCareProducts().size(); i++) {
            WimStoreProduct p = products.innerCareProducts().get(i);
            sb.append("  ").append(i + 1).append(". ").append(p.productId())
                    .append(" | ").append(p.name())
                    .append(" | usage: ").append(p.usage())
                    .append(" | claims: ").append(p.verifiedClaims()).append("\n");
        }

        return sb.toString();
    }

    private String callOpenAi(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
        );

        String json = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API returned " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(String openAiResponseBody) {
        try {
            Map<String, Object> root = objectMapper.readValue(openAiResponseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            return objectMapper.readValue(content, LlmResponse.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private LlmResponse buildFallbackResponse(SolutionObject solution, ProductMatchResult products, String locale) {
        // API key 미설정 또는 호출 실패 시 구조만 채운 최소 응답
        List<LlmResponse.Step> nightSteps = new java.util.ArrayList<>();
        for (int i = 0; i < products.nightProducts().size(); i++) {
            PieceSeoulProduct p = products.nightProducts().get(i);
            nightSteps.add(new LlmResponse.Step(i + 1, p.productId(), p.name(), p.applicationMethod(), ""));
        }
        List<LlmResponse.Step> morningSteps = new java.util.ArrayList<>();
        for (int i = 0; i < products.morningProducts().size(); i++) {
            PieceSeoulProduct p = products.morningProducts().get(i);
            morningSteps.add(new LlmResponse.Step(i + 1, p.productId(), p.name(), p.applicationMethod(), ""));
        }
        List<LlmResponse.Recommendation> innerCare = new java.util.ArrayList<>();
        for (WimStoreProduct p : products.innerCareProducts()) {
            innerCare.add(new LlmResponse.Recommendation(p.productId(), p.name(), p.usage(), ""));
        }

        String caution = solution.actions().containsKey("safety_status") ? "주의가 필요한 상태입니다." : null;

        return new LlmResponse(
                "InnerDerma Care",
                products.primaryConcern() != null ? products.primaryConcern() : "STABLE",
                "",
                new LlmResponse.NightCare("RECOVERY", nightSteps),
                new LlmResponse.MorningCare("PROTECTION", morningSteps),
                new LlmResponse.InnerCare(innerCare, List.of()),
                caution
        );
    }
}
