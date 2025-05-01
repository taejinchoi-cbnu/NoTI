package com.expert.project.noty.controller.auth;

import com.expert.project.noty.dto.auth.RegisterRequest;
import com.expert.project.noty.service.auth.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
public class JoinController {
    private final UserService userService;

    public JoinController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/join")
    public String joinProcess(RegisterRequest request) {

        if (userService.register(request)) {
            return "ok";
        }

        return "null";
    }
}
