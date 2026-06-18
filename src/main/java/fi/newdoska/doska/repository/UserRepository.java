package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByVerificationToken(String token);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.role IN ('USER','PREMIUM')")
    List<User> findAllActiveUsers();
    
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.role IN ('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    List<User> findAllModeratorsAndAdmins();
    
    @Query("SELECT u FROM User u WHERE u.username LIKE %:searchTerm% OR u.firstName LIKE %:searchTerm% OR u.lastName LIKE %:searchTerm%")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countUsersCreatedAfter(@Param("startDate") java.time.LocalDateTime startDate);
    
    Optional<User> findByTelegramId(Long telegramId);
    
    Page<User> findByUsernameContainingOrEmailContaining(String username, String email, Pageable pageable);
} 