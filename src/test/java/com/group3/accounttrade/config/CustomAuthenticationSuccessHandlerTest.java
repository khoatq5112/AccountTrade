package com.group3.accounttrade.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomAuthenticationSuccessHandlerTest {

    private final CustomAuthenticationSuccessHandler handler = new CustomAuthenticationSuccessHandler();

    @Test
    void buyerRedirectsToHomePage() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new UsernamePasswordAuthenticationToken("buyer", "pw", AuthorityUtils.createAuthorityList("ROLE_BUYER")));

        assertEquals("/", response.getRedirectedUrl());
    }

    @Test
    void sellerRedirectsToSellerDashboard() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new UsernamePasswordAuthenticationToken("seller", "pw", AuthorityUtils.createAuthorityList("ROLE_SELLER")));

        assertEquals("/seller/dashboard", response.getRedirectedUrl());
    }
}
