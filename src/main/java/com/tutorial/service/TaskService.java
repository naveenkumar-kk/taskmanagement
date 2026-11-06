package com.tutorial.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.model.Category;
import com.tutorial.model.Priority;
import com.tutorial.model.Project;
import com.tutorial.model.Task;
import com.tutorial.model.TaskStatus;
import com.tutorial.repository.CrudRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class TaskService {

  private final CrudRepository<Task> repository;
  private final ProjectService projectService;
  private final UserService userService;
  private final Map<Category, TaskCategoryHandler> handlers;
  private final TaskTransitionService transitionService;
  private final TaskNotifier taskNotifier;
  private final TaskAssignmentService taskAssignmentService;

  public TaskService(CrudRepository<Task> repository, ProjectService projectService,
      UserService userService, Map<Category, TaskCategoryHandler> handlers,
      TaskTransitionService transitionService,
      TaskNotifier taskNotifier, TaskAssignmentService taskAssignmentService
  ) {
    this.repository = repository;
    this.projectService = projectService;
    this.userService = userService;
    this.handlers = handlers;
    this.transitionService = transitionService;
    this.taskNotifier = taskNotifier;
    this.taskAssignmentService = taskAssignmentService;
  }

  public List<Task> findAll() {
    return List.copyOf(repository.findAll());
  }

  public Optional<Task> findById(UUID id) {
    return repository.findById(id);
  }

  public List<Task> findAllByPriority(Priority priority) {
    return repository.findAll().stream().filter(task -> task.getPriority() == priority)
        .toList();
  }

  public Map<TaskStatus, Long> getCountByStatus() {
    return Map.copyOf(repository.findAll().stream()
        .collect(Collectors.groupingBy(Task::getTaskStatus, Collectors.counting())));
  }

  public List<Task> findByFilters(Map<String, String> filters) {
    return repository.findAll().stream()
        .filter(task -> filters.entrySet().stream()
            .allMatch(filter -> matches(
                task, filter.getKey(), filter.getValue())))
        .toList();
  }

  private boolean matches(Task task, String field, String value) {
    return switch (field.toLowerCase()) {
      case "title" -> task.getTitle() != null
          && task.getTitle().contains(value);
      case "priority" -> task.getPriority() != null
          && task.getPriority().name().equalsIgnoreCase(value);
      case "status" -> task.getTaskStatus() != null
          && task.getTaskStatus().name().equalsIgnoreCase(value);
      case "assignee" -> task.getAssignee() != null
          && task.getAssignee().equals(UUID.fromString(value));
      case "category" -> task.getCategory() != null
          && task.getCategory().name().equalsIgnoreCase(value);
      default -> throw new IllegalArgumentException(
          "Unsupported task field: " + field);
    };
  }

  public Optional<Task> findByTitle(String title) throws Exception {
    try {
      return repository.findAll().stream().filter(task -> task.getTitle().equals(title))
          .findFirst();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Optional<List<Task>> findByProjectId(UUID projectId) {
    return Optional.of(repository.findAll().stream().filter(
        e -> e.getProjectId().equals(projectId)
    ).toList());
  }


  public Task save(UUID projectId, Priority priority, Category category, UUID creator,
      String description, String title, Optional<UUID> assignee, String categoryInfo)
      throws Exception {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("Task title cannot be blank");
    }
    if (priority == null || category == null) {
      throw new IllegalArgumentException("Task priority and category are required");
    }
    if (creator == null || userService.findById(creator).isEmpty()) {
      throw new IllegalArgumentException("Task creator was not found");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Task description cannot be blank");
    }

    Optional<Project> project = projectService.findById(projectId);
    if (project.isEmpty()) {
      throw new Exception("Project not found");
    }

    if (categoryInfo == null || categoryInfo.isBlank()) {
      throw new Exception("Detailed Description is empty");
    }

    Optional<UUID> selectedAssignee = assignee.isEmpty() ? Optional.empty() : assignee;
    if (selectedAssignee.isPresent()) {
      taskAssignmentService.validateAssigneeProject(projectId, selectedAssignee.get());
    }

    ObjectMapper detailedTaskMapper = new ObjectMapper();
    JsonNode detailedTaskInfo = detailedTaskMapper.readTree(categoryInfo);

    if (!detailedTaskInfo.isObject()) {
      throw new IllegalArgumentException("Task details must be a JSON object");
    }
    TaskCategoryHandler handler = handlers.get(category);
    if (handler == null) {
      throw new IllegalArgumentException("Unsupported task category: " + category);
    }

    handler.validateForCreation(detailedTaskInfo, projectId, selectedAssignee);

    Task task = new Task(
        title,
        description,
        category,
        priority,
        TaskStatus.TODO,
        projectId,
        creator,
        selectedAssignee

    );
    repository.save(task);

    handler.save(task.getId(), detailedTaskInfo);
    taskNotifier.notifyTaskCreated(task);
    return task;
  }


  public void startTask(UUID taskId) throws Exception {
    transitionService.startTask(taskId);
  }

  public void blockTask(UUID taskId, String reason) throws Exception {
    transitionService.blockTask(taskId, reason);
  }

  public void resumeTask(UUID taskId) throws Exception {
    transitionService.resumeTask(taskId);
  }

  public void completeTask(UUID taskId, String completionDetails) throws Exception {
    transitionService.completeTask(taskId, completionDetails);
  }

  public void assignTask(UUID taskId, UUID assigneeId) throws Exception {
    taskAssignmentService.updateAssignee(taskId, assigneeId);
  }
}
