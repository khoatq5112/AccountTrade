package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.CredentialStatus;
import com.group3.accounttrade.entity.PostCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostCredentialRepository extends JpaRepository<PostCredential, Integer> {

    /**
     * Find all credentials for a post.
     *
     * @param postId the post ID
     * @return list of all credentials for the post
     */
    List<PostCredential> findByPost_PostId(Integer postId);

    @Query("""
            select pc
            from PostCredential pc
            join fetch pc.credentialStatus cs
            left join fetch pc.soldToOrder sto
            where pc.post.postId = :postId
            order by pc.createdAt desc
            """)
    List<PostCredential> findAllForManagementByPostId(@Param("postId") Integer postId);

    /**
     * Find credentials for a post with pagination, filtering, and sorting.
     *
     * @param postId     the post ID
     * @param statusName the status name filter (can be null)
     * @param keyword    the search keyword for username (can be null)
     * @param pageable   pagination and sorting information
     * @return a page of credentials
     */
    @Query(value = """
            select pc
            from PostCredential pc
            join fetch pc.credentialStatus cs
            left join fetch pc.soldToOrder sto
            where pc.post.postId = :postId
            and (:statusName is null or cs.statusName = :statusName)
            and (:keyword is null or lower(pc.accountUsername) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(pc)
            from PostCredential pc
            join pc.credentialStatus cs
            where pc.post.postId = :postId
            and (:statusName is null or cs.statusName = :statusName)
            and (:keyword is null or lower(pc.accountUsername) like lower(concat('%', :keyword, '%')))
            """)
    Page<PostCredential> findByPostIdWithFilters(
            @Param("postId") Integer postId,
            @Param("statusName") String statusName,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * Find credentials by post ID and status.
     *
     * @param postId the post ID
     * @param credentialStatus the credential status entity
     * @return list of credentials matching the criteria
     */
    List<PostCredential> findByPost_PostIdAndCredentialStatus(Integer postId, CredentialStatus credentialStatus);

    /**
     * Count credentials by post ID and status.
     *
     * @param postId the post ID
     * @param credentialStatus the credential status entity
     * @return count of matching credentials
     */
    long countByPost_PostIdAndCredentialStatus(Integer postId, CredentialStatus credentialStatus);

    /**
     * Count all credentials for a post.
     *
     * @param postId the post ID
     * @return total count of credentials
     */
    long countByPost_PostId(Integer postId);

    /**
     * Find the first available credential for a post (oldest first - FIFO).
     * Used when assigning a credential to a buyer during checkout.
     *
     * @param postId the post ID
     * @param credentialStatus the credential status entity (should be Available)
     * @return Optional containing the credential if available
     */
    Optional<PostCredential> findFirstByPost_PostIdAndCredentialStatusOrderByCreatedAtAsc(
            Integer postId, 
            CredentialStatus credentialStatus
    );

    /**
     * Find the credential that was sold to a specific transaction/order.
     *
     * @param transactionId the transaction ID
     * @return Optional containing the credential if found
     */
    Optional<PostCredential> findBySoldToOrder_TransactionId(Integer transactionId);

    /**
     * Check if any credentials exist for a post with the given status.
     *
     * @param postId the post ID
     * @param credentialStatus the credential status entity
     * @return true if at least one matching credential exists
     */
    boolean existsByPost_PostIdAndCredentialStatus(Integer postId, CredentialStatus credentialStatus);

    /**
     * Delete all credentials for a post.
     *
     * @param postId the post ID
     */
    void deleteByPost_PostId(Integer postId);

    /**
     * Find a specific credential by ID with post information loaded.
     *
     * @param credentialId the credential ID
     * @return Optional containing the credential if found
     */
    Optional<PostCredential> findByCredentialId(Integer credentialId);

    /**
     * Check if a credential exists with the given ID and post ID.
     * Useful for security checks.
     *
     * @param credentialId the credential ID
     * @param postId the post ID
     * @return true if the credential belongs to the specified post
     */
    boolean existsByCredentialIdAndPost_PostId(Integer credentialId, Integer postId);
}
