package com.group3.accounttrade.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    // Session key that login.html reads to pre-fill the email
    public static final String LAST_USERNAME_KEY = "SPRING_SECURITY_LAST_USERNAME";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        // Save the attempted username in the session so the login page can restore it
        String username = request.getParameter("username");
        HttpSession session = request.getSession();
        if (username != null) {
            session.setAttribute(LAST_USERNAME_KEY, username);
        }

        response.sendRedirect("/login.html?error");
    }
}
