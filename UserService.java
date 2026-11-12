package cn.edu.sdua.shequ.service;

import cn.edu.sdua.shequ.common.ApiException;
import cn.edu.sdua.shequ.entity.User;
import cn.edu.sdua.shequ.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 用户管理服务（中介/房东/租客账号）
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User login(String username, String password) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty() || !opt.get().getPassword().equals(password)) {
            throw new ApiException("用户名或密码错误");
        }
        if (opt.get().getStatus() != null && opt.get().getStatus() == 0) {
            throw new ApiException("账号已被禁用，请联系管理员");
        }
        User user = opt.get();
        user.setPassword(null);
        return user;
    }

    public List<User> list(String role, String keyword) {
        List<User> users = (role == null || role.isBlank())
                ? userRepository.findAll() : userRepository.findByRole(role);
        users.forEach(u -> u.setPassword(null));
        if (keyword != null && !keyword.isBlank()) {
            users = users.stream()
                    .filter(u -> (u.getName() != null && u.getName().contains(keyword))
                            || (u.getUsername() != null && u.getUsername().contains(keyword)))
                    .toList();
        }
        return users;
    }

    public User create(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new ApiException("用户名已存在：" + user.getUsername());
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ApiException("密码不能为空");
        }
        if (user.getStatus() == null) user.setStatus(1);
        User saved = userRepository.save(user);
        saved.setPassword(null);
        return saved;
    }

    public User update(Long id, User req) {
        User user = userRepository.findById(id).orElseThrow(() -> new ApiException("用户不存在"));
        userRepository.findByUsername(req.getUsername()).ifPresent(exist -> {
            if (!exist.getId().equals(id)) throw new ApiException("用户名已存在：" + req.getUsername());
        });
        user.setUsername(req.getUsername());
        user.setName(req.getName());
        user.setRole(req.getRole());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        // 密码留空表示不修改
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(req.getPassword());
        }
        User saved = userRepository.save(user);
        saved.setPassword(null);
        return saved;
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
