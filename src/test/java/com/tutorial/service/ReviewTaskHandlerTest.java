package com.tutorial.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.model.Identifiable;
import com.tutorial.model.Review;
import com.tutorial.model.Task;
import com.tutorial.model.Category;
import com.tutorial.model.Priority;
import com.tutorial.model.TaskStatus;
import com.tutorial.repository.CrudRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ReviewTaskHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void rejectReviewWhenReferenceTaskIdIsMissing() throws Exception {
    ReviewTaskHandler handler = newHandler();

    JsonNode details = objectMapper.readTree("{}");
    Exception exception = assertThrows(
        Exception.class,
        () -> handler.validate(details)
    );

    assertEquals("A referenceTaskId is required for a review",
        exception.getMessage()
    );
  }

  @Test
  void rejectsReviewWhenReferenceTaskDoesNotExist() throws Exception {
    ReviewTaskHandler handler = newHandler();
    UUID unknownTaskId = UUID.randomUUID();

    JsonNode details = objectMapper.readTree(
        "{\"referenceTaskId\":\"" + unknownTaskId + "\"}"
    );

    Exception exception = assertThrows(
        Exception.class,
        () -> handler.validate(details)
    );

    assertEquals("Referenced task was not found", exception.getMessage());
  }

  @Test
  void rejectsReferenceTaskFromAnotherProject() throws Exception {
    UUID projectId = UUID.randomUUID();
    Task referenceTask = task(projectId, UUID.randomUUID());
    InMemoryRepository<Task> taskRepository = new InMemoryRepository<>();
    taskRepository.save(referenceTask);
    ReviewTaskHandler handler = new ReviewTaskHandler(
        new InMemoryRepository<>(), taskRepository);
    JsonNode details = objectMapper.readTree(
        "{\"referenceTaskId\":\"" + referenceTask.getId() + "\"}"
    );

    assertThrows(IllegalArgumentException.class,
        () -> handler.validateForCreation(details, UUID.randomUUID(), Optional.empty()));
  }

  @Test
  void rejectsReferencedTaskCreatorAsReviewer() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    Task referenceTask = task(projectId, creatorId);
    InMemoryRepository<Task> taskRepository = new InMemoryRepository<>();
    taskRepository.save(referenceTask);
    ReviewTaskHandler handler = new ReviewTaskHandler(
        new InMemoryRepository<>(), taskRepository);
    JsonNode details = objectMapper.readTree(
        "{\"referenceTaskId\":\"" + referenceTask.getId() + "\"}"
    );

    assertThrows(IllegalArgumentException.class,
        () -> handler.validateForCreation(details, projectId, Optional.of(creatorId)));
  }

  private Task task(UUID projectId, UUID creatorId) {
    Task task = new Task("Referenced task", "Description", Category.FEATURE,
        Priority.MEDIUM, TaskStatus.DONE, projectId, creatorId, Optional.empty());
    task.setId();
    return task;
  }

  private ReviewTaskHandler newHandler() {
    InMemoryRepository<Review> reviewRepository = new InMemoryRepository<>();
    InMemoryRepository<Task> taskRepository = new InMemoryRepository<>();
    return new ReviewTaskHandler(reviewRepository, taskRepository);
  }

  private static class InMemoryRepository<T extends Identifiable>
      implements CrudRepository<T> {

    private final Map<UUID, T> entities = new LinkedHashMap<>();

    @Override
    public void save(T entity) throws IOException {
      if (entity.getId() == null) {
        entity.setId();
      }
      entities.put(entity.getId(), entity);
    }

    @Override
    public Optional<T> findById(UUID id) {
      return Optional.ofNullable(entities.get(id));
    }

    @Override
    public List<T> findAll() {
      return new ArrayList<>(entities.values());
    }

    @Override
    public void update(UUID id, T entity) {
      entities.put(id, entity);
    }

    @Override
    public void delete(UUID id) {
      entities.remove(id);
    }
  }

}
