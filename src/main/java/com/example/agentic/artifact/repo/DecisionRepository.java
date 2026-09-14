package com.example.agentic.artifact.repo;

import com.example.agentic.artifact.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DecisionRepository extends JpaRepository<Decision, UUID> {

    List<Decision> findByWorkflowIdOrderByCreatedAtAsc(UUID workflowId);
}
