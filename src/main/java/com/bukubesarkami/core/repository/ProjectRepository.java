package com.bukubesarkami.core.repository;

import com.bukubesarkami.core.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByProjectCode(String projectCode);

    boolean existsByProjectCode(String projectCode);

    Page<Project> findAllByActiveTrue(Pageable pageable);
}