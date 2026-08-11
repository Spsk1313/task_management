package com.spsk1313.task_management.service;


import com.spsk1313.task_management.dto.AddTagRequest;
import com.spsk1313.task_management.dto.CreateTaskRequest;
import com.spsk1313.task_management.dto.TaskResponse;
import com.spsk1313.task_management.dto.UpdateTaskRequest;
import com.spsk1313.task_management.entity.*;
import com.spsk1313.task_management.exception.DuplicateTaskTagException;
import com.spsk1313.task_management.exception.ProjectNotFoundException;
import com.spsk1313.task_management.exception.TaskNotFoundException;
import com.spsk1313.task_management.repository.ProjectRepository;
import com.spsk1313.task_management.repository.TagRepository;
import com.spsk1313.task_management.repository.TaskRepository;
import com.spsk1313.task_management.repository.TaskSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, TagRepository tagRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.tagRepository = tagRepository;
    }

    public TaskResponse createTask(Long projectId, CreateTaskRequest req) {
        Task task = toEntity(projectId, req);

        taskRepository.save(task);

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(Long projectId, TaskStatus status, TaskPriority priority, String search, Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }
        Specification<Task> spec = Specification.where(TaskSpecifications.belongsToProject(projectId))
                .and(TaskSpecifications.hasStatus(status))
                .and(TaskSpecifications.hasPriority(priority))
                .and(TaskSpecifications.searchByTitle(search));

        return taskRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public TaskResponse updateTask(
            Long taskId,
            UpdateTaskRequest req
    ) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        if (req.title() != null) task.changeTitle(req.title());
        if (req.description() != null) task.changeDescription(req.description());
        if (req.priority() != null) task.changePriority(req.priority());
        if (req.status() != null) task.changeStatus(req.status());
        if (req.dueDate() != null) task.changeDueDate(req.dueDate());

        return toResponse(task);
    }

    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        taskRepository.delete(task);
    }

    public TaskResponse addTagToTask(Long taskId, AddTagRequest req) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));

        String normalizedName = req.name().trim().toLowerCase(Locale.ROOT);

        Tag tag = tagRepository
                .findByName(normalizedName)
                .orElseGet(() -> tagRepository.save(new Tag(normalizedName)));

        if (task.hasTag(tag)) {
            throw new DuplicateTaskTagException(taskId, normalizedName);
        }

        task.addTag(tag);

        return toResponse(task);
    }

    private Task toEntity(Long projectId, CreateTaskRequest req) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
        return new Task(
                req.title(),
                req.description(),
                project,
                req.priority(),
                req.dueDate()
        );
    }


    private TaskResponse toResponse(Task task) {

        Set<String> tags = task
                .getTags()
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getProject().getId(),
                task.getPriority(),
                task.getStatus(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt(),
                tags
        );
    }
}
