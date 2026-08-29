package com.example.usercrudapi.controller;

import com.example.usercrudapi.entity.Role;
import com.example.usercrudapi.entity.User;
import com.example.usercrudapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ── Profile ─────────────────────────────────────────────

    // GET /api/profile — Get the currently authenticated user's profile
    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getUserById(currentUser.getId()));
    }

    // ── User CRUD ───────────────────────────────────────────

    // GET /api/users — Retrieve all users (ADMIN, OWNER only — enforced by SecurityConfig)
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // GET /api/users/{id} — Retrieve a user by ID
    // ADMIN & OWNER can view anyone; USER can only view self
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        if (!currentUser.getRole().equals(Role.ADMIN)
                && !currentUser.getRole().equals(Role.OWNER)
                && !currentUser.getId().equals(id)) {
            throw new AccessDeniedException("You can only view your own profile");
        }

        return ResponseEntity.ok(userService.getUserById(id));
    }

    // POST /api/users — Create a new user (ADMIN only — enforced by SecurityConfig)
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/users/{id} — Update an existing user
    // Authorization logic is handled in UserService
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable UUID id,
                                           @Valid @RequestBody User user,
                                           Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.updateUser(id, user, currentUser));
    }

    // DELETE /api/users/{id} — Delete a user (ADMIN only — enforced by SecurityConfig)
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
