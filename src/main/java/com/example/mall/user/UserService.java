package com.example.mall.user;

import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已被注册");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }
        user.setRole(UserRole.USER);
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return user;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User update(Long id, User updated) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在, id=" + id));
        if (updated.getNickname() != null) {
            existing.setNickname(updated.getNickname());
        }
        if (updated.getPhone() != null) {
            existing.setPhone(updated.getPhone());
        }
        if (updated.getEmail() != null) {
            existing.setEmail(updated.getEmail());
        }
        if (updated.getRole() != null) {
            existing.setRole(updated.getRole());
        }
        return userRepository.save(existing);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
