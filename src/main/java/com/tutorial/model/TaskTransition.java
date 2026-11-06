package com.tutorial.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TaskTransition extends BaseEntity {

  private UUID taskId;
  private TaskStatus fromStatus;
  private TaskStatus toStatus;
  private String reason;
  private Instant createdAt;

  @JsonCreator
  public TaskTransition(
      @JsonProperty("taskId") UUID taskId,
      @JsonProperty("fromStatus") TaskStatus fromStatus,
      @JsonProperty("toStatus") TaskStatus toStatus,
      @JsonProperty("reason") Optional<String> reason) {
    this.taskId = taskId;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.reason = reason == null ? null : reason.orElse(null);
    this.createdAt = Instant.now();

  }

  public UUID getTaskId() {
    return taskId;
  }

  public void setTaskId(UUID taskId) {
    this.taskId = taskId;
  }

  public TaskStatus getFromStatus() {
    return fromStatus;
  }

  public void setFromStatus(TaskStatus fromStatus) {
    this.fromStatus = fromStatus;
  }

  public TaskStatus getToStatus() {
    return toStatus;
  }

  public void setToStatus(TaskStatus toStatus) {
    this.toStatus = toStatus;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

}
