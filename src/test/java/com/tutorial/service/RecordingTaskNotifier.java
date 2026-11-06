package com.tutorial.service;

import com.tutorial.model.Notification;
import com.tutorial.model.Task;
import com.tutorial.model.TaskStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class RecordingTaskNotifier implements TaskNotifier {

  private final List<Notification> notifications = new ArrayList<>();
  private final List<UUID> assignedTaskIds = new ArrayList<>();
  private final List<UUID> assignedUserIds = new ArrayList<>();
  private final List<TaskStatus> transitionStatuses = new ArrayList<>();

  @Override
  public Notification notifyTaskCreated(Task task) {
    return record(TaskNotificationFactory.created(task));
  }

  @Override
  public Notification notifyTaskAssigned(Task task) {
    assignedTaskIds.add(task.getId());
    assignedUserIds.add(task.getAssignee());
    return record(TaskNotificationFactory.assigned(task));
  }

  @Override
  public Notification notifyTaskTransition(Task task, TaskStatus previousStatus) {
    transitionStatuses.add(task.getTaskStatus());
    return record(TaskNotificationFactory.transitioned(task, previousStatus));
  }

  List<Notification> getNotifications() {
    return List.copyOf(notifications);
  }

  List<UUID> getAssignedTaskIds() {
    return List.copyOf(assignedTaskIds);
  }

  List<UUID> getAssignedUserIds() {
    return List.copyOf(assignedUserIds);
  }

  List<TaskStatus> getTransitionStatuses() {
    return List.copyOf(transitionStatuses);
  }

  private Notification record(Notification notification) {
    notifications.add(notification);
    return notification;
  }
}
