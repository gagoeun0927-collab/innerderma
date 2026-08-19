package com.innerderma.wishlist.api;

import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.user.application.UserService;
import com.innerderma.user.domain.User;
import com.innerderma.wishlist.domain.WishlistItem;
import com.innerderma.wishlist.domain.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WishlistControllerTest {

    private WishlistRepository wishlistRepository;
    private UserService userService;
    private MockMvc mockMvc;

    private static final String USER_CODE = "WHS-DEMO-001";
    private static final User USER = new User(USER_CODE, "테스트", "010-1234-1234");

    @BeforeEach
    void setUp() {
        wishlistRepository = mock(WishlistRepository.class);
        userService = mock(UserService.class);
        when(userService.getByUserCode(USER_CODE)).thenReturn(USER);
        mockMvc = MockMvcBuilders.standaloneSetup(new WishlistController(wishlistRepository, userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsWishlistItems() throws Exception {
        WishlistItem item = new WishlistItem(USER, "PSS_001", "PIECE_SEOUL");
        when(wishlistRepository.findByUser_UserCodeOrderByAddedAtDesc(USER_CODE))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/users/{userCode}/wishlist", USER_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productId").value("PSS_001"))
                .andExpect(jsonPath("$.data[0].productSource").value("PIECE_SEOUL"));
    }

    @Test
    void addsNewItemToWishlist() throws Exception {
        when(wishlistRepository.existsByUser_UserCodeAndProductId(USER_CODE, "WIM_003")).thenReturn(false);
        when(wishlistRepository.save(any(WishlistItem.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/users/{userCode}/wishlist", USER_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"WIM_003\",\"productSource\":\"WIM_STORE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value("WIM_003"))
                .andExpect(jsonPath("$.data.productSource").value("WIM_STORE"));
    }

    @Test
    void returnsExistingItemWhenDuplicate() throws Exception {
        WishlistItem existing = new WishlistItem(USER, "PSS_001", "PIECE_SEOUL");
        when(wishlistRepository.existsByUser_UserCodeAndProductId(USER_CODE, "PSS_001")).thenReturn(true);
        when(wishlistRepository.findByUser_UserCodeAndProductId(USER_CODE, "PSS_001"))
                .thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/users/{userCode}/wishlist", USER_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"PSS_001\",\"productSource\":\"PIECE_SEOUL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value("PSS_001"));

        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void removesItemFromWishlist() throws Exception {
        mockMvc.perform(delete("/api/users/{userCode}/wishlist/{productId}", USER_CODE, "PSS_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(wishlistRepository).deleteByUser_UserCodeAndProductId(USER_CODE, "PSS_001");
    }
}
