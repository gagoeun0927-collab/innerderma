package com.innerderma.user.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getByUserCode(String userCode) {
        return userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public User register(String userCode, String name, String phoneNumber) {
        if (userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        return userRepository.save(new User(userCode, name, phoneNumber));
    }

    @Transactional
    public User updateProfile(String userCode, String name, String phoneNumber) {
        User user = getByUserCode(userCode);
        user.updateProfile(name, phoneNumber);
        return user;
    }

    @Transactional
    public User updatePreferredLocale(String userCode, String locale) {
        User user = getByUserCode(userCode);
        user.updatePreferredLocale(locale);
        return user;
    }
}
