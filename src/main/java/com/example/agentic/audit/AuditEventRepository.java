package com.example.agentic.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByWorkflowIdOrderByCreatedAtAsc(UUID workflowId);

    List<AuditEvent> findByEventTypeOrderByCreatedAtAsc(String eventType);
}
