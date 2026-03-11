package com.bukubesarkami.core.repository;

import com.bukubesarkami.core.entity.UserProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProjectRepository extends JpaRepository<UserProject, UUID> {

    List<UserProject> findAllByUserId(UUID userId);

    List<UserProject> findAllByProjectId(UUID projectId);

    Optional<UserProject> findByUserIdAndProjectId(UUID userId, UUID projectId);

    boolean existsByUserIdAndProjectId(UUID userId, UUID projectId);

    @Query("SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId")
    List<UUID> findProjectIdsByUserId(@Param("userId") UUID userId);
}