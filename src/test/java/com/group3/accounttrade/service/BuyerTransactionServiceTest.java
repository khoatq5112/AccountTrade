package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.StockStatus;
import com.group3.accounttrade.entity.Role;
import com.group3.accounttrade.entity.Transaction;
import com.group3.accounttrade.entity.TransactionStatus;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.CartRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.TransactionRepository;
import com.group3.accounttrade.repository.TransactionStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuyerTransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionStatusRepository transactionStatusRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PostService postService;

    @InjectMocks
    private BuyerTransactionService buyerTransactionService;

    private User buyer;
    private User seller;
    private Post post;
    private TransactionStatus completedStatus;

    @BeforeEach
    void setUp() {
        buyer = User.builder()
                .userId(11)
                .username("buyer")
                .role(Role.builder().roleName("Buyer").build())
                .build();
        seller = User.builder()
                .userId(22)
                .username("seller")
                .role(Role.builder().roleName("Seller").build())
                .build();

        completedStatus = TransactionStatus.builder().statusId(1).statusName("Completed").build();

        post = Post.builder()
                .postId(30)
                .title("ChatGPT Plus")
                .seller(seller)
                .price(BigDecimal.valueOf(499000))
                .stockStatus(StockStatus.IN_STOCK)
                .build();
    }

    @Test
    void checkoutCreatesCompletedTransaction() {
        when(postRepository.findById(30)).thenReturn(Optional.of(post));
        when(transactionStatusRepository.findByStatusName("Completed")).thenReturn(Optional.of(completedStatus));
        when(postService.hasAvailableCredentials(30)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction transaction = buyerTransactionService.checkoutPost(buyer, 30);

        assertEquals(buyer, transaction.getBuyer());
        assertEquals(seller, transaction.getSeller());
        assertEquals(post, transaction.getPost());
        assertEquals(BigDecimal.ZERO, transaction.getFee());
        assertEquals(completedStatus, transaction.getStatus());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals("Completed", transactionCaptor.getValue().getStatus().getStatusName());
        verify(postService).assignCredentialToTransaction(any(Integer.class), any(Transaction.class));
    }

    @Test
    void checkoutRejectsOwnPost() {
        Post ownPost = Post.builder()
                .postId(31)
                .seller(buyer)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
        when(postRepository.findById(31)).thenReturn(Optional.of(ownPost));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buyerTransactionService.checkoutPost(buyer, 31));

        assertEquals("You cannot purchase your own post", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void checkoutRejectsOutOfStockPost() {
        post.setStockStatus(StockStatus.OUT_OF_STOCK);
        when(postRepository.findById(30)).thenReturn(Optional.of(post));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> buyerTransactionService.checkoutPost(buyer, 30));

        assertEquals("Product is out of stock", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
