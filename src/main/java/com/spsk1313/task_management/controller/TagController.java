package com.spsk1313.task_management.controller;

import com.spsk1313.task_management.dto.AddTagRequest;
import com.spsk1313.task_management.dto.TaskResponse;
import com.spsk1313.task_management.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TagController {

    private final TaskService taskService;

    public TagController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/tasks/{taskId}/tags")
    public ResponseEntity<TaskResponse> addTagToTask(@PathVariable Long taskId, @Valid @RequestBody AddTagRequest req) {
        TaskResponse response = taskService.addTagToTask(taskId, req);
        return ResponseEntity.ok(response);
    }
}
