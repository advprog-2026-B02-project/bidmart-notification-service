package id.ac.ui.cs.advprog.bidmart.notifications.repository;

import id.ac.ui.cs.advprog.bidmart.notifications.model.ProcessedKafkaEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEvent, UUID> {
    boolean existsByEventId(UUID eventId);
}