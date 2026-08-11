package com.spsk1313.task_management.controller;

import com.spsk1313.task_management.dto.CreateTaskRequest;
import com.spsk1313.task_management.dto.TaskResponse;
import com.spsk1313.task_management.dto.UpdateTaskRequest;
import com.spsk1313.task_management.entity.TaskPriority;
import com.spsk1313.task_management.entity.TaskStatus;
import com.spsk1313.task_management.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(
        name = "Tasks",
        description = "Create, query, update, and delete project tasks"
)
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest req
    ) {
        TaskResponse response = taskService.createTask(projectId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List project tasks",
            description = """
                    Returns a paginated list of tasks belonging to a project.
                    Supports filtering by status and priority, case-insensitive
                    title search, pagination, and sorting.
                    """
    )
    @GetMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        Page<TaskResponse> response = taskService.getTasks(projectId, status, priority, search, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update a task",
            description = """
                    Partially updates a task. Task status changes must follow
                    the allowed status-transition rules.
                    """
    )
    @PatchMapping("/api/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest req
    ) {
        TaskResponse response = taskService.updateTask(taskId, req);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
