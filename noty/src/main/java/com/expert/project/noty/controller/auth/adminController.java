package com.expert.project.noty.controller.auth;

import com.expert.project.noty.dto.auth.RegisterRequest;
import com.expert.project.noty.service.auth.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
public class adminController {
    private final UserService userService;

    public adminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/admin")
    public String adminProcess(RegisterRequest request) {

        return "null";
    }
}
