package com.tutorial.service;

import com.tutorial.model.Notification;
import com.tutorial.model.Task;
import com.tutorial.model.TaskStatus;

public class ConsoleTaskNotifier implements TaskNotifier {

  private final TaskNotifier delegate;

  public ConsoleTaskNotifier(TaskNotifier delegate) {
    this.delegate = delegate;
  }

  @Override
  public Notification notifyTaskCreated(Task task) throws Exception {
    return printAfterDelivery(delegate.notifyTaskCreated(task));
  }

  @Override
  public Notification notifyTaskAssigned(Task task) throws Exception {
    return printAfterDelivery(delegate.notifyTaskAssigned(task));
  }

  @Override
  public Notification notifyTaskTransition(Task task, TaskStatus previousStatus)
      throws Exception {
    return printAfterDelivery(delegate.notifyTaskTransition(task, previousStatus));
  }

  private Notification printAfterDelivery(Notification notification) {
    String recipient = notification.getRecipientId()
        .map(Object::toString)
        .orElse("none (unassigned)");
    System.out.printf("Notification sent | recipient: %s | %s%n",
        recipient, notification.getMessage());
    return notification;
  }
}
