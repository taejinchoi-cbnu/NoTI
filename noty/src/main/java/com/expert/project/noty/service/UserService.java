package com.expert.project.noty.service;

import com.expert.project.noty.dto.LoginRequest;
import com.expert.project.noty.dto.RegisterRequest;
import com.expert.project.noty.entity.User;
import com.expert.project.noty.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

        User user = new User();
        user.setUserId(request.getUserId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());

        userRepository.save(user);
        return true;
    }

    public String login(String userId, String password) {
        Optional<User> userOpt = userRepository.findByUserId(userId);

        if (userOpt.isEmpty()) {
            return "이메일이 존재하지 않습니다.";
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return "비밀번호가 틀렸습니다.";
        }

        return "로그인 성공";
    }
}
