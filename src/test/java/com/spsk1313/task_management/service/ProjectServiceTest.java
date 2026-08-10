package com.spsk1313.task_management.service;

import com.spsk1313.task_management.dto.CreateProjectRequest;
import com.spsk1313.task_management.dto.ProjectResponse;
import com.spsk1313.task_management.dto.UpdateProjectRequest;
import com.spsk1313.task_management.entity.Project;
import com.spsk1313.task_management.entity.User;
import com.spsk1313.task_management.exception.DuplicateProjectNameException;
import com.spsk1313.task_management.exception.OperationForbiddenException;
import com.spsk1313.task_management.exception.ProjectNotFoundException;
import com.spsk1313.task_management.exception.UserNotFoundException;
import com.spsk1313.task_management.repository.ProjectRepository;
import com.spsk1313.task_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProjectServiceTest {

    private ProjectService projectService;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        userRepository = mock(UserRepository.class);

        projectService = new ProjectService(projectRepository, userRepository);
    }

    @Test
    void getProjects_shouldReturnAllProjects() {
        User owner = new User("Sahil", "sahil@example.com");
        Project project = new Project("P1", "Desc", owner);

        when(projectRepository.findAll())
                .thenReturn(List.of(project));

        List<ProjectResponse> projects = projectService.getProjects();

        assertEquals(1, projects.size());

        ProjectResponse response = projects.getFirst();

        assertEquals("P1", response.name());
        assertEquals("Desc", response.description());
    }

    @Test
    void getProjectsByOwnerId_shouldReturnProjects_whenOwnerExists() {
        User owner = new User("Sahil", "sahil@example.com");
        Project project = new Project("P1", "Desc", owner);

        when(userRepository.existsById(1L))
                .thenReturn(true);

        when(projectRepository.findByOwner_Id(1L))
                .thenReturn(List.of(project));

        List<ProjectResponse> projects =
                projectService.getProjectsByOwnerId(1L);

        assertEquals(1, projects.size());

        ProjectResponse response = projects.getFirst();

        assertEquals("P1", response.name());
        assertEquals("Desc", response.description());
    }

    @Test
    void getProjectsByOwnerId_shouldThrow_whenOwnerDoesNotExist() {
        when(userRepository.existsById(1L))
                .thenReturn(false);

        assertThrows(
                UserNotFoundException.class,
                () -> projectService.getProjectsByOwnerId(1L)
        );

        verify(projectRepository, never())
                .findByOwner_Id(any());
    }

    @Test
    void getProjectById_shouldReturnProject_whenProjectExists() {
        User owner = new User("Sahil", "sahil@example.com");
        Project project = new Project("P1", "Desc", owner);

        when(projectRepository.findById(1L))
                .thenReturn(Optional.of(project));

        ProjectResponse response =
                projectService.getProjectById(1L);

        assertEquals("P1", response.name());
        assertEquals("Desc", response.description());
    }

    @Test
    void getProjectById_shouldThrow_whenProjectDoesNotExist() {
        when(projectRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> projectService.getProjectById(1L)
        );
    }

    @Test
    void createProject_shouldCreateProject_whenRequestIsValid() {
        CreateProjectRequest request = new CreateProjectRequest(
                "P1",
                1L,
                "Desc"
        );

        User owner = new User(
                "Sahil",
                "sahil@example.com"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(owner));

        when(projectRepository.existsByOwner_IdAndName(1L, "P1"))
                .thenReturn(false);

        ProjectResponse response =
                projectService.createProject(request);

        assertEquals("P1", response.name());
        assertEquals("Desc", response.description());

        verify(projectRepository)
                .save(any(Project.class));
    }

    @Test
    void createProject_shouldThrow_whenOwnerDoesNotExist() {
        CreateProjectRequest request = new CreateProjectRequest(
                "P1",
                1L,
                "Desc"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> projectService.createProject(request)
        );

        verify(projectRepository, never())
                .save(any(Project.class));
    }

    @Test
    void createProject_shouldThrow_whenNameAlreadyExistsForOwner() {
        CreateProjectRequest request = new CreateProjectRequest(
                "P1",
                1L,
                "Desc"
        );

        User owner = new User(
                "Sahil",
                "sahil@example.com"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(owner));

        when(projectRepository.existsByOwner_IdAndName(1L, "P1"))
                .thenReturn(true);

        assertThrows(
                DuplicateProjectNameException.class,
                () -> projectService.createProject(request)
        );

        verify(projectRepository, never())
                .save(any(Project.class));
    }

    @Test
    void updateProject_shouldUpdateProject_whenRequestIsValid() {
        UpdateProjectRequest request =
                new UpdateProjectRequest(
                        "Updated Project",
                        "Updated description"
                );

        User owner = mock(User.class);
        Project project = mock(Project.class);

        when(owner.getId()).thenReturn(1L);

        when(project.getId()).thenReturn(10L);
        when(project.getOwner()).thenReturn(owner);
        when(project.getName()).thenReturn("Updated Project");
        when(project.getDescription()).thenReturn("Updated description");

        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));

        when(projectRepository.existsByOwner_IdAndNameAndIdNot(
                1L,
                "Updated Project",
                10L
        )).thenReturn(false);

        ProjectResponse response =
                projectService.updateProject(1L, 10L, request);

        verify(project).changeName("Updated Project");
        verify(project).changeDescription("Updated description");

        assertEquals("Updated Project", response.name());
        assertEquals("Updated description", response.description());
    }

    @Test
    void updateProject_shouldThrow_whenProjectDoesNotExist() {
        UpdateProjectRequest request =
                new UpdateProjectRequest(
                        "Updated Project",
                        "Updated description"
                );

        when(projectRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> projectService.updateProject(1L, 10L, request)
        );
    }

    @Test
    void updateProject_shouldThrow_whenOwnerDoesNotMatch() {
        UpdateProjectRequest request =
                new UpdateProjectRequest(
                        "Updated Project",
                        "Updated description"
                );

        User owner = mock(User.class);
        Project project = mock(Project.class);

        when(owner.getId()).thenReturn(1L);
        when(project.getOwner()).thenReturn(owner);

        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                OperationForbiddenException.class,
                () -> projectService.updateProject(999L, 10L, request)
        );

        verify(project, never()).changeName(anyString());
        verify(project, never()).changeDescription(any());

        verify(projectRepository, never())
                .existsByOwner_IdAndNameAndIdNot(
                        any(),
                        anyString(),
                        any()
                );
    }

    @Test
    void updateProject_shouldThrow_whenNameAlreadyExistsForOwner() {
        UpdateProjectRequest request =
                new UpdateProjectRequest(
                        "Existing Project",
                        "Description"
                );

        User owner = mock(User.class);
        Project project = mock(Project.class);

        when(owner.getId()).thenReturn(1L);

        when(project.getId()).thenReturn(10L);
        when(project.getOwner()).thenReturn(owner);

        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));

        when(projectRepository.existsByOwner_IdAndNameAndIdNot(
                1L,
                "Existing Project",
                10L
        )).thenReturn(true);

        assertThrows(
                DuplicateProjectNameException.class,
                () -> projectService.updateProject(1L, 10L, request)
        );

        verify(project, never()).changeName(anyString());
        verify(project, never()).changeDescription(any());
    }

    @Test
    void deleteProject_shouldDeleteProject_whenOwnerMatches() {
        User owner = mock(User.class);
        Project project = mock(Project.class);

        when(owner.getId()).thenReturn(1L);
        when(project.getOwner()).thenReturn(owner);

        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));

        projectService.deleteProject(1L, 10L);

        verify(projectRepository)
                .delete(project);
    }

    @Test
    void deleteProject_shouldThrow_whenProjectDoesNotExist() {
        when(projectRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> projectService.deleteProject(1L, 10L)
        );

        verify(projectRepository, never())
                .delete(any());
    }

    @Test
    void deleteProject_shouldThrow_whenOwnerDoesNotMatch() {
        User owner = mock(User.class);
        Project project = mock(Project.class);

        when(owner.getId()).thenReturn(1L);
        when(project.getOwner()).thenReturn(owner);

        when(projectRepository.findById(10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                OperationForbiddenException.class,
                () -> projectService.deleteProject(999L, 10L)
        );

        verify(projectRepository, never())
                .delete(any());
    }
}