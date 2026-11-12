package cn.edu.sdua.shequ.controller;

import cn.edu.sdua.shequ.common.Result;
import cn.edu.sdua.shequ.entity.User;
import cn.edu.sdua.shequ.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> body) {
        return Result.ok(userService.login(body.get("username"), body.get("password")));
    }
}
