package com.spsk1313.task_management.controller;

import com.spsk1313.task_management.dto.AddTagRequest;
import com.spsk1313.task_management.dto.TaskResponse;
import com.spsk1313.task_management.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
        name = "Tags",
        description = "Manage task tag associations"
)
public class TagController {

    private final TaskService taskService;

    public TagController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            summary = "Attach a tag to a task",
            description = """
                    Attaches a tag to a task. Tag names are normalized by trimming
                    whitespace and converting them to lowercase. Existing global
                    tags are reused. Attaching the same tag twice to one task
                    results in a conflict.
                    """
    )
    @PostMapping("/api/tasks/{taskId}/tags")
    public ResponseEntity<TaskResponse> addTagToTask(@PathVariable Long taskId, @Valid @RequestBody AddTagRequest req) {
        TaskResponse response = taskService.addTagToTask(taskId, req);
        return ResponseEntity.ok(response);
    }
}
