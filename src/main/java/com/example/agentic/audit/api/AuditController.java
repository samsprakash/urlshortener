package com.example.agentic.audit.api;

import com.example.agentic.audit.AuditEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows/{id}/audit")
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    public AuditController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    public List<AuditEventResponse> auditTrail(@PathVariable UUID id) {
        return auditEventRepository.findByWorkflowIdOrderByCreatedAtAsc(id).stream()
                .map(AuditEventResponse::from)
                .toList();
    }
}
