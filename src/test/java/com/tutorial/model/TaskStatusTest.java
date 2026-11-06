package com.tutorial.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class TaskStatusTest {

  @Test
  void todoCanMoveToInProgressButNotDone() {
    assertTrue(TaskStatus.TODO.canTransitionTo(TaskStatus.IN_PROGRESS));
    assertFalse(TaskStatus.TODO.canTransitionTo(TaskStatus.DONE));
  }

  @Test
  void doneCannotMoveToAnotherStatus() {
    for (TaskStatus next : TaskStatus.values()) {
      assertFalse(TaskStatus.DONE.canTransitionTo(next));
    }
  }

  @Test
  void readsLegacyInProgressStatusSpelling() throws Exception {
    assertEquals(TaskStatus.IN_PROGRESS,
        new ObjectMapper().readValue("\"INPROGRESS\"", TaskStatus.class));
  }
}
