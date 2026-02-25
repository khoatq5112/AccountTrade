package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Role;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.RoleRepository;
import com.group3.accounttrade.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(String username, String email, String password, String roleName) throws Exception {
        if (userRepository.existsByUsername(username)) {
            throw new Exception("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new Exception("Email already exists");
        }

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new Exception("Role not found: " + roleName));

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .isActive(true)
                .build();

        userRepository.save(user);
    }
}
