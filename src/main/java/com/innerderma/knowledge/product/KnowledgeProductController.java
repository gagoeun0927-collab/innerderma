package com.innerderma.knowledge.product;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Knowledge Product", description = "KB 제품 상세 조회 (Piece Seoul / WIM Store)")
@RestController
@RequestMapping("/api/knowledge-products")
public class KnowledgeProductController {

    private final PieceSeoulKnowledgeBase pieceSeoulKb;
    private final WimStoreKnowledgeBase wimStoreKb;

    public KnowledgeProductController(PieceSeoulKnowledgeBase pieceSeoulKb, WimStoreKnowledgeBase wimStoreKb) {
        this.pieceSeoulKb = pieceSeoulKb;
        this.wimStoreKb = wimStoreKb;
    }

    @Operation(summary = "KB 제품 전체 목록", description = "source 파라미터로 PIECE_SEOUL/WIM_STORE 필터 가능")
    @GetMapping
    public ApiResponse<List<KnowledgeProductResponse>> getAll(
            @RequestParam(required = false) String source) {
        List<KnowledgeProductResponse> results = new java.util.ArrayList<>();
        if (source == null || "PIECE_SEOUL".equalsIgnoreCase(source)) {
            pieceSeoulKb.findAll().forEach(p -> results.add(KnowledgeProductResponse.fromPieceSeoul(p)));
        }
        if (source == null || "WIM_STORE".equalsIgnoreCase(source)) {
            wimStoreKb.findAll().forEach(p -> results.add(KnowledgeProductResponse.fromWimStore(p)));
        }
        return ApiResponse.success(results);
    }

    @Operation(summary = "KB 제품 상세 조회", description = "productId로 Piece Seoul 또는 WIM Store 제품 상세를 반환")
    @GetMapping("/{productId}")
    public ApiResponse<KnowledgeProductResponse> getById(@PathVariable String productId) {
        var piece = pieceSeoulKb.findAll().stream()
                .filter(p -> p.productId().equals(productId)).findFirst();
        if (piece.isPresent()) {
            return ApiResponse.success(KnowledgeProductResponse.fromPieceSeoul(piece.get()));
        }
        var wim = wimStoreKb.findAll().stream()
                .filter(p -> p.productId().equals(productId)).findFirst();
        if (wim.isPresent()) {
            return ApiResponse.success(KnowledgeProductResponse.fromWimStore(wim.get()));
        }
        throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
    }
}
