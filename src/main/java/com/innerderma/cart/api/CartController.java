package com.innerderma.cart.api;

import com.innerderma.cart.domain.CartItem;
import com.innerderma.cart.domain.CartRepository;
import com.innerderma.common.response.ApiResponse;
import com.innerderma.user.application.UserService;
import com.innerderma.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cart", description = "장바구니 관리")
@RestController
@RequestMapping("/api/users/{userCode}/cart")
public class CartController {

    private final CartRepository cartRepository;
    private final UserService userService;

    public CartController(CartRepository cartRepository, UserService userService) {
        this.cartRepository = cartRepository;
        this.userService = userService;
    }

    @Operation(summary = "장바구니 조회")
    @GetMapping
    public ApiResponse<List<CartResponse>> getCart(@PathVariable String userCode) {
        userService.getByUserCode(userCode);
        return ApiResponse.success(cartRepository.findByUser_UserCodeOrderByAddedAtDesc(userCode)
                .stream().map(CartResponse::from).toList());
    }

    @Operation(summary = "장바구니 추가", description = "제품을 장바구니에 추가합니다. 이미 있으면 수량을 합산합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<CartResponse> addToCart(
            @PathVariable String userCode,
            @Valid @RequestBody CartAddRequest request) {
        User user = userService.getByUserCode(userCode);
        var existing = cartRepository.findByUser_UserCodeAndProductId(userCode, request.productId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.updateQuantity(item.getQuantity() + request.quantity());
            return ApiResponse.success(CartResponse.from(item));
        }
        CartItem item = cartRepository.save(new CartItem(user, request.productId(), request.productSource(), request.quantity()));
        return ApiResponse.success(CartResponse.from(item));
    }

    @Operation(summary = "장바구니 수량 변경")
    @PatchMapping("/{productId}")
    @Transactional
    public ApiResponse<CartResponse> updateQuantity(
            @PathVariable String userCode,
            @PathVariable String productId,
            @Valid @RequestBody CartUpdateRequest request) {
        userService.getByUserCode(userCode);
        CartItem item = cartRepository.findByUser_UserCodeAndProductId(userCode, productId)
                .orElseThrow(() -> new com.innerderma.common.error.BusinessException(
                        com.innerderma.common.error.ErrorCode.INVALID_REQUEST));
        item.updateQuantity(request.quantity());
        return ApiResponse.success(CartResponse.from(item));
    }

    @Operation(summary = "장바구니 항목 삭제")
    @DeleteMapping("/{productId}")
    @Transactional
    public ApiResponse<Void> removeFromCart(
            @PathVariable String userCode,
            @PathVariable String productId) {
        userService.getByUserCode(userCode);
        cartRepository.deleteByUser_UserCodeAndProductId(userCode, productId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "장바구니 전체 비우기")
    @DeleteMapping
    @Transactional
    public ApiResponse<Void> clearCart(@PathVariable String userCode) {
        userService.getByUserCode(userCode);
        cartRepository.deleteByUser_UserCode(userCode);
        return ApiResponse.success(null);
    }

    public record CartAddRequest(
            @NotBlank String productId,
            @NotBlank String productSource,
            @Min(1) int quantity
    ) {}

    public record CartUpdateRequest(@Min(1) int quantity) {}

    public record CartResponse(Long id, String productId, String productSource, int quantity, String addedAt) {
        public static CartResponse from(CartItem item) {
            return new CartResponse(item.getId(), item.getProductId(),
                    item.getProductSource(), item.getQuantity(), item.getAddedAt().toString());
        }
    }
}
