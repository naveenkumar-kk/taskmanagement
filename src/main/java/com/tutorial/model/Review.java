package com.tutorial.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class Review extends TaskEntity {

  private final UUID referenceTaskId;

  @JsonCreator
  public Review(
      @JsonProperty("taskId") UUID taskId,
      @JsonProperty("referenceTaskId") UUID referenceTaskId) {
    this.referenceTaskId = referenceTaskId;
    setTaskId(taskId);
  }

  public UUID getReferenceTaskId() {
    return referenceTaskId;
  }


}
