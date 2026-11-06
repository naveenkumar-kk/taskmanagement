package com.tutorial.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class Feature extends TaskEntity {

  private final String acceptanceCriteria;


  @JsonCreator
  public Feature(
      @JsonProperty("taskId") UUID taskId,
      @JsonProperty("acceptanceCriteria") String acceptanceCriteria) {
    this.acceptanceCriteria = acceptanceCriteria;
    setId();
    setTaskId(taskId);
  }

  public String getAcceptanceCriteria() {
    return acceptanceCriteria;
  }

}
