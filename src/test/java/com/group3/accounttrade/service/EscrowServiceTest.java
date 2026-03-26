package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.AuditLog;
import com.group3.accounttrade.entity.Category;
import com.group3.accounttrade.entity.Escrow;
import com.group3.accounttrade.entity.EscrowStatus;
import com.group3.accounttrade.entity.EscrowTransaction;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.repository.AuditLogRepository;
import com.group3.accounttrade.repository.EscrowRepository;
import com.group3.accounttrade.repository.EscrowStatusRepository;
import com.group3.accounttrade.repository.EscrowTransactionRepository;
import com.group3.accounttrade.repository.NotificationRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

    @Mock
    private EscrowRepository escrowRepository;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private EscrowStatusRepository escrowStatusRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CommissionService commissionService;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private EscrowService escrowService;

    @Test
    void createEscrowUsesManagedEscrowInstanceForTransactionRecord() {
        Category category = Category.builder().categoryId(3).categoryName("Learning").build();
        Post post = Post.builder().postId(12).category(category).build();
        OrderItem orderItem = OrderItem.builder().orderItemId(20L).post(post).build();
        Order order = Order.builder()
                .orderId(10L)
                .orderNumber("ORD-10")
                .totalAmount(BigDecimal.valueOf(59_000))
                .orderItems(List.of(orderItem))
                .build();
        EscrowStatus holdingStatus = EscrowStatus.builder().statusName(EscrowService.STATUS_HOLDING).build();
        Escrow persistedEscrow = Escrow.builder()
                .escrowId(99L)
                .order(order)
                .escrowStatus(holdingStatus)
                .amount(BigDecimal.valueOf(59_000))
                .platformFee(BigDecimal.valueOf(2_950))
                .sellerAmount(BigDecimal.valueOf(56_050))
                .build();

        when(escrowRepository.findByOrder(order)).thenReturn(Optional.empty());
        when(escrowStatusRepository.findByStatusName(EscrowService.STATUS_HOLDING)).thenReturn(Optional.of(holdingStatus));
        when(commissionService.getEffectiveRate(category)).thenReturn(BigDecimal.valueOf(5));
        when(commissionService.calculateFee(BigDecimal.valueOf(59_000), BigDecimal.valueOf(5)))
                .thenReturn(BigDecimal.valueOf(2_950));
        when(escrowRepository.saveAndFlush(any(Escrow.class))).thenReturn(persistedEscrow);
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Escrow escrow = escrowService.createEscrow(order);

        assertSame(persistedEscrow, escrow);
        ArgumentCaptor<EscrowTransaction> transactionCaptor = ArgumentCaptor.forClass(EscrowTransaction.class);
        verify(escrowTransactionRepository).save(transactionCaptor.capture());
        assertSame(persistedEscrow, transactionCaptor.getValue().getEscrow());
        assertEquals(EscrowTransaction.TYPE_CREATED, transactionCaptor.getValue().getTransactionType());
    }
}
