package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    Optional<Wallet> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.user = :user")
    Optional<Wallet> findByUserForUpdate(@Param("user") User user);

    Optional<Wallet> findByUser_Username(String username);

    Optional<Wallet> findByUser_UserId(Integer userId);
}
