package com.example.mall.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registered = userService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(maskPassword(registered));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("USERNAME_EXISTS", e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        if (username == null || password == null) {
            return buildErrorResponse("UNAUTHORIZED", "用户名和密码不能为空");
        }
        try {
            User user = userService.login(username, password);
            session.setAttribute("currentUser", user);
            return ResponseEntity.ok(maskPassword(user));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("UNAUTHORIZED", e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return buildErrorResponse("UNAUTHORIZED", "请先登录");
        }
        return ResponseEntity.ok(maskPassword(user));
    }

    @GetMapping
    public ResponseEntity<?> listAll(HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users.stream()
                .map(this::maskPassword)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        return userService.findById(id)
                .map(u -> ResponseEntity.ok(maskPassword(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody User user, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            User updated = userService.update(id, user);
            return ResponseEntity.ok(maskPassword(updated));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("USER_NOT_FOUND", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private User maskPassword(User user) {
        User masked = new User();
        masked.setId(user.getId());
        masked.setUsername(user.getUsername());
        masked.setNickname(user.getNickname());
        masked.setPhone(user.getPhone());
        masked.setEmail(user.getEmail());
        masked.setRole(user.getRole());
        masked.setCreatedAt(user.getCreatedAt());
        return masked;
    }

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(String error, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
