package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.CredentialAssignment;
import com.group3.accounttrade.entity.Escrow;
import com.group3.accounttrade.entity.EscrowStatus;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.Payment;
import com.group3.accounttrade.entity.PaymentStatus;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.PostCredential;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.CredentialAssignmentRepository;
import com.group3.accounttrade.repository.EscrowRepository;
import com.group3.accounttrade.repository.EscrowStatusRepository;
import com.group3.accounttrade.repository.OrderItemRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.PaymentRepository;
import com.group3.accounttrade.repository.PaymentStatusRepository;
import com.group3.accounttrade.repository.PostCredentialRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyTransactionMigrationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private EscrowRepository escrowRepository;
    @Mock
    private CredentialAssignmentRepository credentialAssignmentRepository;
    @Mock
    private OrderStatusRepository orderStatusRepository;
    @Mock
    private PaymentStatusRepository paymentStatusRepository;
    @Mock
    private EscrowStatusRepository escrowStatusRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostCredentialRepository postCredentialRepository;

    @InjectMocks
    private LegacyTransactionMigrationService migrationService;

    @Test
    void migrateCompletedLegacyTransactionCreatesOrderPaymentEscrowAndAssignment() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
        User buyer = User.builder().userId(10).username("buyer").build();
        User seller = User.builder().userId(20).username("seller").build();
        Post post = Post.builder().postId(30).title("Netflix Premium").seller(seller).build();
        PostCredential credential = PostCredential.builder().credentialId(40).post(post).build();

        LegacyTransactionMigrationService.LegacyTransactionRecord record = LegacyTransactionMigrationService.LegacyTransactionRecord.builder()
                .transactionId(99)
                .postId(30)
                .buyerId(10)
                .sellerId(20)
                .amount(BigDecimal.valueOf(150_000))
                .fee(BigDecimal.valueOf(5_000))
                .statusName("Completed")
                .createdAt(createdAt)
                .processedAt(createdAt.plusMinutes(10))
                .credentialId(40)
                .build();

        when(orderRepository.findByOrderNumber("LEGACY-TRX-99")).thenReturn(Optional.empty());
        when(userRepository.findById(10)).thenReturn(Optional.of(buyer));
        when(userRepository.findById(20)).thenReturn(Optional.of(seller));
        when(postRepository.findById(30)).thenReturn(Optional.of(post));
        when(postCredentialRepository.findById(40)).thenReturn(Optional.of(credential));
        when(orderStatusRepository.findByStatusName(OrderStatus.COMPLETED))
                .thenReturn(Optional.of(OrderStatus.builder().statusName(OrderStatus.COMPLETED).build()));
        when(paymentStatusRepository.findByStatusName(PaymentStatus.PAID))
                .thenReturn(Optional.of(PaymentStatus.builder().statusName(PaymentStatus.PAID).build()));
        when(escrowStatusRepository.findByStatusName(EscrowStatus.RELEASED))
                .thenReturn(Optional.of(EscrowStatus.builder().statusName(EscrowStatus.RELEASED).build()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowRepository.save(any(Escrow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(credentialAssignmentRepository.save(any(CredentialAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        migrationService.migrate(List.of(record));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals("LEGACY-TRX-99", orderCaptor.getValue().getOrderNumber());
        assertEquals(OrderStatus.COMPLETED, orderCaptor.getValue().getOrderStatus().getStatusName());

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        assertEquals("Netflix Premium", itemCaptor.getValue().getPostTitleSnapshot());
        assertEquals(credential, itemCaptor.getValue().getAssignedCredential());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals("LEGACY-PAY-99", paymentCaptor.getValue().getVnpayTxnRef());

        ArgumentCaptor<Escrow> escrowCaptor = ArgumentCaptor.forClass(Escrow.class);
        verify(escrowRepository).save(escrowCaptor.capture());
        assertEquals(EscrowStatus.RELEASED, escrowCaptor.getValue().getEscrowStatus().getStatusName());

        ArgumentCaptor<CredentialAssignment> assignmentCaptor = ArgumentCaptor.forClass(CredentialAssignment.class);
        verify(credentialAssignmentRepository).save(assignmentCaptor.capture());
        assertEquals(CredentialAssignment.STATUS_CONFIRMED, assignmentCaptor.getValue().getAssignmentStatus());
    }
}
