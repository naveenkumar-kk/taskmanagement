package com.tutorial.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class Bug extends TaskEntity {


  private final String reproductionSteps;
  private String resolution;
  private Severity severity;

  @JsonCreator
  public Bug(
      @JsonProperty("taskId") UUID taskId,
      @JsonProperty("severity") Severity severity,
      @JsonProperty("reproductionSteps") String reproductionSteps) {
    this.reproductionSteps = reproductionSteps;
    this.severity = severity;
    setId();
    setTaskId(taskId);
  }


  public String getResolution() {
    return resolution;
  }

  public String getReproductionSteps() {
    return reproductionSteps;
  }

  public void setResolution(String description) {
    this.resolution = description;
  }

  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

}
