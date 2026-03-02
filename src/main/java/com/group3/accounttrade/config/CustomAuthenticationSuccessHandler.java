package com.group3.accounttrade.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String redirectUrl = "/";

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority grantedAuthority : authorities) {
            String authorityName = grantedAuthority.getAuthority();
            if (authorityName.equals("ROLE_BUYER")) {
                redirectUrl = "/buyer_dashboard.html";
                break;
            } else if (authorityName.equals("ROLE_SELLER")) {
                redirectUrl = "/seller_dashboard.html";
                break;
            } else if (authorityName.equals("ROLE_ADMIN")) {
                redirectUrl = "/admin_dashboard.html";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
