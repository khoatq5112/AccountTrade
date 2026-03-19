package com.group3.accounttrade.config;

import com.group3.accounttrade.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

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
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(Arrays.asList("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                        CustomAuthenticationFailureHandler customAuthenticationFailureHandler) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/index.html", "/login", "/login.html", "/register.html",
                                                                "/register", "/verify-otp", "/api/auth/**",
                                                                "/forgot-password", "/reset-password", "/index",
                                                                "/marketplace", "/marketplace/**", "/category/**", "/api/search/**",
                                                                "/payment/vnpay/ipn", "/payment/vnpay/return")
                                                .permitAll()
                                                .requestMatchers("/seller/**").hasAnyRole("SELLER", "ADMIN")
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
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
                                                .rememberMeParameter("remember-me")
                                                .useSecureCookie(false))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login.html?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID", "remember-me")
                                                .permitAll());

                logger.info("Security configuration initialized with remember-me enabled");

                return http.build();
        }
}
