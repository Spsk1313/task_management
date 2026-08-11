package com.spsk1313.task_management.controller;

import com.spsk1313.task_management.dto.AddTagRequest;
import com.spsk1313.task_management.dto.TaskResponse;
import com.spsk1313.task_management.entity.TaskPriority;
import com.spsk1313.task_management.entity.TaskStatus;
import com.spsk1313.task_management.exception.DuplicateTaskTagException;
import com.spsk1313.task_management.exception.TaskNotFoundException;
import com.spsk1313.task_management.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TagControllerTest {

    private TaskService taskService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        taskService = mock(TaskService.class);

        TagController controller =
                new TagController(taskService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    // ----------------------------------------------------------------
    // POST /api/tasks/{taskId}/tags
    // ----------------------------------------------------------------

    @Test
    void addTagToTask_shouldReturn200AndUpdatedTask_whenRequestIsValid()
            throws Exception {

        TaskResponse response = taskResponse(
                Set.of("java")
        );

        when(taskService.addTagToTask(
                eq(1L),
                any(AddTagRequest.class)
        )).thenReturn(response);

        String json = """
                {
                  "name": "java"
                }
                """;

        mockMvc.perform(
                        post("/api/tasks/{taskId}/tags", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title")
                        .value("Build API"))
                .andExpect(jsonPath("$.projectId")
                        .value(10))
                .andExpect(jsonPath("$.tags.length()")
                        .value(1))
                .andExpect(jsonPath("$.tags[0]")
                        .value("java"));

        ArgumentCaptor<AddTagRequest> captor =
                ArgumentCaptor.forClass(
                        AddTagRequest.class
                );

        verify(taskService).addTagToTask(
                eq(1L),
                captor.capture()
        );

        assertEquals(
                "java",
                captor.getValue().name()
        );
    }

    @Test
    void addTagToTask_shouldReturn400_whenNameIsBlank()
            throws Exception {

        String json = """
                {
                  "name": ""
                }
                """;

        mockMvc.perform(
                        post("/api/tasks/{taskId}/tags", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"));

        verifyNoInteractions(taskService);
    }

    @Test
    void addTagToTask_shouldReturn400_whenNameExceedsMaximumLength()
            throws Exception {

        String json = """
                {
                  "name": "%s"
                }
                """.formatted("x".repeat(51));

        mockMvc.perform(
                        post("/api/tasks/{taskId}/tags", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"));

        verifyNoInteractions(taskService);
    }

    @Test
    void addTagToTask_shouldReturn404_whenTaskDoesNotExist()
            throws Exception {

        when(taskService.addTagToTask(
                eq(999L),
                any(AddTagRequest.class)
        )).thenThrow(
                new TaskNotFoundException(999L)
        );

        String json = """
                {
                  "name": "java"
                }
                """;

        mockMvc.perform(
                        post("/api/tasks/{taskId}/tags", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Task with id 999 not found"
                        ));
    }

    @Test
    void addTagToTask_shouldReturn409_whenTagAlreadyAttached()
            throws Exception {

        when(taskService.addTagToTask(
                eq(1L),
                any(AddTagRequest.class)
        )).thenThrow(
                new DuplicateTaskTagException(
                        1L,
                        "java"
                )
        );

        String json = """
                {
                  "name": "java"
                }
                """;

        mockMvc.perform(
                        post("/api/tasks/{taskId}/tags", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Tag \"java\" is already attached to task 1"
                        ));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private TaskResponse taskResponse(
            Set<String> tags
    ) {
        return new TaskResponse(
                1L,
                "Build API",
                "Implement task API",
                10L,
                TaskPriority.HIGH,
                TaskStatus.TODO,
                LocalDate.of(2026, 8, 20),
                null,
                null,
                null,
                tags
        );
    }
}