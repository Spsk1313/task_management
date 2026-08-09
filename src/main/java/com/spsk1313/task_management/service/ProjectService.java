package com.spsk1313.task_management.service;

import com.spsk1313.task_management.dto.CreateProjectRequest;
import com.spsk1313.task_management.dto.ProjectResponse;
import com.spsk1313.task_management.dto.UpdateProjectRequest;
import com.spsk1313.task_management.entity.Project;
import com.spsk1313.task_management.entity.User;
import com.spsk1313.task_management.exception.DuplicateProjectNameException;
import com.spsk1313.task_management.exception.OperationForbiddenException;
import com.spsk1313.task_management.exception.ProjectNotFoundException;
import com.spsk1313.task_management.exception.UserNotFoundException;
import com.spsk1313.task_management.repository.ProjectRepository;
import com.spsk1313.task_management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        return projectRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByOwnerId(Long ownerId) {
        if (!userRepository.existsById(ownerId)) throw new UserNotFoundException(ownerId);
        return projectRepository.findByOwner_Id(ownerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
        return toResponse(project);
    }

    public ProjectResponse createProject(CreateProjectRequest req) {
        Project project = toEntity(req);

        if (projectRepository.existsByOwner_IdAndName(req.ownerId(), project.getName()))
            throw new DuplicateProjectNameException(req.name());

        projectRepository.save(project);

        return toResponse(project);
    }

    public ProjectResponse updateProject(
            Long ownerId,
            Long projectId,
            UpdateProjectRequest req
    ) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        User owner = project.getOwner();

        if (!Objects.equals(owner.getId(), ownerId)) {
            throw new OperationForbiddenException();
        }

        String normalizedName = req.name().trim();

        if (projectRepository.existsByOwner_IdAndNameAndIdNot(
                owner.getId(),
                normalizedName,
                project.getId()
        )) {
            throw new DuplicateProjectNameException(normalizedName);
        }

        project.changeName(normalizedName);
        project.changeDescription(req.description());

        return toResponse(project);
    }

    public void deleteProject(Long ownerId, Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
        User owner = project.getOwner();
        if (!Objects.equals(owner.getId(), ownerId)) throw new OperationForbiddenException();
        projectRepository.delete(project);
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getId()
        );
    }

    private Project toEntity(CreateProjectRequest req) {
        User owner = userRepository.findById(req.ownerId()).orElseThrow(() -> new UserNotFoundException(req.ownerId()));

        return new Project(
                req.name(),
                req.description(),
                owner
        );
    }
}
