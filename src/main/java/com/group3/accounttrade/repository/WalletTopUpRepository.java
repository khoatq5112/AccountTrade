package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Wallet;
import com.group3.accounttrade.entity.WalletTopUp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTopUpRepository extends JpaRepository<WalletTopUp, Long> {

    Optional<WalletTopUp> findByVnpayTxnRef(String vnpayTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wt FROM WalletTopUp wt WHERE wt.vnpayTxnRef = :vnpayTxnRef")
    Optional<WalletTopUp> findLockedByVnpayTxnRef(@Param("vnpayTxnRef") String vnpayTxnRef);

    List<WalletTopUp> findByWalletAndStatusOrderByCreatedAtDesc(Wallet wallet, String status);

    List<WalletTopUp> findByWallet_User_UserIdOrderByCreatedAtDesc(Integer userId);
}
