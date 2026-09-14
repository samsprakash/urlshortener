package com.example.agentic.orchestration.repo;

import com.example.agentic.orchestration.state.NodeStatus;
import com.example.agentic.orchestration.state.WorkflowNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, UUID> {

    List<WorkflowNode> findByWorkflowIdOrderByCreatedAtAsc(UUID workflowId);

    Optional<WorkflowNode> findByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);

    List<WorkflowNode> findByWorkflowIdAndStatus(UUID workflowId, NodeStatus status);
}
