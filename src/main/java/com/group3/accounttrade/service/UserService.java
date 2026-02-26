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
    private final OtpService otpService;
    private final EmailService emailService;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, OtpService otpService, EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    public void registerUser(String username, String email, String password, String roleName) throws Exception {
        // 1. Normalize Username: trim leading/trailing spaces and collapse/remove
        // internal multi-spaces.
        username = username.trim().replaceAll("\\s+", " ");

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
                .isActive(false) // Deactivated until OTP verification
                .build();

        userRepository.save(user);

        // Generation and Dispatch of OTP
        String otp = otpService.generateAndStoreOtp(email);
        emailService.sendOtpEmail(email, otp);
    }

    public boolean activateUserWithOtp(String email, String otp) {
        if (otpService.verifyOtp(email, otp)) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                user.setIsActive(true);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }
}
