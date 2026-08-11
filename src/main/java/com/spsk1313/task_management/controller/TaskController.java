package com.spsk1313.task_management.controller;

import com.spsk1313.task_management.dto.CreateTaskRequest;
import com.spsk1313.task_management.dto.TaskResponse;
import com.spsk1313.task_management.dto.UpdateTaskRequest;
import com.spsk1313.task_management.entity.TaskPriority;
import com.spsk1313.task_management.entity.TaskStatus;
import com.spsk1313.task_management.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
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
