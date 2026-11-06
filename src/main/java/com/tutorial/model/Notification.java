package com.tutorial.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import java.util.UUID;

public class Notification extends BaseEntity {

  private Optional<UUID> recipientId;
  private UUID taskId;
  private String message;

  @JsonCreator
  public Notification(
      @JsonProperty("recipientId") Optional<UUID> recipientId,
      @JsonProperty("taskId") UUID taskId,
      @JsonProperty("message") String message) {
    this.recipientId = recipientId.isEmpty() ? Optional.empty() : recipientId;
    this.taskId = taskId;
    this.message = message;
  }

  public Optional<UUID> getRecipientId() {
    return this.recipientId;
  }

  public void setRecipientId(Optional<UUID> recipientId) {
    this.recipientId = recipientId;
  }

  public UUID getTaskId() {
    return this.taskId;
  }

  public void setTaskId(UUID taskId) {
    this.taskId = taskId;
  }

  public String getMessage() {
    return this.message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
