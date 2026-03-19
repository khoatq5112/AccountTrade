package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Payment;
import com.group3.accounttrade.entity.PaymentCallback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentCallback entity.
 * Stores both Return URL and IPN callback data for audit and verification.
 */
@Repository
public interface PaymentCallbackRepository extends JpaRepository<PaymentCallback, Long> {

    List<PaymentCallback> findByPayment(Payment payment);

    List<PaymentCallback> findByPaymentOrderByReceivedAtDesc(Payment payment);

    Optional<PaymentCallback> findByVnpayTxnRef(String vnpayTxnRef);

    @Query("SELECT pc FROM PaymentCallback pc WHERE pc.payment.order.orderNumber = :orderNumber ORDER BY pc.receivedAt DESC")
    List<PaymentCallback> findByOrderNumberOrderByReceivedAtDesc(String orderNumber);

    @Query("SELECT pc FROM PaymentCallback pc WHERE pc.callbackType = :callbackType AND pc.payment = :payment")
    List<PaymentCallback> findByCallbackTypeAndPayment(String callbackType, Payment payment);

    @Query("SELECT COUNT(pc) FROM PaymentCallback pc WHERE pc.payment = :payment AND pc.callbackType = 'IPN'")
    long countIpnCallbacksByPayment(Payment payment);

    boolean existsByVnpayTxnRef(String vnpayTxnRef);
}
