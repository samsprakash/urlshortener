package com.example.agentic.orchestration.repo;

import com.example.agentic.orchestration.state.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
}
