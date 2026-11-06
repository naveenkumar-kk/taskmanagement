package com.tutorial.service;

import com.tutorial.model.Task;
import com.tutorial.model.Category;
import com.tutorial.model.Project;
import com.tutorial.model.Review;
import com.tutorial.model.User;
import com.tutorial.repository.CrudRepository;
import java.util.UUID;
import java.util.Objects;

public class TaskAssignmentService {

  private final UserService userService;
  private final ProjectService projectService;
  private final CrudRepository<Task> taskRepository;
  private final TaskCategoryReader<Review> reviewReader;
  private final TaskNotifier notificationService;

  public TaskAssignmentService(UserService userService, ProjectService projectService,
      CrudRepository<Task> taskRepository, TaskCategoryReader<Review> reviewReader,
      TaskNotifier notificationService) {
    this.userService = userService;
    this.projectService = projectService;
    this.taskRepository = taskRepository;
    this.reviewReader = reviewReader;
    this.notificationService = notificationService;
  }


  public void validateAssigneeProject(UUID projectId, UUID assigneeId) throws Exception {
    User user = userService.findById(assigneeId)
        .orElseThrow(() -> new IllegalArgumentException("Assignee not found"));

    Project project = projectService.findById(projectId).orElseThrow(
        () -> new IllegalArgumentException("Project not found"));
    if (!project.getMembers().contains(user)) {
      throw new IllegalArgumentException(
          "Assignee does not have access to this project");
    }
  }


  public void updateAssignee(UUID taskId, UUID assigneeId) throws Exception {

    Task task = taskRepository.findById(taskId).orElseThrow(
        () -> new Exception("Task with id: " + taskId + " not found")
    );
    validateAssigneeProject(task.getProjectId(), assigneeId);

    if (task.getCategory() == Category.REVIEW) {
      Review review = reviewReader.getByTaskId(taskId).orElseThrow(
          () -> new IllegalStateException("Review details were not found for this task"));
      Task referencedTask = taskRepository.findById(review.getReferenceTaskId()).orElseThrow(
          () -> new IllegalStateException("Referenced task was not found"));
      if (!task.getProjectId().equals(referencedTask.getProjectId())) {
        throw new IllegalArgumentException("A review must reference a task in the same project");
      }
      if (Objects.equals(referencedTask.getCreator(), assigneeId)) {
        throw new IllegalArgumentException("A reviewer cannot review their own task");
      }
    }

    task = task.assignedTo(assigneeId);
    taskRepository.update(taskId, task);
    notificationService.notifyTaskAssigned(task);
  }
}
