package com.spsk1313.task_management.controller;

import com.spsk1313.task_management.dto.CreateTaskRequest;
import com.spsk1313.task_management.dto.TaskResponse;
import com.spsk1313.task_management.dto.UpdateTaskRequest;
import com.spsk1313.task_management.entity.TaskPriority;
import com.spsk1313.task_management.entity.TaskStatus;
import com.spsk1313.task_management.exception.InvalidTaskStatusTransitionException;
import com.spsk1313.task_management.exception.ProjectNotFoundException;
import com.spsk1313.task_management.exception.TaskNotFoundException;
import com.spsk1313.task_management.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerTest {

    private TaskService taskService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        taskService = mock(TaskService.class);

        TaskController controller = new TaskController(taskService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    // ----------------------------------------------------------------
    // POST /api/projects/{projectId}/tasks
    // ----------------------------------------------------------------

    @Test
    void createTask_shouldReturn201AndCreatedTask_whenRequestIsValid()
            throws Exception {

        when(taskService.createTask(
                eq(10L),
                any(CreateTaskRequest.class)
        )).thenReturn(taskResponse());

        String json = """
                {
                  "title": "Build API",
                  "description": "Implement task API",
                  "priority": "HIGH",
                  "dueDate": "2026-08-20"
                }
                """;

        mockMvc.perform(
                        post("/api/projects/{projectId}/tasks", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Build API"))
                .andExpect(jsonPath("$.description")
                        .value("Implement task API"))
                .andExpect(jsonPath("$.projectId").value(10))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate")
                        .value("2026-08-20"));

        ArgumentCaptor<CreateTaskRequest> captor =
                ArgumentCaptor.forClass(CreateTaskRequest.class);

        verify(taskService)
                .createTask(eq(10L), captor.capture());

        CreateTaskRequest request = captor.getValue();

        assertEquals("Build API", request.title());
        assertEquals(
                "Implement task API",
                request.description()
        );
        assertEquals(TaskPriority.HIGH, request.priority());
        assertEquals(
                LocalDate.of(2026, 8, 20),
                request.dueDate()
        );
    }

    @Test
    void createTask_shouldReturn400_whenTitleIsBlank()
            throws Exception {

        String json = """
                {
                  "title": "",
                  "description": "Description",
                  "priority": "HIGH"
                }
                """;

        mockMvc.perform(
                        post("/api/projects/{projectId}/tasks", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"));

        verifyNoInteractions(taskService);
    }

    @Test
    void createTask_shouldReturn400_whenPriorityIsMissing()
            throws Exception {

        String json = """
                {
                  "title": "Build API",
                  "description": "Description"
                }
                """;

        mockMvc.perform(
                        post("/api/projects/{projectId}/tasks", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void createTask_shouldReturn400_whenPriorityIsInvalid()
            throws Exception {

        String json = """
                {
                  "title": "Build API",
                  "priority": "URGENT"
                }
                """;

        mockMvc.perform(
                        post("/api/projects/{projectId}/tasks", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void createTask_shouldReturn404_whenProjectDoesNotExist()
            throws Exception {

        when(taskService.createTask(
                eq(999L),
                any(CreateTaskRequest.class)
        )).thenThrow(
                new ProjectNotFoundException(999L)
        );

        String json = """
                {
                  "title": "Build API",
                  "priority": "HIGH"
                }
                """;

        mockMvc.perform(
                        post("/api/projects/{projectId}/tasks", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"));
    }

    // ----------------------------------------------------------------
    // GET /api/projects/{projectId}/tasks
    // ----------------------------------------------------------------

    @Test
    void getTasks_shouldReturn200AndPageOfTasks()
            throws Exception {

        Pageable pageable = PageRequest.of(0, 20);

        Page<TaskResponse> page = new PageImpl<>(
                List.of(taskResponse()),
                pageable,
                1
        );

        when(taskService.getTasks(
                eq(10L),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/projects/{projectId}/tasks", 10L)
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Build API"))
                .andExpect(jsonPath("$.content[0].priority")
                        .value("HIGH"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("TODO"))

                // Standalone MockMvc serializes PageImpl directly.
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void getTasks_shouldPassFiltersAndPaginationToService()
            throws Exception {

        when(taskService.getTasks(
                eq(10L),
                eq(TaskStatus.TODO),
                eq(TaskPriority.HIGH),
                eq("spring"),
                any(Pageable.class)
        )).thenReturn(emptyTaskPage());

        mockMvc.perform(
                        get("/api/projects/{projectId}/tasks", 10L)
                                .param("status", "TODO")
                                .param("priority", "HIGH")
                                .param("search", "spring")
                                .param("page", "2")
                                .param("size", "10")
                                .param("sort", "dueDate,desc")
                )
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(taskService).getTasks(
                eq(10L),
                eq(TaskStatus.TODO),
                eq(TaskPriority.HIGH),
                eq("spring"),
                captor.capture()
        );

        Pageable pageable = captor.getValue();

        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());

        Sort.Order dueDate =
                pageable.getSort().getOrderFor("dueDate");

        assertNotNull(dueDate);
        assertEquals(
                Sort.Direction.DESC,
                dueDate.getDirection()
        );
    }

    @Test
    void getTasks_shouldSupportMultipleSortFields()
            throws Exception {

        when(taskService.getTasks(
                eq(10L),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(emptyTaskPage());

        mockMvc.perform(
                        get("/api/projects/{projectId}/tasks", 10L)
                                .param("sort", "priority,desc")
                                .param("sort", "dueDate,asc")
                )
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(taskService).getTasks(
                eq(10L),
                isNull(),
                isNull(),
                isNull(),
                captor.capture()
        );

        Pageable pageable = captor.getValue();

        Sort.Order priority =
                pageable.getSort().getOrderFor("priority");

        Sort.Order dueDate =
                pageable.getSort().getOrderFor("dueDate");

        assertNotNull(priority);
        assertNotNull(dueDate);

        assertEquals(
                Sort.Direction.DESC,
                priority.getDirection()
        );

        assertEquals(
                Sort.Direction.ASC,
                dueDate.getDirection()
        );
    }

    @Test
    void getTasks_shouldReturn400_whenStatusIsInvalid()
            throws Exception {

        mockMvc.perform(
                        get("/api/projects/{projectId}/tasks", 10L)
                                .param("status", "FINISHED")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void getTasks_shouldReturn400_whenPriorityIsInvalid()
            throws Exception {

        mockMvc.perform(
                        get("/api/projects/{projectId}/tasks", 10L)
                                .param("priority", "URGENT")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void getTasks_shouldReturn404_whenProjectDoesNotExist()
            throws Exception {

        when(taskService.getTasks(
                eq(999L),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenThrow(
                new ProjectNotFoundException(999L)
        );

        mockMvc.perform(
                        get("/api/projects/{projectId}/tasks", 999L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"));
    }

    // ----------------------------------------------------------------
    // PATCH /api/tasks/{taskId}
    // ----------------------------------------------------------------

    @Test
    void updateTask_shouldReturn200AndUpdatedTask_whenRequestIsValid()
            throws Exception {

        TaskResponse response = new TaskResponse(
                1L,
                "Updated Task",
                "Updated description",
                10L,
                TaskPriority.MEDIUM,
                TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 8, 25),
                null,
                null,
                null,
                Set.of()
        );

        when(taskService.updateTask(
                eq(1L),
                any(UpdateTaskRequest.class)
        )).thenReturn(response);

        String json = """
                {
                  "title": "Updated Task",
                  "description": "Updated description",
                  "priority": "MEDIUM",
                  "status": "IN_PROGRESS",
                  "dueDate": "2026-08-25"
                }
                """;

        mockMvc.perform(
                        patch("/api/tasks/{taskId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title")
                        .value("Updated Task"))
                .andExpect(jsonPath("$.description")
                        .value("Updated description"))
                .andExpect(jsonPath("$.priority")
                        .value("MEDIUM"))
                .andExpect(jsonPath("$.status")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.dueDate")
                        .value("2026-08-25"));
    }

    @Test
    void updateTask_shouldBindPartialRequestCorrectly()
            throws Exception {

        when(taskService.updateTask(
                eq(1L),
                any(UpdateTaskRequest.class)
        )).thenReturn(taskResponse());

        String json = """
                {
                  "status": "DONE"
                }
                """;

        mockMvc.perform(
                        patch("/api/tasks/{taskId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateTaskRequest> captor =
                ArgumentCaptor.forClass(UpdateTaskRequest.class);

        verify(taskService)
                .updateTask(eq(1L), captor.capture());

        UpdateTaskRequest request = captor.getValue();

        assertEquals(TaskStatus.DONE, request.status());
        assertNull(request.title());
        assertNull(request.description());
        assertNull(request.priority());
        assertNull(request.dueDate());
    }

    @Test
    void updateTask_shouldReturn400_whenTitleExceedsMaximumLength()
            throws Exception {

        String json = """
                {
                  "title": "%s"
                }
                """.formatted("x".repeat(256));

        mockMvc.perform(
                        patch("/api/tasks/{taskId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void updateTask_shouldReturn400_whenStatusIsInvalid()
            throws Exception {

        String json = """
                {
                  "status": "FINISHED"
                }
                """;

        mockMvc.perform(
                        patch("/api/tasks/{taskId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void updateTask_shouldReturn404_whenTaskDoesNotExist()
            throws Exception {

        when(taskService.updateTask(
                eq(999L),
                any(UpdateTaskRequest.class)
        )).thenThrow(
                new TaskNotFoundException(999L)
        );

        String json = """
                {
                  "title": "Updated"
                }
                """;

        mockMvc.perform(
                        patch("/api/tasks/{taskId}", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"));
    }

    @Test
    void updateTask_shouldReturn409_whenStatusTransitionIsInvalid()
            throws Exception {

        when(taskService.updateTask(
                eq(1L),
                any(UpdateTaskRequest.class)
        )).thenThrow(
                new InvalidTaskStatusTransitionException(
                        TaskStatus.BLOCKED,
                        TaskStatus.DONE
                )
        );

        String json = """
                {
                  "status": "DONE"
                }
                """;

        mockMvc.perform(
                        patch("/api/tasks/{taskId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Cannot transition from BLOCKED to DONE"
                        ));
    }

    // ----------------------------------------------------------------
    // DELETE /api/tasks/{taskId}
    // ----------------------------------------------------------------

    @Test
    void deleteTask_shouldReturn204_whenTaskExists()
            throws Exception {

        mockMvc.perform(
                        delete("/api/tasks/{taskId}", 1L)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(taskService).deleteTask(1L);
    }

    @Test
    void deleteTask_shouldReturn404_whenTaskDoesNotExist()
            throws Exception {

        doThrow(new TaskNotFoundException(999L))
                .when(taskService)
                .deleteTask(999L);

        mockMvc.perform(
                        delete("/api/tasks/{taskId}", 999L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private TaskResponse taskResponse() {
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
                Set.of()
        );
    }

    private Page<TaskResponse> emptyTaskPage() {
        return new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0
        );
    }
}