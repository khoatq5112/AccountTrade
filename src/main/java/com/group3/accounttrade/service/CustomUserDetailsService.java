package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username/email: {}", username);

        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "User not found with email or username: " + username)));

        if (user.getIsActive() != null && !user.getIsActive()) {
            logger.warn("User is inactive: {}", username);
            throw new UsernameNotFoundException("User is essentially inactive");
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                "ROLE_" + user.getRole().getRoleName().toUpperCase());

        logger.info("User loaded successfully: {}, role: {}", user.getUsername(), user.getRole().getRoleName());

        // Use the full constructor with all required fields for proper remember-me support
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                true,  // enabled
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                true,  // accountNonLocked
                Collections.singletonList(authority));
    }
}
