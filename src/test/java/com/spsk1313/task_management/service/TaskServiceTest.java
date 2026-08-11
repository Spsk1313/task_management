package com.spsk1313.task_management.service;

import com.spsk1313.task_management.dto.AddTagRequest;
import com.spsk1313.task_management.dto.CreateTaskRequest;
import com.spsk1313.task_management.dto.TaskResponse;
import com.spsk1313.task_management.dto.UpdateTaskRequest;
import com.spsk1313.task_management.entity.*;
import com.spsk1313.task_management.exception.DuplicateTaskTagException;
import com.spsk1313.task_management.exception.InvalidTaskStatusTransitionException;
import com.spsk1313.task_management.exception.ProjectNotFoundException;
import com.spsk1313.task_management.exception.TaskNotFoundException;
import com.spsk1313.task_management.repository.ProjectRepository;
import com.spsk1313.task_management.repository.TagRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private TagRepository tagRepository;

    private TaskService taskService;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        projectRepository = mock(ProjectRepository.class);
        tagRepository = mock(TagRepository.class);

        taskService = new TaskService(
                taskRepository,
                projectRepository,
                tagRepository
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

    // ----------------------------------------------------------------
    // CREATE TASK
    // ----------------------------------------------------------------

    @Test
    void createTask_shouldCreateTask_whenProjectExists() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Build API",
                "Implement task API",
                TaskPriority.HIGH,
                LocalDate.of(2026, 8, 20)
        );

        when(projectRepository.findById(1L))
                .thenReturn(Optional.of(project));

        TaskResponse response =
                taskService.createTask(1L, request);

        assertEquals("Build API", response.title());
        assertEquals(
                "Implement task API",
                response.description()
        );
        assertEquals(TaskPriority.HIGH, response.priority());
        assertEquals(TaskStatus.TODO, response.status());
        assertEquals(
                LocalDate.of(2026, 8, 20),
                response.dueDate()
        );
        assertNull(response.completedAt());
        assertTrue(response.tags().isEmpty());

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_shouldThrow_whenProjectDoesNotExist() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Build API",
                "Description",
                TaskPriority.HIGH,
                null
        );

        when(projectRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> taskService.createTask(999L, request)
        );

        verify(taskRepository, never())
                .save(any(Task.class));
    }

    // ----------------------------------------------------------------
    // UPDATE TASK
    // ----------------------------------------------------------------

    @Test
    void updateTask_shouldUpdateProvidedFields() {
        Task task = new Task(
                "Old title",
                "Old description",
                project,
                TaskPriority.LOW,
                null
        );

        UpdateTaskRequest request = new UpdateTaskRequest(
                "New title",
                "New description",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 8, 25)
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        TaskResponse response =
                taskService.updateTask(1L, request);

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

        UpdateTaskRequest request = new UpdateTaskRequest(
                null,
                null,
                TaskPriority.HIGH,
                null,
                null
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        TaskResponse response =
                taskService.updateTask(1L, request);

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
        UpdateTaskRequest request = new UpdateTaskRequest(
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
                () -> taskService.updateTask(999L, request)
        );
    }

    @Test
    void updateTask_shouldSetCompletedAt_whenTaskBecomesDone() {
        Task task = createTask();

        UpdateTaskRequest request = new UpdateTaskRequest(
                null,
                null,
                null,
                TaskStatus.DONE,
                null
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        TaskResponse response =
                taskService.updateTask(1L, request);

        assertEquals(TaskStatus.DONE, response.status());
        assertNotNull(response.completedAt());
    }

    @Test
    void updateTask_shouldClearCompletedAt_whenDoneTaskIsReopened() {
        Task task = createTask();

        task.changeStatus(TaskStatus.DONE);

        assertNotNull(task.getCompletedAt());

        UpdateTaskRequest request = new UpdateTaskRequest(
                null,
                null,
                null,
                TaskStatus.IN_PROGRESS,
                null
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        TaskResponse response =
                taskService.updateTask(1L, request);

        assertEquals(
                TaskStatus.IN_PROGRESS,
                response.status()
        );
        assertNull(response.completedAt());
    }

    @Test
    void updateTask_shouldThrow_whenStatusTransitionIsInvalid() {
        Task task = createTask();

        task.changeStatus(TaskStatus.IN_PROGRESS);
        task.changeStatus(TaskStatus.BLOCKED);

        UpdateTaskRequest request = new UpdateTaskRequest(
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
                () -> taskService.updateTask(1L, request)
        );

        assertEquals(
                TaskStatus.BLOCKED,
                task.getStatus()
        );
        assertNull(task.getCompletedAt());
    }

    // ----------------------------------------------------------------
    // DELETE TASK
    // ----------------------------------------------------------------

    @Test
    void deleteTask_shouldDeleteTask_whenTaskExists() {
        Task task = createTask();

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

        verify(taskRepository, never())
                .delete(any(Task.class));
    }

    // ----------------------------------------------------------------
    // GET TASKS
    // ----------------------------------------------------------------

    @Test
    void getTasks_shouldReturnPageOfTasks() {
        Task task = new Task(
                "Build API",
                "Description",
                project,
                TaskPriority.HIGH,
                null
        );

        Pageable pageable =
                PageRequest.of(0, 10);

        Page<Task> page = new PageImpl<>(
                List.of(task),
                pageable,
                1
        );

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

        assertEquals(
                "Build API",
                taskResponse.title()
        );
        assertEquals(
                TaskPriority.HIGH,
                taskResponse.priority()
        );
        assertEquals(
                TaskStatus.TODO,
                taskResponse.status()
        );
    }

    @Test
    void getTasks_shouldPassSpecificationAndPageableToRepository() {
        Pageable pageable =
                PageRequest.of(0, 20);

        when(projectRepository.existsById(1L))
                .thenReturn(true);

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(
                new PageImpl<>(
                        List.of(),
                        pageable,
                        0
                )
        );

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
        Pageable pageable =
                PageRequest.of(0, 10);

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

    // ----------------------------------------------------------------
    // TAGS
    // ----------------------------------------------------------------

    @Test
    void addTagToTask_shouldCreateTag_whenTagDoesNotExist() {
        Task task = createTask();

        AddTagRequest request =
                new AddTagRequest(" JAVA ");

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        when(tagRepository.findByName("java"))
                .thenReturn(Optional.empty());

        when(tagRepository.save(any(Tag.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        TaskResponse response =
                taskService.addTagToTask(1L, request);

        verify(tagRepository)
                .findByName("java");

        verify(tagRepository)
                .save(any(Tag.class));

        assertEquals(1, response.tags().size());
        assertTrue(response.tags().contains("java"));
        assertEquals(1, task.getTags().size());
    }

    @Test
    void addTagToTask_shouldReuseExistingTag_whenTagExistsGlobally() {
        Task task = createTask();

        Tag existingTag = new Tag("java");

        AddTagRequest request =
                new AddTagRequest(" JAVA ");

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        when(tagRepository.findByName("java"))
                .thenReturn(Optional.of(existingTag));

        TaskResponse response =
                taskService.addTagToTask(1L, request);

        verify(tagRepository)
                .findByName("java");

        verify(tagRepository, never())
                .save(any(Tag.class));

        assertEquals(1, response.tags().size());
        assertTrue(response.tags().contains("java"));
        assertTrue(task.getTags().contains(existingTag));
    }

    @Test
    void addTagToTask_shouldNormalizeTagNameBeforeLookup() {
        Task task = createTask();

        AddTagRequest request =
                new AddTagRequest("   JaVa   ");

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        when(tagRepository.findByName("java"))
                .thenReturn(Optional.empty());

        when(tagRepository.save(any(Tag.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        TaskResponse response =
                taskService.addTagToTask(1L, request);

        verify(tagRepository)
                .findByName("java");

        assertTrue(response.tags().contains("java"));
    }

    @Test
    void addTagToTask_shouldThrow_whenTaskDoesNotExist() {
        AddTagRequest request =
                new AddTagRequest("java");

        when(taskRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskService.addTagToTask(
                        999L,
                        request
                )
        );

        verifyNoInteractions(tagRepository);
    }

    @Test
    void addTagToTask_shouldThrow_whenTagAlreadyAttachedToTask() {
        Task task = createTask();

        Tag existingTag = new Tag("java");

        /*
         * Attach the canonical Tag first.
         */
        task.addTag(existingTag);

        AddTagRequest request =
                new AddTagRequest(" JAVA ");

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        when(tagRepository.findByName("java"))
                .thenReturn(Optional.of(existingTag));

        assertThrows(
                DuplicateTaskTagException.class,
                () -> taskService.addTagToTask(
                        1L,
                        request
                )
        );

        assertEquals(1, task.getTags().size());

        verify(tagRepository, never())
                .save(any(Tag.class));
    }

    @Test
    void addTagToTask_shouldAllowDifferentTagsOnSameTask() {
        Task task = createTask();

        Tag javaTag = new Tag("java");
        Tag springTag = new Tag("spring");

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        when(tagRepository.findByName("java"))
                .thenReturn(Optional.of(javaTag));

        when(tagRepository.findByName("spring"))
                .thenReturn(Optional.of(springTag));

        taskService.addTagToTask(
                1L,
                new AddTagRequest("java")
        );

        TaskResponse response =
                taskService.addTagToTask(
                        1L,
                        new AddTagRequest("spring")
                );

        assertEquals(2, response.tags().size());
        assertTrue(response.tags().contains("java"));
        assertTrue(response.tags().contains("spring"));
    }

    // ----------------------------------------------------------------
    // HELPERS
    // ----------------------------------------------------------------

    private Task createTask() {
        return new Task(
                "Task",
                "Description",
                project,
                TaskPriority.HIGH,
                null
        );
    }
}