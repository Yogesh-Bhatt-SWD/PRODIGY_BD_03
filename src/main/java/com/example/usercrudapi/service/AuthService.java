package com.example.usercrudapi.service;

import com.example.usercrudapi.dto.AuthRequest;
import com.example.usercrudapi.dto.AuthResponse;
import com.example.usercrudapi.dto.RegisterRequest;
import com.example.usercrudapi.entity.Role;
import com.example.usercrudapi.entity.User;
import com.example.usercrudapi.exception.DuplicateResourceException;
import com.example.usercrudapi.repository.UserRepository;
import com.example.usercrudapi.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user with role USER.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A user with email '" + request.getEmail() + "' already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAge(request.getAge());
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(token, savedUser.getId(), savedUser.getName(),
                savedUser.getEmail(), savedUser.getRole());
    }

    /**
     * Authenticate a user and return a JWT token.
     */
    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user.getId(), user.getName(),
                user.getEmail(), user.getRole());
    }
}
