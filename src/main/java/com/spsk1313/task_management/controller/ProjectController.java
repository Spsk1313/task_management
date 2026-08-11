package com.spsk1313.task_management.controller;

import com.spsk1313.task_management.dto.CreateProjectRequest;
import com.spsk1313.task_management.dto.ProjectResponse;
import com.spsk1313.task_management.dto.UpdateProjectRequest;
import com.spsk1313.task_management.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Tag(
        name = "Projects",
        description = "Manage projects and project ownership"
)
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects(
            @RequestParam(required = false) Long ownerId
    ) {
        if (ownerId == null) {
            return ResponseEntity.ok(projectService.getProjects());
        }

        return ResponseEntity.ok(
                projectService.getProjectsByOwnerId(ownerId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                projectService.getProjectById(id)
        );
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest req
    ) {
        ProjectResponse response = projectService.createProject(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @RequestParam Long ownerId,
            @Valid @RequestBody UpdateProjectRequest req
    ) {
        ProjectResponse response =
                projectService.updateProject(ownerId, id, req);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @RequestParam Long ownerId
    ) {
        projectService.deleteProject(ownerId, id);

        return ResponseEntity.noContent().build();
    }
}