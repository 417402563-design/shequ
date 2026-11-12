package cn.edu.sdua.shequ.controller;

import cn.edu.sdua.shequ.common.Result;
import cn.edu.sdua.shequ.entity.User;
import cn.edu.sdua.shequ.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理（中介/房东/租客账号）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<List<User>> list(@RequestParam(required = false) String role,
                                   @RequestParam(required = false) String keyword) {
        return Result.ok(userService.list(role, keyword));
    }

    @PostMapping
    public Result<User> create(@RequestBody User user) {
        user.setId(null);
        return Result.ok(userService.create(user));
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody User user) {
        return Result.ok(userService.update(id, user));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
