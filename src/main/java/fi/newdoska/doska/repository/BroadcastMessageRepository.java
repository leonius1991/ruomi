package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.BroadcastMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessage, Long> {
    
    List<BroadcastMessage> findBySentOrderByCreatedAtDesc(Boolean sent);
    
    List<BroadcastMessage> findAllByOrderByCreatedAtDesc();
}

