package com.innerderma.user.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void returnsUserByUserCode() {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-0000-0000");
        given(userRepository.findByUserCode("WHS-DEMO-001")).willReturn(Optional.of(user));

        User result = userService.getByUserCode("WHS-DEMO-001");

        assertThat(result.getName()).isEqualTo("테스트 사용자");
    }

    @Test
    void throwsBusinessExceptionWhenUserDoesNotExist() {
        given(userRepository.findByUserCode("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUserCode("UNKNOWN"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).errorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
