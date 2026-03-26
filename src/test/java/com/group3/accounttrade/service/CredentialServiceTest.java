package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.AuditLog;
import com.group3.accounttrade.entity.CredentialAssignment;
import com.group3.accounttrade.entity.CredentialStatus;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.PostCredential;
import com.group3.accounttrade.entity.StockStatus;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.AuditLogRepository;
import com.group3.accounttrade.repository.CredentialAssignmentRepository;
import com.group3.accounttrade.repository.CredentialStatusRepository;
import com.group3.accounttrade.repository.NotificationRepository;
import com.group3.accounttrade.repository.OrderItemRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.PostCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    @Mock
    private PostCredentialRepository postCredentialRepository;

    @Mock
    private CredentialStatusRepository credentialStatusRepository;

    @Mock
    private CredentialAssignmentRepository credentialAssignmentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PostService postService;

    @InjectMocks
    private CredentialService credentialService;

    @Test
    void reserveCredentialsUpdatesPostStockAfterHoldingLastCredential() {
        User buyer = User.builder().userId(1).username("buyer").build();
        User seller = User.builder().userId(2).username("seller").build();
        Post post = Post.builder().postId(9).title("Coursera").seller(seller).stockStatus(StockStatus.IN_STOCK).build();
        Order order = Order.builder().orderId(10L).orderNumber("ORD-10").buyer(buyer).build();
        OrderItem orderItem = OrderItem.builder().order(order).post(post).build();
        CredentialStatus availableStatus = CredentialStatus.builder().statusName(CredentialService.STATUS_AVAILABLE).build();
        CredentialStatus holdingStatus = CredentialStatus.builder().statusName(CredentialService.STATUS_HOLDING).build();
        PostCredential credential = PostCredential.builder().credentialId(100).post(post).credentialStatus(availableStatus).build();

        when(credentialStatusRepository.findByStatusName(CredentialService.STATUS_AVAILABLE)).thenReturn(Optional.of(availableStatus));
        when(credentialStatusRepository.findByStatusName(CredentialService.STATUS_HOLDING)).thenReturn(Optional.of(holdingStatus));
        when(postCredentialRepository.findFirstByPost_PostIdAndCredentialStatusOrderByCreatedAtAsc(9, availableStatus))
                .thenReturn(Optional.of(credential));
        when(postCredentialRepository.save(any(PostCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        credentialService.reserveCredentials(post, 1, orderItem);

        verify(postService).updateStockStatus(9);
    }

    @Test
    void releaseOrderCredentialsUpdatesPostStockAfterReturningHeldCredential() {
        Post post = Post.builder().postId(9).title("Coursera").stockStatus(StockStatus.OUT_OF_STOCK).build();
        OrderItem orderItem = OrderItem.builder().orderItemId(20L).post(post).build();
        Order order = Order.builder().orderId(10L).orderNumber("ORD-10").orderItems(List.of(orderItem)).build();
        CredentialStatus availableStatus = CredentialStatus.builder().statusName(CredentialService.STATUS_AVAILABLE).build();
        CredentialStatus holdingStatus = CredentialStatus.builder().statusName(CredentialService.STATUS_HOLDING).build();
        PostCredential credential = PostCredential.builder().credentialId(100).post(post).credentialStatus(holdingStatus).build();

        when(credentialStatusRepository.findByStatusName(CredentialService.STATUS_AVAILABLE)).thenReturn(Optional.of(availableStatus));
        when(credentialStatusRepository.findByStatusName(CredentialService.STATUS_HOLDING)).thenReturn(Optional.of(holdingStatus));
        when(postCredentialRepository.findByPost_PostIdAndCredentialStatus(9, holdingStatus)).thenReturn(List.of(credential));
        when(postCredentialRepository.save(any(PostCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        credentialService.releaseOrderCredentials(order);

        verify(postService).updateStockStatus(9);
    }

    @Test
    void markCredentialsAsConfirmedUpdatesPostStockAfterSellingCredential() {
        Post post = Post.builder().postId(9).title("Coursera").stockStatus(StockStatus.IN_STOCK).build();
        OrderItem orderItem = OrderItem.builder().orderItemId(20L).post(post).build();
        Order order = Order.builder().orderId(10L).orderNumber("ORD-10").buyer(User.builder().userId(1).build()).orderItems(List.of(orderItem)).build();
        CredentialStatus soldStatus = CredentialStatus.builder().statusName(CredentialService.STATUS_SOLD).build();
        PostCredential credential = PostCredential.builder().credentialId(100).post(post).build();
        CredentialAssignment assignment = CredentialAssignment.builder().assignmentId(30L).orderItem(orderItem).credential(credential).build();

        when(credentialStatusRepository.findByStatusName(CredentialService.STATUS_SOLD)).thenReturn(Optional.of(soldStatus));
        when(credentialAssignmentRepository.findByOrderItem(orderItem)).thenReturn(List.of(assignment));
        when(postCredentialRepository.save(any(PostCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(credentialAssignmentRepository.save(any(CredentialAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        credentialService.markCredentialsAsConfirmed(order);

        verify(postService).updateStockStatus(9);
    }
}
