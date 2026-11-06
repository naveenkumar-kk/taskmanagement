package com.tutorial.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TaskStatus {
  TODO, IN_PROGRESS, DONE, BLOCKED;

  @JsonCreator
  public static TaskStatus fromStoredValue(String value) {
    if ("INPROGRESS".equalsIgnoreCase(value)) {
      return IN_PROGRESS;
    }
    return TaskStatus.valueOf(value);
  }

  public boolean canTransitionTo(TaskStatus next) {
    if (next == null) {
      return false;
    }

    return switch (this) {
      case TODO, BLOCKED -> next == IN_PROGRESS;
      case IN_PROGRESS -> next == DONE || next == BLOCKED;
      case DONE -> false;
    };
  }
}
