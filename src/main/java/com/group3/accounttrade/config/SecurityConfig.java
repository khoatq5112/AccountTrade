package com.group3.accounttrade.config;

import com.group3.accounttrade.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final CustomUserDetailsService customUserDetailsService;

        @Autowired
        public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
                this.customUserDetailsService = customUserDetailsService;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                        CustomAuthenticationFailureHandler customAuthenticationFailureHandler) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/index.html", "/login.html", "/register.html",
                                                                "/register", "/verify-otp", "/api/auth/**",
                                                                "/forgot-password", "/reset-password")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login.html")
                                                .loginProcessingUrl("/login")
                                                .successHandler(customAuthenticationSuccessHandler)
                                                .failureHandler(customAuthenticationFailureHandler)
                                                .permitAll())
                                .rememberMe(rememberMe -> rememberMe
                                                .userDetailsService(customUserDetailsService)
                                                .key("trustbridge-remember-me-secret-key")
                                                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days
                                                .rememberMeParameter("remember-me"))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login.html?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID", "remember-me")
                                                .permitAll());

                return http.build();
        }
}
