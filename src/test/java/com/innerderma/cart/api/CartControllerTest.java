package com.innerderma.cart.api;

import com.innerderma.cart.domain.CartItem;
import com.innerderma.cart.domain.CartRepository;
import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.user.application.UserService;
import com.innerderma.user.domain.User;
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

class CartControllerTest {

    private CartRepository cartRepository;
    private UserService userService;
    private MockMvc mockMvc;

    private static final String USER_CODE = "WHS-DEMO-001";
    private static final User USER = new User(USER_CODE, "테스트", "010-1234-1234");

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        userService = mock(UserService.class);
        when(userService.getByUserCode(USER_CODE)).thenReturn(USER);
        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartRepository, userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsCartItems() throws Exception {
        CartItem item = new CartItem(USER, "PSS_001", "PIECE_SEOUL", 2);
        when(cartRepository.findByUser_UserCodeOrderByAddedAtDesc(USER_CODE))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/users/{userCode}/cart", USER_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productId").value("PSS_001"))
                .andExpect(jsonPath("$.data[0].quantity").value(2));
    }

    @Test
    void addsNewItemToCart() throws Exception {
        when(cartRepository.findByUser_UserCodeAndProductId(USER_CODE, "WIM_003"))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/users/{userCode}/cart", USER_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"WIM_003\",\"productSource\":\"WIM_STORE\",\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value("WIM_003"))
                .andExpect(jsonPath("$.data.quantity").value(1));
    }

    @Test
    void addsQuantityWhenProductAlreadyInCart() throws Exception {
        CartItem existing = new CartItem(USER, "PSS_001", "PIECE_SEOUL", 2);
        when(cartRepository.findByUser_UserCodeAndProductId(USER_CODE, "PSS_001"))
                .thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/users/{userCode}/cart", USER_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"PSS_001\",\"productSource\":\"PIECE_SEOUL\",\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value("PSS_001"))
                .andExpect(jsonPath("$.data.quantity").value(5));

        verify(cartRepository, never()).save(any());
    }

    @Test
    void updatesQuantity() throws Exception {
        CartItem item = new CartItem(USER, "PSS_001", "PIECE_SEOUL", 2);
        when(cartRepository.findByUser_UserCodeAndProductId(USER_CODE, "PSS_001"))
                .thenReturn(Optional.of(item));

        mockMvc.perform(patch("/api/users/{userCode}/cart/{productId}", USER_CODE, "PSS_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(5));
    }

    @Test
    void removesItemFromCart() throws Exception {
        mockMvc.perform(delete("/api/users/{userCode}/cart/{productId}", USER_CODE, "PSS_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cartRepository).deleteByUser_UserCodeAndProductId(USER_CODE, "PSS_001");
    }

    @Test
    void clearsEntireCart() throws Exception {
        mockMvc.perform(delete("/api/users/{userCode}/cart", USER_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cartRepository).deleteByUser_UserCode(USER_CODE);
    }
}
