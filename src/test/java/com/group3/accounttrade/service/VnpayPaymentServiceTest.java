package com.group3.accounttrade.service;

import com.group3.accounttrade.config.VnpayConfig;
import com.group3.accounttrade.entity.PaymentCallback;
import com.group3.accounttrade.repository.AuditLogRepository;
import com.group3.accounttrade.repository.EscrowRepository;
import com.group3.accounttrade.repository.EscrowStatusRepository;
import com.group3.accounttrade.repository.EscrowTransactionRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.PaymentCallbackRepository;
import com.group3.accounttrade.repository.PaymentRepository;
import com.group3.accounttrade.repository.PaymentStatusRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VnpayPaymentServiceTest {

    @Mock
    private VnpayConfig vnpayConfig;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentCallbackRepository paymentCallbackRepository;

    @Mock
    private PaymentStatusRepository paymentStatusRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private EscrowRepository escrowRepository;

    @Mock
    private EscrowStatusRepository escrowStatusRepository;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CredentialService credentialService;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private VnpayPaymentService vnpayPaymentService;

    @Test
    void processReturnCallbackConfirmsTopUpBeforeRedirectingBackToCheckout() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TU-12345");
        params.put("vnp_TransactionNo", "999888");
        params.put("vnp_ResponseCode", VnpayConfig.RESPONSE_SUCCESS);
        params.put("vnp_TransactionStatus", VnpayConfig.TXN_STATUS_SUCCESS);
        params.put("vnp_Amount", "21000000");
        params.put("vnp_SecureHash", "hash");

        when(request.getQueryString()).thenReturn("vnp_TxnRef=TU-12345");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(vnpayConfig.validateChecksum(params)).thenReturn(true);
        when(vnpayConfig.getClientIpAddress(request)).thenReturn("127.0.0.1");
        when(paymentCallbackRepository.save(any(PaymentCallback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.getPendingPostIdForTopUp("TU-12345")).thenReturn(12);

        VnpayPaymentService.PaymentResult result = vnpayPaymentService.processReturnCallback(params, request);

        assertTrue(result.isSuccess());
        assertTrue(result.isTopUp());
        assertEquals(12, result.getPendingPostId());
        verify(walletService).confirmTopUp("TU-12345", "999888");
    }
}
