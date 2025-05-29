package com.expert.project.noty.service.auth;

import com.expert.project.noty.dto.auth.*;
import com.expert.project.noty.entity.UserEntity;
import com.expert.project.noty.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean register(RegisterRequest request) {
        if (userRepository.findByUserId(request.getUserId()).isPresent()) {
            return false; // 이미 존재하는 userId
        }

        System.out.println(request.getPassword());

        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(request.getUserId());
        userEntity.setPassword(passwordEncoder.encode(request.getPassword()));
        userEntity.setNickname(request.getNickname());
        userEntity.setEmail(request.getEmail());
        userEntity.setRole("ROLE_ADMIN");

        userRepository.save(userEntity);
        return true;
    }

    public String login(LoginRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByUserId(request.getUserId());

        if (userOpt.isEmpty()) {
            return "아이디가 존재하지 않습니다.";
        }

        UserEntity userEntity = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            return "비밀번호가 틀렸습니다.";
        }

        return "로그인 성공";
    }

    public String modify(ModifyRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByUserId(request.getUserId());

        if (userOpt.isEmpty()) {
            return "이메일이 존재하지 않습니다.";
        }

        UserEntity userEntity = userOpt.get();

        userEntity.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(userEntity);

        return "비밀번호 변경 성공";
    }

    public UserInfoResponse getUserById(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾지 못함"));
        return new UserInfoResponse(userEntity.getUserId(), userEntity.getNickname(), userEntity.getEmail());
    }

    public boolean updateUserInfo(UpdateUserRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<UserEntity> optionalUser = userRepository.findByUserId(userId);

        if (optionalUser.isPresent()) {
            UserEntity userEntity = optionalUser.get();
            userEntity.setUserId(request.getUserId());
            userEntity.setNickname(request.getNickname());
            userEntity.setEmail(request.getEmail());
            userRepository.save(userEntity);
            return true;
        }

        return false;
    }
}
