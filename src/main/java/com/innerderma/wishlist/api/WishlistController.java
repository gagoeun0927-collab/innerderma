package com.innerderma.wishlist.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.user.application.UserService;
import com.innerderma.user.domain.User;
import com.innerderma.wishlist.domain.WishlistItem;
import com.innerderma.wishlist.domain.WishlistRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Wishlist", description = "제품 찜(위시리스트) 관리")
@RestController
@RequestMapping("/api/users/{userCode}/wishlist")
public class WishlistController {

    private final WishlistRepository wishlistRepository;
    private final UserService userService;

    public WishlistController(WishlistRepository wishlistRepository, UserService userService) {
        this.wishlistRepository = wishlistRepository;
        this.userService = userService;
    }

    @Operation(summary = "찜 목록 조회")
    @GetMapping
    public ApiResponse<List<WishlistResponse>> getWishlist(@PathVariable String userCode) {
        userService.getByUserCode(userCode);
        return ApiResponse.success(wishlistRepository.findByUser_UserCodeOrderByAddedAtDesc(userCode)
                .stream().map(WishlistResponse::from).toList());
    }

    @Operation(summary = "찜 추가", description = "제품을 위시리스트에 추가합니다. 이미 있으면 무시.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<WishlistResponse> addToWishlist(
            @PathVariable String userCode,
            @Valid @RequestBody WishlistRequest request) {
        User user = userService.getByUserCode(userCode);
        if (wishlistRepository.existsByUser_UserCodeAndProductId(userCode, request.productId())) {
            WishlistItem existing = wishlistRepository.findByUser_UserCodeAndProductId(userCode, request.productId()).get();
            return ApiResponse.success(WishlistResponse.from(existing));
        }
        WishlistItem item = wishlistRepository.save(new WishlistItem(user, request.productId(), request.productSource()));
        return ApiResponse.success(WishlistResponse.from(item));
    }

    @Operation(summary = "찜 삭제")
    @DeleteMapping("/{productId}")
    @Transactional
    public ApiResponse<Void> removeFromWishlist(
            @PathVariable String userCode,
            @PathVariable String productId) {
        userService.getByUserCode(userCode);
        wishlistRepository.deleteByUser_UserCodeAndProductId(userCode, productId);
        return ApiResponse.success(null);
    }

    public record WishlistRequest(@NotBlank String productId, @NotBlank String productSource) {}

    public record WishlistResponse(Long id, String productId, String productSource, String addedAt) {
        public static WishlistResponse from(WishlistItem item) {
            return new WishlistResponse(item.getId(), item.getProductId(),
                    item.getProductSource(), item.getAddedAt().toString());
        }
    }
}
