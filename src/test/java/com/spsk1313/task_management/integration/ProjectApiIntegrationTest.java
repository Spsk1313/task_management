package com.spsk1313.task_management.integration;

import com.spsk1313.task_management.entity.Project;
import com.spsk1313.task_management.entity.User;
import com.spsk1313.task_management.repository.ProjectRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ProjectApiIntegrationTest {

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

    @BeforeEach
    void cleanDatabase() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createProject_shouldPersistProject_andReturn201() throws Exception {
        User owner = userRepository.save(
                new User("Sahil", "sahil@example.com")
        );

        String json = """
                {
                  "name": "Backend API",
                  "description": "Spring Boot project",
                  "ownerId": %d
                }
                """.formatted(owner.getId());

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Backend API"))
                .andExpect(jsonPath("$.description")
                        .value("Spring Boot project"))
                .andExpect(jsonPath("$.ownerId")
                        .value(owner.getId()));

        assertEquals(1, projectRepository.count());

        Project savedProject =
                projectRepository.findAll().getFirst();

        assertEquals("Backend API", savedProject.getName());
        assertEquals(
                "Spring Boot project",
                savedProject.getDescription()
        );
        assertEquals(
                owner.getId(),
                savedProject.getOwner().getId()
        );
    }

    @Test
    void getProjectsByOwnerId_shouldReturnOnlyOwnersProjects()
            throws Exception {

        User owner1 = userRepository.save(
                new User("Sahil", "sahil@example.com")
        );

        User owner2 = userRepository.save(
                new User("John", "john@example.com")
        );

        projectRepository.save(
                new Project("P1", "First", owner1)
        );

        projectRepository.save(
                new Project("P2", "Second", owner2)
        );

        mockMvc.perform(
                        get("/api/projects")
                                .param(
                                        "ownerId",
                                        owner1.getId().toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("P1"))
                .andExpect(jsonPath("$[0].ownerId")
                        .value(owner1.getId()));
    }

    @Test
    void updateProject_shouldPersistChanges_whenOwnerMatches()
            throws Exception {

        User owner = userRepository.save(
                new User("Sahil", "sahil@example.com")
        );

        Project project = projectRepository.save(
                new Project(
                        "Old Project",
                        "Old description",
                        owner
                )
        );

        String json = """
                {
                  "name": "Updated Project",
                  "description": "Updated description"
                }
                """;

        mockMvc.perform(
                        put("/api/projects/{id}", project.getId())
                                .param(
                                        "ownerId",
                                        owner.getId().toString()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Updated Project"))
                .andExpect(jsonPath("$.description")
                        .value("Updated description"))
                .andExpect(jsonPath("$.ownerId")
                        .value(owner.getId()));

        Project updatedProject =
                projectRepository.findById(project.getId())
                        .orElseThrow();

        assertEquals(
                "Updated Project",
                updatedProject.getName()
        );

        assertEquals(
                "Updated description",
                updatedProject.getDescription()
        );
    }

    @Test
    void updateProject_shouldReturn403_whenOwnerDoesNotMatch()
            throws Exception {

        User owner = userRepository.save(
                new User("Sahil", "sahil@example.com")
        );

        User otherUser = userRepository.save(
                new User("John", "john@example.com")
        );

        Project project = projectRepository.save(
                new Project(
                        "Original",
                        "Original description",
                        owner
                )
        );

        String json = """
                {
                  "name": "Hacked Project",
                  "description": "Changed"
                }
                """;

        mockMvc.perform(
                        put("/api/projects/{id}", project.getId())
                                .param(
                                        "ownerId",
                                        otherUser.getId().toString()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error")
                        .value("Forbidden"));

        Project unchangedProject =
                projectRepository.findById(project.getId())
                        .orElseThrow();

        assertEquals(
                "Original",
                unchangedProject.getName()
        );

        assertEquals(
                "Original description",
                unchangedProject.getDescription()
        );
    }

    @Test
    void createProject_shouldReturn409_whenNameAlreadyExistsForOwner()
            throws Exception {

        User owner = userRepository.save(
                new User("Sahil", "sahil@example.com")
        );

        projectRepository.save(
                new Project(
                        "Backend API",
                        "Existing project",
                        owner
                )
        );

        String json = """
                {
                  "name": "Backend API",
                  "description": "Duplicate project",
                  "ownerId": %d
                }
                """.formatted(owner.getId());

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"));

        assertEquals(1, projectRepository.count());
    }

    @Test
    void deleteProject_shouldRemoveProject_whenOwnerMatches()
            throws Exception {

        User owner = userRepository.save(
                new User("Sahil", "sahil@example.com")
        );

        Project project = projectRepository.save(
                new Project("P1", "Desc", owner)
        );

        Long projectId = project.getId();

        mockMvc.perform(
                        delete("/api/projects/{id}", projectId)
                                .param(
                                        "ownerId",
                                        owner.getId().toString()
                                )
                )
                .andExpect(status().isNoContent());

        assertFalse(
                projectRepository.existsById(projectId)
        );
    }

    @Test
    void deletingUser_shouldCascadeDeleteOwnedProjects() {
        User owner = userRepository.save(
                new User("Sahil", "sahil@example.com")
        );

        projectRepository.save(
                new Project("P1", "Desc", owner)
        );

        assertEquals(1, projectRepository.count());

        userRepository.deleteById(owner.getId());

        assertEquals(0, projectRepository.count());
    }
}