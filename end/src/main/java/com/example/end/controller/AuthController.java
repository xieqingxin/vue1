package com.example.end.controller;

import com.example.end.common.Result;
import com.example.end.config.AuthInterceptor;
import com.example.end.dto.UserDTO;
import com.example.end.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/** 注册 / 登录 / 用户信息 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result register(@Validated @RequestBody UserDTO dto) {
        Long id = userService.register(dto);
        return Result.ok("注册成功", Result.map("id", id));
    }

    @PostMapping("/login")
    public Result login(@Validated @RequestBody UserDTO dto) {
        return Result.ok("登录成功", userService.login(dto));
    }

    @GetMapping("/me")
    public Result me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(userService.profile(userId));
    }
}
