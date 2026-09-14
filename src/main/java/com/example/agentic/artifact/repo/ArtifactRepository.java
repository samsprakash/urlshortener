package com.example.agentic.artifact.repo;

import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.ArtifactStatus;
import com.example.agentic.artifact.ArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

    List<Artifact> findByWorkflowIdOrderByCreatedAtAsc(UUID workflowId);

    List<Artifact> findByWorkflowIdAndTypeOrderByVersionDesc(UUID workflowId, ArtifactType type);

    default Optional<Artifact> findLatestActive(UUID workflowId, ArtifactType type) {
        return findByWorkflowIdAndTypeOrderByVersionDesc(workflowId, type).stream()
                .filter(a -> a.getStatus() == ArtifactStatus.ACTIVE)
                .findFirst();
    }

    List<Artifact> findByWorkflowIdAndStatus(UUID workflowId, ArtifactStatus status);
}
