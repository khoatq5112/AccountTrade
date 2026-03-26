package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Wallet;
import com.group3.accounttrade.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);

    List<WalletTransaction> findByWallet_User_UserId(Integer userId);
}
