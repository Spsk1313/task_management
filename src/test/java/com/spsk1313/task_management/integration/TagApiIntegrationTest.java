package com.spsk1313.task_management.integration;

import com.spsk1313.task_management.entity.*;
import com.spsk1313.task_management.repository.ProjectRepository;
import com.spsk1313.task_management.repository.TagRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TagApiIntegrationTest {

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

    @Autowired
    private TagRepository tagRepository;

    private Project project;

    @BeforeEach
    void setUp() {
        /*
         * Delete in dependency order:
         *
         * task_tags is removed when tasks/tags are deleted
         * because of the FK behavior.
         */
        taskRepository.deleteAll();
        tagRepository.deleteAll();
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
                        "Integration test project",
                        owner
                )
        );
    }

    // ---------------------------------------------------------------
    // CREATE + ATTACH
    // ---------------------------------------------------------------

    @Test
    void addTag_shouldCreateTagAndAttachItToTask()
            throws Exception {

        Task task = createTask("Build API");

        String json = """
                {
                  "name": "java"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/tasks/{taskId}/tags",
                                task.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(task.getId()))
                .andExpect(jsonPath("$.tags.length()")
                        .value(1))
                .andExpect(jsonPath("$.tags[0]")
                        .value("java"));

        /*
         * Prove the Tag itself was persisted.
         */
        assertEquals(1, tagRepository.count());

        Tag javaTag = tagRepository
                .findByName("java")
                .orElseThrow();

        assertNotNull(javaTag.getId());
        assertEquals("java", javaTag.getName());

        /*
         * Reload Task from PostgreSQL and prove that
         * the relationship was persisted too.
         */
        Task persistedTask = taskRepository
                .findById(task.getId())
                .orElseThrow();

        Set<String> tagNames = persistedTask
                .getTags()
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("java"), tagNames);
    }

    // ---------------------------------------------------------------
    // NORMALIZATION
    // ---------------------------------------------------------------

    @Test
    void addTag_shouldNormalizeTagNameBeforePersisting()
            throws Exception {

        Task task = createTask("Build API");

        String json = """
                {
                  "name": "   JaVa   "
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/tasks/{taskId}/tags",
                                task.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]")
                        .value("java"));

        assertEquals(1, tagRepository.count());

        assertTrue(
                tagRepository
                        .findByName("java")
                        .isPresent()
        );

        assertTrue(
                tagRepository
                        .findByName("   JaVa   ")
                        .isEmpty()
        );
    }

    // ---------------------------------------------------------------
    // GLOBAL TAG REUSE
    // ---------------------------------------------------------------

    @Test
    void addTag_shouldReuseExistingTagAcrossDifferentTasks()
            throws Exception {

        Task taskOne = createTask("Build API");
        Task taskTwo = createTask("Write Tests");

        addTag(taskOne.getId(), "java");
        addTag(taskTwo.getId(), " JAVA ");

        /*
         * THIS is the important assertion.
         *
         * Two tasks use "java", but tags contains
         * only ONE canonical row.
         */
        assertEquals(1, tagRepository.count());

        Tag javaTag = tagRepository
                .findByName("java")
                .orElseThrow();

        Task persistedTaskOne = taskRepository
                .findById(taskOne.getId())
                .orElseThrow();

        Task persistedTaskTwo = taskRepository
                .findById(taskTwo.getId())
                .orElseThrow();

        Set<Long> taskOneTagIds = persistedTaskOne
                .getTags()
                .stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        Set<Long> taskTwoTagIds = persistedTaskTwo
                .getTags()
                .stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of(javaTag.getId()),
                taskOneTagIds
        );

        assertEquals(
                Set.of(javaTag.getId()),
                taskTwoTagIds
        );
    }

    // ---------------------------------------------------------------
    // DUPLICATE ASSOCIATION
    // ---------------------------------------------------------------

    @Test
    void addTag_shouldReturn409_whenTagAlreadyAttachedToSameTask()
            throws Exception {

        Task task = createTask("Build API");

        addTag(task.getId(), "java");

        String duplicateRequest = """
                {
                  "name": " JAVA "
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/tasks/{taskId}/tags",
                                task.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(duplicateRequest)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"));

        /*
         * Still only one global Tag.
         */
        assertEquals(1, tagRepository.count());

        Task persistedTask = taskRepository
                .findById(task.getId())
                .orElseThrow();

        /*
         * And still only one relationship.
         */
        assertEquals(
                1,
                persistedTask.getTags().size()
        );
    }

    // ---------------------------------------------------------------
    // MULTIPLE TAGS
    // ---------------------------------------------------------------

    @Test
    void addTag_shouldAllowMultipleDifferentTagsOnSameTask()
            throws Exception {

        Task task = createTask("Build API");

        addTag(task.getId(), "java");
        addTag(task.getId(), "spring");
        addTag(task.getId(), "backend");

        assertEquals(3, tagRepository.count());

        Task persistedTask = taskRepository
                .findById(task.getId())
                .orElseThrow();

        Set<String> tagNames = persistedTask
                .getTags()
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        "java",
                        "spring",
                        "backend"
                ),
                tagNames
        );
    }

    // ---------------------------------------------------------------
    // MISSING TASK
    // ---------------------------------------------------------------

    @Test
    void addTag_shouldReturn404_whenTaskDoesNotExist()
            throws Exception {

        String json = """
                {
                  "name": "java"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/tasks/{taskId}/tags",
                                999999L
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"));

        /*
         * Failure must not accidentally create a Tag.
         */
        assertEquals(0, tagRepository.count());
    }

    // ---------------------------------------------------------------
    // VALIDATION
    // ---------------------------------------------------------------

    @Test
    void addTag_shouldReturn400_whenTagNameIsBlank()
            throws Exception {

        Task task = createTask("Build API");

        String json = """
                {
                  "name": "   "
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/tasks/{taskId}/tags",
                                task.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        assertEquals(0, tagRepository.count());
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private Task createTask(String title) {
        Task task = new Task(
                title,
                "Test description",
                project,
                TaskPriority.HIGH,
                null
        );

        return taskRepository.save(task);
    }

    private void addTag(
            Long taskId,
            String name
    ) throws Exception {

        String json = """
                {
                  "name": "%s"
                }
                """.formatted(name);

        mockMvc.perform(
                        post(
                                "/api/tasks/{taskId}/tags",
                                taskId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }
}