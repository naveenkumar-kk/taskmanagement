package com.tutorial.service;

import com.tutorial.model.Notification;
import com.tutorial.model.Task;
import com.tutorial.model.TaskStatus;
import com.tutorial.repository.CrudRepository;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class NotificationService implements TaskNotifier {

  private final CrudRepository<Notification> repository;

  public NotificationService(CrudRepository<Notification> repository) {
    this.repository = repository;
  }

  @Override
  public Notification notifyTaskCreated(Task task) throws IOException {
    Notification notification = TaskNotificationFactory.created(task);
    repository.save(notification);
    return notification;
  }

  @Override
  public Notification notifyTaskAssigned(Task task) throws IOException {
    Notification notification = TaskNotificationFactory.assigned(task);
    repository.save(notification);
    return notification;
  }

  @Override
  public Notification notifyTaskTransition(Task task, TaskStatus previousStatus)
      throws IOException {
    Notification notification = TaskNotificationFactory.transitioned(task, previousStatus);
    repository.save(notification);
    return notification;
  }
}
