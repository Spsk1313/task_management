package com.spsk1313.task_management.repository;

import com.spsk1313.task_management.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwner_Id(Long ownerId);

    boolean existsByOwner_IdAndName(Long ownerId, String name);

    boolean existsByOwner_IdAndNameAndIdNot(Long ownerId, String name, Long projectId);
}
