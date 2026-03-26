package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Wallet;
import com.group3.accounttrade.entity.WalletTopUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTopUpRepository extends JpaRepository<WalletTopUp, Long> {

    Optional<WalletTopUp> findByVnpayTxnRef(String vnpayTxnRef);

    List<WalletTopUp> findByWalletAndStatusOrderByCreatedAtDesc(Wallet wallet, String status);

    List<WalletTopUp> findByWallet_User_UserIdOrderByCreatedAtDesc(Integer userId);
}
