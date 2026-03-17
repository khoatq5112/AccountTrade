package com.group3.accounttrade.controller;

import com.group3.accounttrade.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Test
    void checkEmailReturnsAvailablePayload() {
        when(userService.isEmailTaken("buyer@example.com")).thenReturn(false);

        var response = authController.checkEmail("buyer@example.com");

        assertEquals(200, response.getStatusCode().value());
        assertFalse((Boolean) response.getBody().get("exists"));
        assertTrue((Boolean) response.getBody().get("valid"));
    }

    @Test
    void checkEmailRejectsBlankEmail() {
        var response = authController.checkEmail("   ");

        assertEquals(400, response.getStatusCode().value());
        assertFalse((Boolean) response.getBody().get("exists"));
        assertFalse((Boolean) response.getBody().get("valid"));
    }
}
