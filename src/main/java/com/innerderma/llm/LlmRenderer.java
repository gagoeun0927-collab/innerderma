package com.innerderma.llm;

import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.knowledge.product.ProductMatchResult;

/**
 * LLM 렌더링 인터페이스. Solution Object + 매칭 제품 + locale을 받아 사용자 대면 자연어 응답을 생성한다.
 */
public interface LlmRenderer {

    /**
     * @param solution  Rule Engine이 결정한 Solution Object
     * @param products  Product Matcher가 선택한 제품 목록
     * @param locale    사용자 언어 (예: "ko", "en", "ja")
     * @return LLM이 생성한 사용자 대면 응답
     */
    LlmResponse render(SolutionObject solution, ProductMatchResult products, String locale);
}
