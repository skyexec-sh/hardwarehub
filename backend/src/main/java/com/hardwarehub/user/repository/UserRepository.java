package com.hardwarehub.user.repository;

import com.hardwarehub.user.domain.RoleName;
import com.hardwarehub.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    @Query("""
            SELECT DISTINCT u FROM User u LEFT JOIN u.roles r
            WHERE u.deletedAt IS NULL
              AND (:username IS NULL OR :username = '' OR
                   LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
              AND (:name IS NULL OR :name = '' OR
                   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR
                   LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%')) OR
                   LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:email IS NULL OR :email = '' OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (:role IS NULL OR r.name = :role)
              AND (:active IS NULL OR u.active = :active)
              AND (
                :search IS NULL OR :search = '' OR
                LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<User> search(
            @Param("search") String search,
            @Param("username") String username,
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") RoleName role,
            @Param("active") Boolean active,
            Pageable pageable);
}
