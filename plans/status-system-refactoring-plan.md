# Status System Refactoring Plan

## Overview
This plan addresses multiple changes to the status system:
1. Remove password strength checking from login page
2. Fix category deletion foreign key error
3. Replace PostStatus with StockStatus for Posts
4. Create new CredentialStatus table for post_credentials

---

## Part 1: Remove Password Strength from Login Page

### File: `src/main/resources/templates/login.html`

**Changes:**
- Remove password strength validation section (lines ~239-245)
- Remove `strengthBar` element
- Remove `passwordHint` element
- Remove password strength checking logic from `validatePassword()` function
- Keep only basic password non-empty validation

---

## Part 2: Fix Category Deletion Foreign Key Error

### Current Issue:
When trying to delete a category that has posts referencing it, MySQL throws a foreign key constraint error.

### Solution Options:

**Option A: Soft Delete (Recommended)**
- Add `is_active` column to Categories table
- Mark categories as inactive instead of deleting
- Filter out inactive categories in queries

**Option B: SET NULL on Delete**
- Modify foreign key constraint to SET NULL on delete
- Posts with deleted category will have NULL category_id

**Option C: Restrict Deletion**
- Prevent deletion of categories that have posts
- Show error message to user

### Recommended: Option B - SET NULL on Delete

**Changes:**

1. **Update `schema.sql`:**
```sql
ALTER TABLE Posts 
DROP FOREIGN KEY posts_ibfk_3;

ALTER TABLE Posts 
ADD CONSTRAINT fk_posts_category 
FOREIGN KEY (category_id) REFERENCES Categories(category_id) 
ON DELETE SET NULL;
```

2. **Update `Category.java` entity:**
- No changes needed, JPA will handle SET NULL

---

## Part 3: Replace PostStatus with StockStatus for Posts

### Current State:
- `Post_Statuses` table with: Available, Holding, Sold, Hidden
- `Posts.status_id` references Post_Statuses
- `Posts.stock_status` enum (IN_STOCK, OUT_OF_STOCK) - already exists

### New State:
- Remove `status_id` from Post entity
- Use only `stock_status` enum (IN_STOCK, OUT_OF_STOCK)
- PostStatus table will no longer be used for Posts

### Changes:

1. **Update `Post.java`:**
```java
// REMOVE this field:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "status_id")
private PostStatus status;

// KEEP this field:
@Enumerated(EnumType.STRING)
@Column(name = "stock_status")
private StockStatus stockStatus;
```

2. **Update `PostService.java`:**
- Remove references to `PostStatus`
- Remove `getAllPostStatuses()` method
- Update `createPost()` to not set status
- Update `updatePost()` to not handle statusId

3. **Update `SellerController.java`:**
- Remove status dropdown from edit form
- Remove statusId parameter from update method

4. **Update Templates:**
- `seller_edit_post.html`: Remove status selection dropdown
- `seller_posts.html`: Update status display to use stockStatus

---

## Part 4: Create New CredentialStatus Table

### Current State:
- `CredentialStatus` enum in Java with: AVAILABLE, SOLD
- `Post_Credentials.credential_status` enum column

### New State:
- `Credential_Statuses` table with: Available, Holding, Sold, Hidden
- `Post_Credentials.credential_status_id` references Credential_Statuses
- `CredentialStatus` Java entity (not enum)

### Changes:

1. **Create `CredentialStatus.java` entity:**
```java
@Data
@Entity
@Table(name = "Credential_Statuses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "status_name", nullable = false, unique = true)
    private String statusName;

    @Column(name = "description")
    private String description;
}
```

2. **Create `CredentialStatusRepository.java`:**
```java
@Repository
public interface CredentialStatusRepository extends JpaRepository<CredentialStatus, Integer> {
    Optional<CredentialStatus> findByStatusName(String statusName);
}
```

3. **Update `PostCredential.java`:**
```java
// REPLACE:
@Enumerated(EnumType.STRING)
@Column(name = "credential_status")
private CredentialStatus credentialStatus;

// WITH:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "credential_status_id")
private CredentialStatus credentialStatus;
```

4. **Delete old `CredentialStatus.java` enum file**

5. **Update `PostCredentialRepository.java`:**
- Change enum parameter to Integer or entity

6. **Update `PostService.java`:**
- Fetch CredentialStatus entity instead of using enum
- Update methods: `addCredential()`, `updateStockStatus()`, `assignCredentialToTransaction()`

7. **Update `DataInitializer.java`:**
```java
private void initCredentialStatuses() {
    if (credentialStatusRepository.count() == 0) {
        credentialStatusRepository.save(CredentialStatus.builder()
            .statusName("Available").description("Ready for sale").build());
        credentialStatusRepository.save(CredentialStatus.builder()
            .statusName("Holding").description("Held during transaction").build());
        credentialStatusRepository.save(CredentialStatus.builder()
            .statusName("Sold").description("Sold to buyer").build());
        credentialStatusRepository.save(CredentialStatus.builder()
            .statusName("Hidden").description("Hidden from inventory").build());
    }
}
```

8. **Update Templates:**
- `seller_post_credentials.html`: Update status badge logic

---

## Part 5: SQL Migration Script

### File: `migration-status-refactoring.sql`

```sql
-- ============================================
-- Status System Refactoring Migration
-- ============================================

-- Step 1: Fix category foreign key
ALTER TABLE Posts DROP FOREIGN KEY IF EXISTS posts_ibfk_3;
ALTER TABLE Posts ADD CONSTRAINT fk_posts_category 
    FOREIGN KEY (category_id) REFERENCES Categories(category_id) 
    ON DELETE SET NULL;

-- Step 2: Create Credential_Statuses table
CREATE TABLE IF NOT EXISTS Credential_Statuses (
    status_id INT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Step 3: Insert credential statuses
INSERT INTO Credential_Statuses (status_name, description) VALUES
    ('Available', 'Ready for sale'),
    ('Holding', 'Held during transaction'),
    ('Sold', 'Sold to buyer'),
    ('Hidden', 'Hidden from inventory');

-- Step 4: Add new credential_status_id column to Post_Credentials
ALTER TABLE Post_Credentials ADD COLUMN credential_status_id INT;

-- Step 5: Migrate existing data from enum to foreign key
UPDATE Post_Credentials SET credential_status_id = 
    CASE credential_status
        WHEN 'AVAILABLE' THEN 1
        WHEN 'SOLD' THEN 3
        ELSE 1
    END;

-- Step 6: Make credential_status_id NOT NULL and add foreign key
ALTER TABLE Post_Credentials MODIFY COLUMN credential_status_id INT NOT NULL;
ALTER TABLE Post_Credentials ADD CONSTRAINT fk_credential_status 
    FOREIGN KEY (credential_status_id) REFERENCES Credential_Statuses(status_id);

-- Step 7: Drop old enum column
ALTER TABLE Post_Credentials DROP COLUMN credential_status;

-- Step 8: Remove status_id from Posts (optional - for cleanup)
-- ALTER TABLE Posts DROP FOREIGN KEY fk_posts_status;
-- ALTER TABLE Posts DROP COLUMN status_id;

-- Note: Post_Statuses table can be kept for backward compatibility or dropped later
```

---

## Implementation Order

1. **Phase 1: Login Page** (Simple)
   - Remove password strength checking from login.html

2. **Phase 2: Category Fix** (Database)
   - Run ALTER TABLE to fix foreign key constraint

3. **Phase 3: CredentialStatus Table** (Entity changes)
   - Create CredentialStatus entity
   - Create CredentialStatusRepository
   - Update DataInitializer
   - Run migration to create table and populate data

4. **Phase 4: PostCredential Update** (Entity changes)
   - Update PostCredential entity
   - Update PostCredentialRepository
   - Run migration to add new column

5. **Phase 5: Post Entity Update** (Entity changes)
   - Remove status field from Post entity
   - Update PostService
   - Update SellerController

6. **Phase 6: Template Updates** (UI changes)
   - Update seller_edit_post.html
   - Update seller_post_credentials.html
   - Update seller_posts.html

7. **Phase 7: Cleanup** (Final)
   - Remove old enum file
   - Remove unused PostStatus references
   - Test all functionality

---

## Files to Modify

### Java Files:
- `src/main/java/com/group3/accounttrade/entity/Post.java`
- `src/main/java/com/group3/accounttrade/entity/PostCredential.java`
- `src/main/java/com/group3/accounttrade/entity/CredentialStatus.java` (replace enum with entity)
- `src/main/java/com/group3/accounttrade/repository/CredentialStatusRepository.java` (new)
- `src/main/java/com/group3/accounttrade/repository/PostCredentialRepository.java`
- `src/main/java/com/group3/accounttrade/service/PostService.java`
- `src/main/java/com/group3/accounttrade/controller/SellerController.java`
- `src/main/java/com/group3/accounttrade/config/DataInitializer.java`

### Template Files:
- `src/main/resources/templates/login.html`
- `src/main/resources/templates/seller_edit_post.html`
- `src/main/resources/templates/seller_post_credentials.html`
- `src/main/resources/templates/seller_posts.html`

### SQL Files:
- `src/main/resources/static/schema/migration-status-refactoring.sql` (new)
- `src/main/resources/static/schema/schema.sql` (update)

---

## Testing Checklist

- [ ] Login page works without password strength checking
- [ ] Category can be deleted (posts get NULL category)
- [ ] New credential statuses appear in database
- [ ] Credentials can be created with Available status
- [ ] Credentials can be updated to Holding/Sold/Hidden
- [ ] Post stock status displays correctly
- [ ] Seller can create/edit posts without status dropdown
- [ ] All existing functionality still works
