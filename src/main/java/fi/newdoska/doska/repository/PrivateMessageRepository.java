package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.PrivateMessage;
import fi.newdoska.doska.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
    
    @Query("SELECT pm FROM PrivateMessage pm WHERE " +
           "((pm.sender = :user1 AND pm.recipient = :user2) OR " +
           "(pm.sender = :user2 AND pm.recipient = :user1)) AND " +
           "((pm.sender = :user1 AND pm.deletedBySender = false) OR " +
           "(pm.recipient = :user1 AND pm.deletedByRecipient = false)) " +
           "ORDER BY pm.sentAt ASC")
    List<PrivateMessage> findConversation(@Param("user1") User user1, @Param("user2") User user2);
    
    @Query("SELECT pm FROM PrivateMessage pm WHERE " +
           "pm.recipient = :user AND pm.read = false AND pm.deletedByRecipient = false " +
           "ORDER BY pm.sentAt DESC")
    List<PrivateMessage> findUnreadMessages(@Param("user") User user);
    
    @Query("SELECT DISTINCT u FROM User u WHERE u IN " +
           "(SELECT pm.recipient FROM PrivateMessage pm WHERE pm.sender = :user AND pm.deletedBySender = false) OR " +
           "u IN (SELECT pm.sender FROM PrivateMessage pm WHERE pm.recipient = :user AND pm.deletedByRecipient = false)")
    List<User> findConversationPartners(@Param("user") User user);
    
    long countByRecipientAndReadFalseAndDeletedByRecipientFalse(User recipient);
}

