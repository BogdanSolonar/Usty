package com.planning.backend.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/users -> обоє з моєї пари (імена, кольори, аватарки)
    @GetMapping
    public List<UserResponse> couple() {
        return userService.couple();
    }

    // POST /api/users/me/avatar (multipart, поле "file") -> нова аватарка поточного користувача
    @PostMapping("/me/avatar")
    public UserResponse updateAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        return userService.updateAvatar(file);
    }
}
