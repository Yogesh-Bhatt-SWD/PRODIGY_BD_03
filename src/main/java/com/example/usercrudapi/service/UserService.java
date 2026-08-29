package com.example.usercrudapi.service;

import com.example.usercrudapi.entity.Role;
import com.example.usercrudapi.entity.User;
import com.example.usercrudapi.exception.DuplicateResourceException;
import com.example.usercrudapi.exception.ResourceNotFoundException;
import com.example.usercrudapi.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Admin-only: create a user with a specified role.
     */
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException(
                    "A user with email '" + user.getEmail() + "' already exists");
        }

        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Default to USER role if none specified
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        return userRepository.save(user);
    }

    /**
     * Update a user. Non-admin users can only update their own name, email, and age.
     * Only ADMIN can change roles.
     */
    public User updateUser(UUID id, User userDetails, User currentUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Authorization: ADMIN can update anyone; OWNER and USER can only update themselves
        if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(id)) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        // Check email uniqueness
        if (userRepository.existsByEmailAndIdNot(userDetails.getEmail(), id)) {
            throw new DuplicateResourceException(
                    "A user with email '" + userDetails.getEmail() + "' already exists");
        }

        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setAge(userDetails.getAge());

        // Only ADMIN can change roles
        if (currentUser.getRole().equals(Role.ADMIN) && userDetails.getRole() != null) {
            existingUser.setRole(userDetails.getRole());
        }

        return userRepository.save(existingUser);
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
