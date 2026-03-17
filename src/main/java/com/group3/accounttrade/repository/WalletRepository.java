package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    Optional<Wallet> findByUser(User user);

    Optional<Wallet> findByUser_Username(String username);

    Optional<Wallet> findByUser_UserId(Integer userId);
}
