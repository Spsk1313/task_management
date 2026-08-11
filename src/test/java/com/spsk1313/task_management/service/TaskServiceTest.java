package com.spsk1313.task_management.service;

import com.spsk1313.task_management.dto.CreateTaskRequest;
import com.spsk1313.task_management.dto.TaskResponse;
import com.spsk1313.task_management.dto.UpdateTaskRequest;
import com.spsk1313.task_management.entity.*;
import com.spsk1313.task_management.exception.InvalidTaskStatusTransitionException;
import com.spsk1313.task_management.exception.ProjectNotFoundException;
import com.spsk1313.task_management.exception.TaskNotFoundException;
import com.spsk1313.task_management.repository.ProjectRepository;
import com.spsk1313.task_management.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private TaskService taskService;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        projectRepository = mock(ProjectRepository.class);

        taskService = new TaskService(
                taskRepository,
                projectRepository
        );

        owner = new User(
                "Sahil",
                "sahil@example.com"
        );

        project = new Project(
                "Backend Project",
                "Description",
                owner
        );
    }

    @Test
    void createTask_shouldCreateTask_whenProjectExists() {
        CreateTaskRequest req = new CreateTaskRequest(
                "Build API",
                "Implement task API",
                TaskPriority.HIGH,
                LocalDate.of(2026, 8, 20)
        );

        when(projectRepository.findById(1L))
                .thenReturn(Optional.of(project));

        TaskResponse response =
                taskService.createTask(1L, req);

        assertEquals("Build API", response.title());
        assertEquals(
                "Implement task API",
                response.description()
        );
        assertEquals(TaskPriority.HIGH, response.priority());
        assertEquals(TaskStatus.TODO, response.status());
        assertNull(response.completedAt());

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_shouldThrow_whenProjectDoesNotExist() {
        CreateTaskRequest req = new CreateTaskRequest(
                "Build API",
                "Description",
                TaskPriority.HIGH,
                null
        );

        when(projectRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> taskService.createTask(999L, req)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTask_shouldUpdateProvidedFields() {
        Task task = new Task(
                "Old title",
                "Old description",
                project,
                TaskPriority.LOW,
                null
        );

        UpdateTaskRequest req = new UpdateTaskRequest(
                "New title",
                "New description",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 8, 25)
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        TaskResponse response =
                taskService.updateTask(1L, req);

        assertEquals("New title", response.title());
        assertEquals(
                "New description",
                response.description()
        );
        assertEquals(TaskPriority.HIGH, response.priority());
        assertEquals(
                TaskStatus.IN_PROGRESS,
                response.status()
        );
        assertEquals(
                LocalDate.of(2026, 8, 25),
                response.dueDate()
        );
    }

    @Test
    void updateTask_shouldLeaveUnspecifiedFieldsUnchanged() {
        LocalDate originalDueDate =
                LocalDate.of(2026, 8, 20);

        Task task = new Task(
                "Original title",
                "Original description",
                project,
                TaskPriority.MEDIUM,
                originalDueDate
        );

        UpdateTaskRequest req = new UpdateTaskRequest(
                null,
                null,
                TaskPriority.HIGH,
                null,
                null
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        TaskResponse response =
                taskService.updateTask(1L, req);

        assertEquals(
                "Original title",
                response.title()
        );
        assertEquals(
                "Original description",
                response.description()
        );
        assertEquals(TaskPriority.HIGH, response.priority());
        assertEquals(TaskStatus.TODO, response.status());
        assertEquals(originalDueDate, response.dueDate());
    }

    @Test
    void updateTask_shouldThrow_whenTaskDoesNotExist() {
        UpdateTaskRequest req = new UpdateTaskRequest(
                "New title",
                null,
                null,
                null,
                null
        );

        when(taskRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskService.updateTask(999L, req)
        );
    }

    @Test
    void updateTask_shouldSetCompletedAt_whenTaskBecomesDone() {
        Task task = new Task(
                "Task",
                null,
                project,
                TaskPriority.HIGH,
                null
        );

        UpdateTaskRequest req = new UpdateTaskRequest(
                null,
                null,
                null,
                TaskStatus.DONE,
                null
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        TaskResponse response =
                taskService.updateTask(1L, req);

        assertEquals(TaskStatus.DONE, response.status());
        assertNotNull(response.completedAt());
    }

    @Test
    void updateTask_shouldClearCompletedAt_whenDoneTaskIsReopened() {
        Task task = new Task(
                "Task",
                null,
                project,
                TaskPriority.HIGH,
                null
        );

        task.changeStatus(TaskStatus.DONE);

        UpdateTaskRequest req = new UpdateTaskRequest(
                null,
                null,
                null,
                TaskStatus.IN_PROGRESS,
                null
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        TaskResponse response =
                taskService.updateTask(1L, req);

        assertEquals(
                TaskStatus.IN_PROGRESS,
                response.status()
        );
        assertNull(response.completedAt());
    }

    @Test
    void updateTask_shouldThrow_whenStatusTransitionIsInvalid() {
        Task task = new Task(
                "Task",
                null,
                project,
                TaskPriority.HIGH,
                null
        );

        task.changeStatus(TaskStatus.IN_PROGRESS);
        task.changeStatus(TaskStatus.BLOCKED);

        UpdateTaskRequest req = new UpdateTaskRequest(
                null,
                null,
                null,
                TaskStatus.DONE,
                null
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        assertThrows(
                InvalidTaskStatusTransitionException.class,
                () -> taskService.updateTask(1L, req)
        );

        assertEquals(TaskStatus.BLOCKED, task.getStatus());
    }

    @Test
    void deleteTask_shouldDeleteTask_whenTaskExists() {
        Task task = new Task(
                "Task",
                null,
                project,
                TaskPriority.HIGH,
                null
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        taskService.deleteTask(1L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_shouldThrow_whenTaskDoesNotExist() {
        when(taskRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskService.deleteTask(999L)
        );

        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void getTasks_shouldReturnPageOfTasks() {
        Task task = new Task(
                "Build API",
                "Description",
                project,
                TaskPriority.HIGH,
                null
        );

        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> page =
                new PageImpl<>(List.of(task), pageable, 1);

        when(projectRepository.existsById(1L))
                .thenReturn(true);

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(page);

        Page<TaskResponse> response =
                taskService.getTasks(
                        1L,
                        null,
                        null,
                        null,
                        pageable
                );

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());

        TaskResponse taskResponse =
                response.getContent().getFirst();

        assertEquals("Build API", taskResponse.title());
        assertEquals(TaskPriority.HIGH, taskResponse.priority());
        assertEquals(TaskStatus.TODO, taskResponse.status());
    }

    @Test
    void getTasks_shouldPassSpecificationAndPageableToRepository() {
        Pageable pageable = PageRequest.of(0, 20);

        when(projectRepository.existsById(1L))
                .thenReturn(true);

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(Page.empty(pageable));

        taskService.getTasks(
                1L,
                TaskStatus.TODO,
                TaskPriority.HIGH,
                "spring",
                pageable
        );

        verify(taskRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
    }

    @Test
    void getTasks_shouldThrow_whenProjectDoesNotExist() {
        Pageable pageable = PageRequest.of(0, 10);

        when(projectRepository.existsById(999L))
                .thenReturn(false);

        assertThrows(
                ProjectNotFoundException.class,
                () -> taskService.getTasks(
                        999L,
                        null,
                        null,
                        null,
                        pageable
                )
        );

        verify(taskRepository, never())
                .findAll(
                        any(Specification.class),
                        any(Pageable.class)
                );
    }
}