package com.spsk1313.task_management.integration;

import com.spsk1313.task_management.entity.User;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_shouldPersistUser_andReturn201() throws Exception {
        String json = """
                {
                  "name": "Sahil",
                  "email": "SAHIL@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Sahil"))
                .andExpect(jsonPath("$.email").value("sahil@example.com"));

        List<User> users = userRepository.findAll();

        assertEquals(1, users.size());

        User savedUser = users.getFirst();

        assertNotNull(savedUser.getId());
        assertEquals("Sahil", savedUser.getName());
        assertEquals("sahil@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    void getUserById_shouldReturnPersistedUser() throws Exception {
        User user = new User("Sahil", "sahil@example.com");
        User savedUser = userRepository.save(user);

        mockMvc.perform(get("/api/users/{id}", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.name").value("Sahil"))
                .andExpect(jsonPath("$.email").value("sahil@example.com"));
    }

    @Test
    void updateUser_shouldPersistChanges() throws Exception {
        User user = new User("Sahil", "old@example.com");
        User savedUser = userRepository.save(user);

        Long id = savedUser.getId();

        String json = """
                {
                  "name": "Sahilpreet",
                  "email": "NEW@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Sahilpreet"))
                .andExpect(jsonPath("$.email").value("new@example.com"));

        User updatedUser = userRepository.findById(id)
                .orElseThrow();

        assertEquals("Sahilpreet", updatedUser.getName());
        assertEquals("new@example.com", updatedUser.getEmail());
    }

    @Test
    void createUser_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        userRepository.save(
                new User("Existing User", "sahil@example.com")
        );

        String json = """
                {
                  "name": "Another User",
                  "email": "SAHIL@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));

        assertEquals(1, userRepository.count());
    }
}