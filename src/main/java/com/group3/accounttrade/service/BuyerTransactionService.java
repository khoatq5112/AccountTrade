package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.CartRepository;
import com.group3.accounttrade.repository.PostCredentialRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.TransactionRepository;
import com.group3.accounttrade.repository.TransactionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuyerTransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final PostRepository postRepository;
    private final CartRepository cartRepository;
    private final PostCredentialRepository postCredentialRepository;
    private final PostService postService;

    /**
     * Checkout a post - assigns an available credential to the buyer.
     *
     * @param buyer  the buyer making the purchase
     * @param postId the post ID to purchase
     * @return the created transaction
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if no credentials available
     */
    @Transactional
    public Transaction checkoutPost(User buyer, Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        // Validate buyer is not seller
        if (post.getSeller() != null && post.getSeller().getUserId().equals(buyer.getUserId())) {
            throw new IllegalArgumentException("You cannot purchase your own post");
        }

        // Check stock status
        if (post.getStockStatus() != StockStatus.IN_STOCK) {
            throw new IllegalStateException("Product is out of stock");
        }

        // Verify there's an available credential
        if (!postService.hasAvailableCredentials(postId)) {
            throw new IllegalStateException("No available credentials for this post");
        }

        // Create transaction with COMPLETED status (instant delivery)
        TransactionStatus completedStatus = transactionStatusRepository.findByStatusName("Completed")
                .orElseThrow(() -> new IllegalStateException("Completed status not found"));

        Transaction transaction = Transaction.builder()
                .post(post)
                .buyer(buyer)
                .seller(post.getSeller())
                .amount(post.getPrice())
                .fee(BigDecimal.ZERO)
                .status(completedStatus)
                .build();

        transaction = transactionRepository.save(transaction);

        // Assign an available credential to this transaction
        PostCredential credential = postService.assignCredentialToTransaction(postId, transaction);

        // Remove from cart if present
        cartRepository.findByUserAndPost(buyer, post).ifPresent(cartRepository::delete);

        return transaction;
    }

    /**
     * Gets all purchases for a buyer.
     *
     * @param buyer the buyer
     * @return list of transactions
     */
    @Transactional(readOnly = true)
    public List<Transaction> getPurchases(User buyer) {
        return transactionRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    /**
     * Gets a specific transaction for a buyer with credential information.
     *
     * @param buyer         the buyer
     * @param transactionId the transaction ID
     * @return the transaction if found and belongs to buyer
     * @throws IllegalArgumentException if transaction not found or access denied
     */
    @Transactional(readOnly = true)
    public Transaction getPurchaseForBuyer(User buyer, Integer transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // Verify ownership
        if (!transaction.getBuyer().getUserId().equals(buyer.getUserId())) {
            throw new IllegalArgumentException("You do not have access to this transaction");
        }

        return transaction;
    }

    /**
     * Gets the credential for a purchased transaction.
     * Only the buyer who purchased can access this.
     *
     * @param buyer         the buyer
     * @param transactionId the transaction ID
     * @return the credential assigned to this transaction
     * @throws IllegalArgumentException if transaction not found or access denied
     */
    @Transactional(readOnly = true)
    public PostCredential getPurchasedCredential(User buyer, Integer transactionId) {
        // Verify buyer owns the transaction
        Transaction transaction = getPurchaseForBuyer(buyer, transactionId);

        // Get the credential assigned to this transaction
        return postCredentialRepository.findBySoldToOrder_TransactionId(transaction.getTransactionId())
                .orElseThrow(() -> new IllegalStateException("No credential found for this transaction"));
    }
}
