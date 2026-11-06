package com.tutorial.service;

import com.tutorial.model.Notification;
import com.tutorial.model.Task;
import com.tutorial.model.TaskStatus;

public interface TaskNotifier {

  Notification notifyTaskCreated(Task task) throws Exception;

  Notification notifyTaskAssigned(Task task) throws Exception;

  Notification notifyTaskTransition(Task task, TaskStatus previousStatus) throws Exception;
}
