package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.CredentialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredentialStatusRepository extends JpaRepository<CredentialStatus, Integer> {
    
    Optional<CredentialStatus> findByStatusName(String statusName);
    
    boolean existsByStatusName(String statusName);
}
