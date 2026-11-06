package com.tutorial.service;

import com.tutorial.model.Notification;
import com.tutorial.model.Task;
import com.tutorial.model.TaskStatus;
import java.util.Optional;
import java.util.UUID;

public final class TaskNotificationFactory {

  private TaskNotificationFactory() {
  }

  public static Notification created(Task task) {
    Optional<UUID> recipient = Optional.ofNullable(task.getAssignee());
    String message = recipient.isPresent()
        ? "Task " + task.getTitle() + " has been created and assigned"
        : "Task " + task.getTitle() + " has been created (unassigned)";
    return new Notification(recipient, task.getId(), message);
  }

  public static Notification assigned(Task task) {
    return new Notification(Optional.of(task.getAssignee()), task.getId(),
        "You were assigned task: " + task.getTitle());
  }

  public static Notification transitioned(Task task, TaskStatus previousStatus) {
    return new Notification(Optional.of(task.getAssignee()), task.getId(),
        "Task " + task.getTitle() + " was moved from " + previousStatus + " to "
            + task.getTaskStatus());
  }
}
