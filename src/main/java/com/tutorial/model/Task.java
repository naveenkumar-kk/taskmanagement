package com.tutorial.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class Task extends BaseEntity {


  private final Category category;
  private final UUID projectId;
  private final UUID assignee;
  private final Priority priority;
  private final UUID creator;
  private final String description;
  private final String title;
  private final TaskStatus taskStatus;

  private final Instant completedAt;

  @JsonCreator
  public Task(
      @JsonProperty("title") String title,
      @JsonProperty("description") String description,
      @JsonProperty("category") Category category,
      @JsonProperty("priority") Priority priority,
      @JsonProperty("taskStatus") TaskStatus taskStatus,
      @JsonProperty("projectId") UUID projectId,
      @JsonProperty("creator") UUID creator,
      @JsonProperty("assignee") UUID assignee,
      @JsonProperty("completedAt") Instant completedAt) {

    this.projectId = projectId;
    this.priority = priority;
    this.category = category;
    this.taskStatus = taskStatus;
    this.description = description;
    this.title = title;
    this.creator = creator;
    this.assignee = assignee;
    this.completedAt = completedAt;
  }

  public Task(String title, String description, Category category, Priority priority,
      TaskStatus taskStatus, UUID projectId, UUID creator, Optional<UUID> assignee) {
    this(title, description, category, priority, taskStatus, projectId, creator,
        assignee == null ? null : assignee.orElse(null), null);
  }

  public UUID getProjectId() {
    return projectId;
  }

  public UUID getAssignee() {
    return assignee;
  }

  public Priority getPriority() {
    return priority;
  }

  public TaskStatus getTaskStatus() {
    return taskStatus;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void validateTransitionTo(TaskStatus nextStatus) {
    if (taskStatus == null || !taskStatus.canTransitionTo(nextStatus)) {
      throw new IllegalStateException(
          "Cannot change status from " + taskStatus + " to " + nextStatus);
    }
  }

  public Task start() {
    if (assignee == null) {
      throw new IllegalStateException("Task must have an assignee before work can start");
    }
    return withStatus(TaskStatus.IN_PROGRESS, null);
  }

  public Task block(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("A reason is required to block a task");
    }
    return withStatus(TaskStatus.BLOCKED, null);
  }

  public Task resume() {
    return withStatus(TaskStatus.IN_PROGRESS, null);
  }

  public Task complete() {
    return withStatus(TaskStatus.DONE, Instant.now());
  }

  public Task assignedTo(UUID assigneeId) {
    if (taskStatus == TaskStatus.DONE) {
      throw new IllegalStateException("A completed task cannot be reassigned");
    }
    if (assigneeId == null) {
      throw new IllegalArgumentException("Assignee cannot be null");
    }
    return new Task(title, description, category, priority, taskStatus, projectId, creator,
        assigneeId, completedAt, getId(), getCreatedAt());
  }

  private Task withStatus(TaskStatus nextStatus, Instant completedTime) {
    validateTransitionTo(nextStatus);
    return new Task(title, description, category, priority, nextStatus, projectId, creator,
        assignee, completedTime, getId(), getCreatedAt());
  }

  private Task(String title, String description, Category category, Priority priority,
      TaskStatus taskStatus, UUID projectId, UUID creator, UUID assignee,
      Instant completedAt, UUID id, Instant createdAt) {
    this(title, description, category, priority, taskStatus, projectId, creator, assignee,
        completedAt);
    if (id != null) {
      setId(id);
    }
    setCreatedAt(createdAt);
  }

  @Override
  public void setId() {
    if (getId() != null) {
      throw new IllegalStateException("A task ID cannot be changed after it is assigned");
    }
    super.setId();
  }

  @Override
  public void setId(UUID id) {
    if (getId() != null && !getId().equals(id)) {
      throw new IllegalStateException("A task ID cannot be changed after it is assigned");
    }
    super.setId(id);
  }

  public UUID getCreator() {
    return creator;
  }

  public String getDescription() {
    return description;
  }

  public String getTitle() {
    return title;
  }

  public Category getCategory() {
    return category;
  }

}
