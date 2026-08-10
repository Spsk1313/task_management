package com.spsk1313.task_management.controller;

import com.spsk1313.task_management.dto.CreateProjectRequest;
import com.spsk1313.task_management.dto.ProjectResponse;
import com.spsk1313.task_management.dto.UpdateProjectRequest;
import com.spsk1313.task_management.exception.DuplicateProjectNameException;
import com.spsk1313.task_management.exception.OperationForbiddenException;
import com.spsk1313.task_management.exception.ProjectNotFoundException;
import com.spsk1313.task_management.exception.UserNotFoundException;
import com.spsk1313.task_management.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectControllerTest {

    private ProjectService projectService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        projectService = mock(ProjectService.class);

        ProjectController projectController =
                new ProjectController(projectService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------
    // GET /api/projects
    // ---------------------------------------------------------

    @Test
    void shouldReturn200AndAllProjects() throws Exception {
        ProjectResponse response =
                new ProjectResponse(1L, "P1", "Desc", 1L);

        when(projectService.getProjects())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("P1"))
                .andExpect(jsonPath("$[0].description").value("Desc"))
                .andExpect(jsonPath("$[0].ownerId").value(1));
    }

    @Test
    void shouldReturn200AndOwnerProjects_whenOwnerIdProvided()
            throws Exception {

        ProjectResponse response =
                new ProjectResponse(1L, "P1", "Desc", 1L);

        when(projectService.getProjectsByOwnerId(1L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/projects")
                                .param("ownerId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("P1"))
                .andExpect(jsonPath("$[0].description").value("Desc"))
                .andExpect(jsonPath("$[0].ownerId").value(1));
    }

    // ---------------------------------------------------------
    // GET /api/projects/{id}
    // ---------------------------------------------------------

    @Test
    void shouldReturn200_whenProjectExists() throws Exception {
        ProjectResponse response =
                new ProjectResponse(1L, "P1", "Desc", 1L);

        when(projectService.getProjectById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("P1"))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.ownerId").value(1));
    }

    @Test
    void shouldReturn404_whenProjectDoesNotExist() throws Exception {
        when(projectService.getProjectById(999L))
                .thenThrow(new ProjectNotFoundException(999L));

        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ---------------------------------------------------------
    // POST /api/projects
    // ---------------------------------------------------------

    @Test
    void shouldReturn201_whenRequestIsValid() throws Exception {
        ProjectResponse response =
                new ProjectResponse(1L, "P1", "Desc", 1L);

        when(projectService.createProject(
                any(CreateProjectRequest.class)
        )).thenReturn(response);

        String json = """
                {
                  "name": "P1",
                  "description": "Desc",
                  "ownerId": 1
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("P1"))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.ownerId").value(1));
    }

    @Test
    void shouldReturn400_whenRequestIsInvalid() throws Exception {
        String json = """
                {
                  "name": "",
                  "description": "Desc",
                  "ownerId": null
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.ownerId").exists());
    }

    @Test
    void shouldReturn404_whenOwnerDoesNotExist() throws Exception {
        when(projectService.createProject(
                any(CreateProjectRequest.class)
        )).thenThrow(new UserNotFoundException(999L));

        String json = """
                {
                  "name": "P1",
                  "description": "Desc",
                  "ownerId": 999
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturn409_whenProjectNameAlreadyExists()
            throws Exception {

        when(projectService.createProject(
                any(CreateProjectRequest.class)
        )).thenThrow(new DuplicateProjectNameException("P1"));

        String json = """
                {
                  "name": "P1",
                  "description": "Desc",
                  "ownerId": 1
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ---------------------------------------------------------
    // PUT /api/projects/{id}?ownerId=...
    // ---------------------------------------------------------

    @Test
    void updateProject_shouldReturn200_whenRequestIsValid()
            throws Exception {

        ProjectResponse response =
                new ProjectResponse(
                        10L,
                        "Updated Project",
                        "Updated description",
                        1L
                );

        when(projectService.updateProject(
                eq(1L),
                eq(10L),
                any(UpdateProjectRequest.class)
        )).thenReturn(response);

        String json = """
                {
                  "name": "Updated Project",
                  "description": "Updated description"
                }
                """;

        mockMvc.perform(
                        put("/api/projects/10")
                                .param("ownerId", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name")
                        .value("Updated Project"))
                .andExpect(jsonPath("$.description")
                        .value("Updated description"))
                .andExpect(jsonPath("$.ownerId").value(1));
    }

    @Test
    void updateProject_shouldReturn400_whenRequestIsInvalid()
            throws Exception {

        String json = """
                {
                  "name": "",
                  "description": "Description"
                }
                """;

        mockMvc.perform(
                        put("/api/projects/10")
                                .param("ownerId", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void updateProject_shouldReturn404_whenProjectDoesNotExist()
            throws Exception {

        when(projectService.updateProject(
                eq(1L),
                eq(999L),
                any(UpdateProjectRequest.class)
        )).thenThrow(new ProjectNotFoundException(999L));

        String json = """
                {
                  "name": "Updated Project",
                  "description": "Updated description"
                }
                """;

        mockMvc.perform(
                        put("/api/projects/999")
                                .param("ownerId", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void updateProject_shouldReturn403_whenOwnerDoesNotMatch()
            throws Exception {

        when(projectService.updateProject(
                eq(999L),
                eq(10L),
                any(UpdateProjectRequest.class)
        )).thenThrow(new OperationForbiddenException());

        String json = """
                {
                  "name": "Updated Project",
                  "description": "Updated description"
                }
                """;

        mockMvc.perform(
                        put("/api/projects/10")
                                .param("ownerId", "999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void updateProject_shouldReturn409_whenProjectNameAlreadyExists()
            throws Exception {

        when(projectService.updateProject(
                eq(1L),
                eq(10L),
                any(UpdateProjectRequest.class)
        )).thenThrow(
                new DuplicateProjectNameException("Existing Project")
        );

        String json = """
                {
                  "name": "Existing Project",
                  "description": "Description"
                }
                """;

        mockMvc.perform(
                        put("/api/projects/10")
                                .param("ownerId", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ---------------------------------------------------------
    // DELETE /api/projects/{id}?ownerId=...
    // ---------------------------------------------------------

    @Test
    void deleteProject_shouldReturn204_whenOwnerMatches()
            throws Exception {

        mockMvc.perform(
                        delete("/api/projects/10")
                                .param("ownerId", "1")
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProject_shouldReturn404_whenProjectDoesNotExist()
            throws Exception {

        org.mockito.Mockito.doThrow(
                new ProjectNotFoundException(999L)
        ).when(projectService).deleteProject(1L, 999L);

        mockMvc.perform(
                        delete("/api/projects/999")
                                .param("ownerId", "1")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void deleteProject_shouldReturn403_whenOwnerDoesNotMatch()
            throws Exception {

        org.mockito.Mockito.doThrow(
                new OperationForbiddenException()
        ).when(projectService).deleteProject(999L, 10L);

        mockMvc.perform(
                        delete("/api/projects/10")
                                .param("ownerId", "999")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}