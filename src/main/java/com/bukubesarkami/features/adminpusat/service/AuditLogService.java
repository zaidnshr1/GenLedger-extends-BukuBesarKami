package com.bukubesarkami.features.adminpusat.service;

import com.bukubesarkami.core.entity.AuditLog;
import com.bukubesarkami.core.entity.Project;
import com.bukubesarkami.core.entity.User;
import com.bukubesarkami.core.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public record AuditLogResponse(
            UUID id, UUID userId, String username,
            UUID projectId, String action, String entityType,
            UUID entityId, String oldValue, String newValue,
            String ipAddress, OffsetDateTime createdAt
    ) {}

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User user, Project project, String action,
                    String entityType, UUID entityId,
                    String oldValue, String newValue, String ipAddress) {

        AuditLog log = AuditLog.builder()
                .user(user)
                .project(project)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByProject(UUID projectId, Pageable pageable) {
        return auditLogRepository.findAllByProjectId(projectId, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUser().getId(), log.getUser().getUsername(),
                log.getProject() != null ? log.getProject().getId() : null,
                log.getAction(), log.getEntityType(), log.getEntityId(),
                log.getOldValue(), log.getNewValue(),
                log.getIpAddress(), log.getCreatedAt()
        );
    }
}