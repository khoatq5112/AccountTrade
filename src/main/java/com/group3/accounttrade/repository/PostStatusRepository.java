package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostStatusRepository extends JpaRepository<PostStatus, Integer> {
    Optional<PostStatus> findByStatusName(String statusName);
}
