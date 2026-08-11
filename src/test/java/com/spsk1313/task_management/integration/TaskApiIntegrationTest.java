package com.spsk1313.task_management.integration;

import com.spsk1313.task_management.entity.*;
import com.spsk1313.task_management.repository.ProjectRepository;
import com.spsk1313.task_management.repository.TaskRepository;
import com.spsk1313.task_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    private Project project;

    @BeforeEach
    void setUp() {
        /*
         * Delete children before parents because:
         *
         * tasks -> projects -> users
         */
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(
                new User(
                        "Sahil",
                        "sahil@example.com"
                )
        );

        project = projectRepository.save(
                new Project(
                        "Backend Project",
                        "Spring Boot project",
                        owner
                )
        );
    }

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    @Test
    void createTask_shouldPersistTask_andReturn201()
            throws Exception {

        String json = """
                {
                  "title": "Build API",
                  "description": "Implement task endpoints",
                  "priority": "HIGH",
                  "dueDate": "2026-08-20"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/projects/{projectId}/tasks",
                                project.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title")
                        .value("Build API"))
                .andExpect(jsonPath("$.description")
                        .value("Implement task endpoints"))
                .andExpect(jsonPath("$.projectId")
                        .value(project.getId()))
                .andExpect(jsonPath("$.priority")
                        .value("HIGH"))
                .andExpect(jsonPath("$.status")
                        .value("TODO"))
                .andExpect(jsonPath("$.dueDate")
                        .value("2026-08-20"));

        List<Task> tasks = taskRepository.findAll();

        assertEquals(1, tasks.size());

        Task savedTask = tasks.getFirst();

        assertNotNull(savedTask.getId());

        assertEquals(
                "Build API",
                savedTask.getTitle()
        );

        assertEquals(
                "Implement task endpoints",
                savedTask.getDescription()
        );

        assertEquals(
                project.getId(),
                savedTask.getProject().getId()
        );

        assertEquals(
                TaskPriority.HIGH,
                savedTask.getPriority()
        );

        /*
         * New tasks must ALWAYS start TODO.
         */
        assertEquals(
                TaskStatus.TODO,
                savedTask.getStatus()
        );

        assertEquals(
                LocalDate.of(2026, 8, 20),
                savedTask.getDueDate()
        );

        assertNotNull(savedTask.getCreatedAt());
        assertNotNull(savedTask.getUpdatedAt());
        assertNull(savedTask.getCompletedAt());
    }

    @Test
    void createTask_shouldReturn404_whenProjectDoesNotExist()
            throws Exception {

        String json = """
                {
                  "title": "Build API",
                  "priority": "HIGH"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/projects/{projectId}/tasks",
                                999999L
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        assertEquals(0, taskRepository.count());
    }

    // ------------------------------------------------------------
    // GET / FILTERING
    // ------------------------------------------------------------

    @Test
    void getTasks_shouldReturnOnlyTasksForRequestedProject()
            throws Exception {

        Project otherProject = createProject(
                "Other Project"
        );

        createTask(
                project,
                "Task A",
                TaskPriority.HIGH,
                LocalDate.of(2026, 8, 20)
        );

        createTask(
                project,
                "Task B",
                TaskPriority.LOW,
                LocalDate.of(2026, 8, 25)
        );

        createTask(
                otherProject,
                "Should Not Appear",
                TaskPriority.HIGH,
                LocalDate.of(2026, 8, 30)
        );

        mockMvc.perform(
                        get(
                                "/api/projects/{projectId}/tasks",
                                project.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(2));
    }

    @Test
    void getTasks_shouldFilterByPriority()
            throws Exception {

        createTask(
                project,
                "Critical API",
                TaskPriority.HIGH,
                LocalDate.of(2026, 8, 20)
        );

        createTask(
                project,
                "Documentation",
                TaskPriority.LOW,
                LocalDate.of(2026, 8, 25)
        );

        mockMvc.perform(
                        get(
                                "/api/projects/{projectId}/tasks",
                                project.getId()
                        )
                                .param("priority", "HIGH")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Critical API"))
                .andExpect(jsonPath("$.content[0].priority")
                        .value("HIGH"));
    }

    @Test
    void getTasks_shouldFilterByStatus()
            throws Exception {

        Task todo = createTask(
                project,
                "TODO Task",
                TaskPriority.HIGH,
                null
        );

        Task inProgress = createTask(
                project,
                "Working Task",
                TaskPriority.HIGH,
                null
        );

        inProgress.changeStatus(
                TaskStatus.IN_PROGRESS
        );

        taskRepository.save(inProgress);

        mockMvc.perform(
                        get(
                                "/api/projects/{projectId}/tasks",
                                project.getId()
                        )
                                .param(
                                        "status",
                                        "IN_PROGRESS"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(inProgress.getId()))
                .andExpect(jsonPath("$.content[0].status")
                        .value("IN_PROGRESS"));
    }

    @Test
    void getTasks_shouldSearchByTitleIgnoringCase()
            throws Exception {

        createTask(
                project,
                "Learn Spring Boot",
                TaskPriority.HIGH,
                null
        );

        createTask(
                project,
                "Learn Docker",
                TaskPriority.MEDIUM,
                null
        );

        mockMvc.perform(
                        get(
                                "/api/projects/{projectId}/tasks",
                                project.getId()
                        )
                                .param(
                                        "search",
                                        "SPRING"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Learn Spring Boot"));
    }

    // ------------------------------------------------------------
    // PAGINATION
    // ------------------------------------------------------------

    @Test
    void getTasks_shouldPaginateResults()
            throws Exception {

        createTask(
                project,
                "Task 1",
                TaskPriority.HIGH,
                null
        );

        createTask(
                project,
                "Task 2",
                TaskPriority.HIGH,
                null
        );

        createTask(
                project,
                "Task 3",
                TaskPriority.HIGH,
                null
        );

        mockMvc.perform(
                        get(
                                "/api/projects/{projectId}/tasks",
                                project.getId()
                        )
                                .param("page", "0")
                                .param("size", "2")
                                .param("sort", "title,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(2))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Task 1"))
                .andExpect(jsonPath("$.content[1].title")
                        .value("Task 2"));
    }

    // ------------------------------------------------------------
    // SORTING
    // ------------------------------------------------------------

    @Test
    void getTasks_shouldSortByDueDateAscending()
            throws Exception {

        createTask(
                project,
                "Later",
                TaskPriority.HIGH,
                LocalDate.of(2026, 9, 10)
        );

        createTask(
                project,
                "Earlier",
                TaskPriority.HIGH,
                LocalDate.of(2026, 8, 15)
        );

        mockMvc.perform(
                        get(
                                "/api/projects/{projectId}/tasks",
                                project.getId()
                        )
                                .param(
                                        "sort",
                                        "dueDate,asc"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title")
                        .value("Earlier"))
                .andExpect(jsonPath("$.content[1].title")
                        .value("Later"));
    }

    // ------------------------------------------------------------
    // COMBINED SPECIFICATIONS
    // ------------------------------------------------------------

    @Test
    void getTasks_shouldApplyMultipleFiltersTogether()
            throws Exception {

        Task matching = createTask(
                project,
                "Spring Security",
                TaskPriority.HIGH,
                null
        );

        matching.changeStatus(
                TaskStatus.IN_PROGRESS
        );

        taskRepository.save(matching);

        Task wrongPriority = createTask(
                project,
                "Spring MVC",
                TaskPriority.LOW,
                null
        );

        wrongPriority.changeStatus(
                TaskStatus.IN_PROGRESS
        );

        taskRepository.save(wrongPriority);

        createTask(
                project,
                "Docker",
                TaskPriority.HIGH,
                null
        );

        mockMvc.perform(
                        get(
                                "/api/projects/{projectId}/tasks",
                                project.getId()
                        )
                                .param(
                                        "status",
                                        "IN_PROGRESS"
                                )
                                .param(
                                        "priority",
                                        "HIGH"
                                )
                                .param(
                                        "search",
                                        "spring"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Spring Security"));
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    @Test
    void updateTask_shouldPersistChanges()
            throws Exception {

        Task task = createTask(
                project,
                "Old Title",
                TaskPriority.LOW,
                LocalDate.of(2026, 8, 20)
        );

        String json = """
                {
                  "title": "New Title",
                  "priority": "HIGH",
                  "status": "IN_PROGRESS",
                  "dueDate": "2026-09-01"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/tasks/{taskId}",
                                task.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("New Title"))
                .andExpect(jsonPath("$.priority")
                        .value("HIGH"))
                .andExpect(jsonPath("$.status")
                        .value("IN_PROGRESS"));

        Task updated = taskRepository
                .findById(task.getId())
                .orElseThrow();

        assertEquals(
                "New Title",
                updated.getTitle()
        );

        assertEquals(
                TaskPriority.HIGH,
                updated.getPriority()
        );

        assertEquals(
                TaskStatus.IN_PROGRESS,
                updated.getStatus()
        );

        assertEquals(
                LocalDate.of(2026, 9, 1),
                updated.getDueDate()
        );
    }

    @Test
    void updateTask_shouldSetCompletedAt_whenTaskBecomesDone()
            throws Exception {

        Task task = createTask(
                project,
                "Finish API",
                TaskPriority.HIGH,
                null
        );

        String json = """
                {
                  "status": "DONE"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/tasks/{taskId}",
                                task.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("DONE"))
                .andExpect(jsonPath("$.completedAt")
                        .exists());

        Task updated = taskRepository
                .findById(task.getId())
                .orElseThrow();

        assertEquals(
                TaskStatus.DONE,
                updated.getStatus()
        );

        assertNotNull(
                updated.getCompletedAt()
        );
    }

    @Test
    void updateTask_shouldReturn409_whenTransitionIsInvalid()
            throws Exception {

        Task task = createTask(
                project,
                "Blocked Task",
                TaskPriority.HIGH,
                null
        );

        task.changeStatus(
                TaskStatus.IN_PROGRESS
        );

        task.changeStatus(
                TaskStatus.BLOCKED
        );

        taskRepository.save(task);

        String json = """
                {
                  "status": "DONE"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/tasks/{taskId}",
                                task.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isConflict());

        Task unchanged = taskRepository
                .findById(task.getId())
                .orElseThrow();

        assertEquals(
                TaskStatus.BLOCKED,
                unchanged.getStatus()
        );

        assertNull(
                unchanged.getCompletedAt()
        );
    }

    // ------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------

    @Test
    void deleteTask_shouldRemoveTaskFromDatabase()
            throws Exception {

        Task task = createTask(
                project,
                "Delete Me",
                TaskPriority.LOW,
                null
        );

        Long taskId = task.getId();

        mockMvc.perform(
                        delete(
                                "/api/tasks/{taskId}",
                                taskId
                        )
                )
                .andExpect(status().isNoContent());

        assertFalse(
                taskRepository.existsById(taskId)
        );
    }

    @Test
    void deleteTask_shouldReturn404_whenTaskDoesNotExist()
            throws Exception {

        mockMvc.perform(
                        delete(
                                "/api/tasks/{taskId}",
                                999999L
                        )
                )
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private Task createTask(
            Project project,
            String title,
            TaskPriority priority,
            LocalDate dueDate
    ) {
        Task task = new Task(
                title,
                "Test description",
                project,
                priority,
                dueDate
        );

        return taskRepository.save(task);
    }

    private Project createProject(String name) {
        User owner = userRepository.save(
                new User(
                        name + " Owner",
                        name.toLowerCase()
                                .replace(" ", "")
                                + "@example.com"
                )
        );

        return projectRepository.save(
                new Project(
                        name,
                        "Test project",
                        owner
                )
        );
    }
}