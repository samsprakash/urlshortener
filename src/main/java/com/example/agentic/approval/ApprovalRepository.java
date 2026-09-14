package com.example.agentic.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    List<Approval> findByWorkflowIdOrderByRequestedAtAsc(UUID workflowId);

    Optional<Approval> findByNodeIdAndStatus(UUID nodeId, ApprovalStatus status);
}
