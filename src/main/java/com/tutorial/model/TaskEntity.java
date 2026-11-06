package com.tutorial.model;

import java.util.UUID;

public abstract class TaskEntity extends BaseEntity {

  private UUID taskId;


  public UUID getTaskId() {
    return taskId;
  }

  public void setTaskId(UUID taskId) {
    this.taskId = taskId;
  }

}
