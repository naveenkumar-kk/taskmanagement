package com.tutorial.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tutorial.model.Bug;
import com.tutorial.model.Category;
import com.tutorial.model.Severity;
import com.tutorial.model.Task;
import com.tutorial.repository.CrudRepository;
import java.util.NoSuchElementException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class BugTaskHandler implements TaskCategoryHandler, TaskCompletionPolicy,
    TaskCategoryReader<Bug> {

  private final CrudRepository<Bug> repository;

  public BugTaskHandler(CrudRepository<Bug> repository) {
    this.repository = repository;
  }

  @Override
  public boolean supports(Category category) {
    return category == Category.BUG;
  }

  @Override
  public void validate(JsonNode details) throws Exception {
    if (details == null || !details.isObject()) {
      throw new IllegalArgumentException("Bug details must be a JSON object");
    }
    String reproductionSteps = details.path("reproductionSteps").asText("");
    if (reproductionSteps.isBlank()) {
      throw new IllegalArgumentException("Reproduction steps cannot be blank");
    }
    String severity = details.path("severity").asText("");
    try {
      Severity.valueOf(severity.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("A valid bug severity is required", exception);
    }
  }

  @Override
  public void save(UUID taskId, JsonNode details) throws Exception {
    Severity severity = Severity.valueOf(
        details.get("severity").asText().trim().toUpperCase(Locale.ROOT));
    String reproductionSteps = details.get("reproductionSteps").asText();
    repository.save(new Bug(taskId, severity, reproductionSteps));
  }


  @Override
  public Optional<Bug> getByTaskId(UUID taskId) throws Exception {
    return repository.findAll().stream().filter(bug -> taskId.equals(bug.getTaskId())).findFirst();
  }


  @Override
  public void complete(Task task, Optional<String> details) throws Exception {
    if (details.isEmpty()) {
      throw new IllegalArgumentException("Bug resolution is required");
    }
    UUID taskId = task.getId();

    Bug bug = getByTaskId(taskId).orElseThrow(
        () -> new NoSuchElementException("Bug not found for task:" + taskId));

    String resolution = details.filter(value -> !value.isBlank())
        .orElseThrow(() -> new IllegalArgumentException("Bug resolution cannot be blank"));
    bug.setResolution(resolution);
    repository.update(bug.getId(), bug);
  }
}
