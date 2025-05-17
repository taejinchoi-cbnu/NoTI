package com.expert.project.noty.config;

import com.expert.project.noty.entity.UserEntity;
import com.expert.project.noty.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class TestDataInitializer {

    @Bean
    CommandLineRunner initTestUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            userRepository.deleteAll();

            String userId1 = "test";
            if (userRepository.findByUserId(userId1).isEmpty()) {
                UserEntity userEntity = new UserEntity();
                userEntity.setUserId(userId1);
                userEntity.setPassword(passwordEncoder.encode("1234"));
                userEntity.setNickname("test");
                userEntity.setEmail("test" + "@example.com");
                userEntity.setRole("ROLE_ADMIN");
                userRepository.save(userEntity);
            }

            String userId2 = "taejin";
            if (userRepository.findByUserId(userId2).isEmpty()) {
                UserEntity userEntity = new UserEntity();
                userEntity.setUserId(userId2);
                userEntity.setPassword(passwordEncoder.encode("1234"));
                userEntity.setNickname("taejin");
                userEntity.setEmail("test" + "@example.com");
                userEntity.setRole("ROLE_ADMIN");
                userRepository.save(userEntity);
            }

            // 100개의 테스트 데이터 입력
            for (int i = 1; i <= 100; i++) {
                String userId = "testuser" + i;
                if (userRepository.findByUserId(userId).isEmpty()) {
                    UserEntity userEntity = new UserEntity();
                    userEntity.setUserId(userId);
                    userEntity.setPassword(passwordEncoder.encode("password" + i));
                    userEntity.setNickname("TestUser" + i);
                    userEntity.setEmail("test" + i + "@example.com");
                    userEntity.setRole("ROLE_ADMIN");
                    userRepository.save(userEntity);
                }
            }
        };
    }
}
