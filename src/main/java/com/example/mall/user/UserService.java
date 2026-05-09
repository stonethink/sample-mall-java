package com.example.mall.user;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
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

    public User updateProfile(Long userId, User updated) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在, id=" + userId));
        if (updated.getNickname() != null) {
            existing.setNickname(updated.getNickname());
        }
        if (updated.getPhone() != null) {
            existing.setPhone(updated.getPhone());
        }
        if (updated.getEmail() != null) {
            existing.setEmail(updated.getEmail());
        }
        return userRepository.save(existing);
    }

    public User changePassword(Long userId, String oldPassword, String newPassword) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在, id=" + userId));
        if (!existing.getPassword().equals(oldPassword)) {
            throw new IllegalArgumentException("旧密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }
        existing.setPassword(newPassword);
        return userRepository.save(existing);
    }

    public User resetPassword(Long userId, String newPassword) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在, id=" + userId));
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }
        existing.setPassword(newPassword);
        return userRepository.save(existing);
    }
}
