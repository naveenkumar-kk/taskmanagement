package com.tutorial.service;

import com.tutorial.model.Category;
import com.tutorial.model.Task;
import com.tutorial.model.TaskStatus;
import com.tutorial.model.TaskTransition;
import com.tutorial.repository.CrudRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TaskTransitionService {

  private final CrudRepository<Task> taskRepository;

  private final CrudRepository<TaskTransition> taskTransitionRepository;
  private final Map<Category, TaskCompletionPolicy> completionPolicies;
  private final TaskNotifier notificationService;

  public TaskTransitionService(CrudRepository<Task> taskRepository,
      CrudRepository<TaskTransition> taskTransitionRepository,
      Map<Category, TaskCompletionPolicy> completionPolicies,
      TaskNotifier notificationService) {
    this.taskRepository = taskRepository;
    this.taskTransitionRepository = taskTransitionRepository;
    this.completionPolicies = completionPolicies;
    this.notificationService = notificationService;
  }

  public void updateStatus(UUID taskId, TaskStatus newStatus, String reason,
      String completionDetails) throws Exception {
    Task task = taskRepository.findById(taskId).orElseThrow(
        () -> new Exception("Task with id: " + taskId + " not found")
    );
    TaskStatus previousStaus = task.getTaskStatus();
    Optional<String> details = Optional.ofNullable(completionDetails);

    if (newStatus == null) {
      throw new IllegalArgumentException("New task status is required");
    }

    switch (newStatus) {
      case TODO -> task.validateTransitionTo(newStatus);
      case IN_PROGRESS -> {
        if (previousStaus == TaskStatus.BLOCKED) {
          task = task.resume();
        } else {
          task = task.start();
        }
      }
      case BLOCKED -> task = task.block(reason);
      case DONE -> {
        task.validateTransitionTo(TaskStatus.DONE);
        TaskCompletionPolicy policy = completionPolicies.get(task.getCategory());
        if (policy != null) {
          policy.complete(task, details);
        }
        task = task.complete();
      }
    }

    taskRepository.update(taskId, task);
    taskTransitionRepository.save(
        new TaskTransition(taskId, previousStaus, newStatus, Optional.ofNullable(reason)));
    if (task.getAssignee() != null) {
      notificationService.notifyTaskTransition(task, previousStaus);
    }
  }

  public void startTask(UUID taskId) throws Exception {
    updateStatus(taskId, TaskStatus.IN_PROGRESS, null, null);
  }

  public void blockTask(UUID taskId, String reason) throws Exception {
    updateStatus(taskId, TaskStatus.BLOCKED, reason, null);
  }

  public void resumeTask(UUID taskId) throws Exception {
    updateStatus(taskId, TaskStatus.IN_PROGRESS, null, null);
  }

  public void completeTask(UUID taskId, String completionDetails) throws Exception {
    updateStatus(taskId, TaskStatus.DONE, null, completionDetails);
  }


}
