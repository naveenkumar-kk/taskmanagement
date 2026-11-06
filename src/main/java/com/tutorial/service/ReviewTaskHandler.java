package com.tutorial.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tutorial.model.Category;
import com.tutorial.model.Review;
import com.tutorial.model.Task;
import com.tutorial.repository.CrudRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ReviewTaskHandler implements TaskCategoryHandler, TaskCategoryReader<Review> {

  private final CrudRepository<Review> repository;
  private final CrudRepository<Task> taskRepository;


  public ReviewTaskHandler(CrudRepository<Review> repository, CrudRepository<Task> taskRepository) {
    this.repository = repository;
    this.taskRepository = taskRepository;
  }

  @Override
  public boolean supports(Category category) {
    return category == Category.REVIEW;
  }

  @Override
  public void validate(JsonNode details) throws Exception {
    if (details == null || !details.isObject() || !details.hasNonNull("referenceTaskId")) {
      throw new IllegalArgumentException("A referenceTaskId is required for a review");
    }

    findReferenceTask(details);
  }

  @Override
  public void validateForCreation(JsonNode details, UUID projectId,
      Optional<UUID> assigneeId) throws Exception {
    validate(details);
    Task referenceTask = findReferenceTask(details);

    if (!Objects.equals(projectId, referenceTask.getProjectId())) {
      throw new IllegalArgumentException("A review must reference a task in the same project");
    }
    if (assigneeId.isPresent() && assigneeId.get().equals(referenceTask.getCreator())) {
      throw new IllegalArgumentException("A reviewer cannot review their own task");
    }
  }

  @Override
  public void save(UUID taskId, JsonNode details) throws Exception {
    UUID referenceTaskId = UUID.fromString(details.get("referenceTaskId").textValue());
    repository.save(new Review(taskId, referenceTaskId));
  }

  @Override
  public Optional<Review> getByTaskId(UUID taskId) throws Exception {
    return repository.findAll().stream().filter(review -> taskId.equals(review.getTaskId()))
        .findFirst();
  }

  private Task findReferenceTask(JsonNode details) {
    UUID referenceTaskId;
    try {
      referenceTaskId = UUID.fromString(details.path("referenceTaskId").asText());
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("referenceTaskId must be a valid UUID", exception);
    }
    return taskRepository.findById(referenceTaskId).orElseThrow(
        () -> new IllegalArgumentException("Referenced task was not found"));
  }
}
