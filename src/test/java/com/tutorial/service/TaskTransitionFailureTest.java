package com.tutorial.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tutorial.model.Bug;
import com.tutorial.model.Category;
import com.tutorial.model.Feature;
import com.tutorial.model.Identifiable;
import com.tutorial.model.Priority;
import com.tutorial.model.Review;
import com.tutorial.model.Severity;
import com.tutorial.model.Task;
import com.tutorial.model.TaskStatus;
import com.tutorial.model.TaskTransition;
import com.tutorial.repository.CrudRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TaskTransitionFailureTest {

  @Test
  void reviewCannotMoveDirectlyTodoToDone() throws IOException {
    Fixture fixture = new Fixture(Category.REVIEW, TaskStatus.TODO);

    assertThrows(IllegalStateException.class, () ->
        fixture.service.updateStatus(fixture.task.getId(), TaskStatus.DONE, null, null));
    assertEquals(TaskStatus.TODO, currentStatus(fixture));
    assertTrue(fixture.transitionInMemoryRepository.findAll().isEmpty());
    assertTrue(fixture.notifier.getNotifications().isEmpty());
  }

  @Test
  void bugCannotCompleteWithoutResolutionAndRemainsUnchanged() throws Exception {
    Fixture fixture = new Fixture(Category.BUG, TaskStatus.IN_PROGRESS);

    assertThrows(IllegalArgumentException.class, () ->
        fixture.service.updateStatus(fixture.task.getId(), TaskStatus.DONE, null, null));

    assertEquals(TaskStatus.IN_PROGRESS, currentStatus(fixture));
    assertNull(fixture.bugInMemoryRepository.findAll().getFirst().getResolution());
    assertTrue(fixture.transitionInMemoryRepository.findAll().isEmpty());
    assertTrue(fixture.notifier.getNotifications().isEmpty());
  }

  @Test
  void featureCannotCompleteWithUncheckedCriteriaAndRemainsUnchanged() throws Exception {
    Fixture fixture = new Fixture(Category.FEATURE, TaskStatus.IN_PROGRESS);

    assertThrows(IllegalStateException.class, () ->
        fixture.service.updateStatus(fixture.task.getId(), TaskStatus.DONE, null, null));

    assertEquals(TaskStatus.IN_PROGRESS, currentStatus(fixture));
    assertTrue(fixture.transitionInMemoryRepository.findAll().isEmpty());
    assertTrue(fixture.notifier.getNotifications().isEmpty());
  }

  @Test
  void cannotStartUnassignedTaskAndRejectedStartLeavesStateUnchanged() throws Exception {
    Fixture fixture = new Fixture(Category.BUG, TaskStatus.TODO);
    Task unassigned = new Task("Unassigned", "Task", Category.BUG, Priority.MEDIUM,
        TaskStatus.TODO, UUID.randomUUID(), UUID.randomUUID(), Optional.empty());
    unassigned.setId();
    fixture.taskInMemoryRepository.save(unassigned);
    fixture.bugInMemoryRepository.save(
        new Bug(unassigned.getId(), Severity.MEDIUM, "Reproduction steps"));

    assertThrows(IllegalStateException.class, () -> fixture.service.startTask(unassigned.getId()));

    assertEquals(TaskStatus.TODO, fixture.taskInMemoryRepository.findById(unassigned.getId())
        .orElseThrow().getTaskStatus());
    assertTrue(fixture.transitionInMemoryRepository.findAll().isEmpty());
    assertTrue(fixture.notifier.getNotifications().isEmpty());
  }

  @Test
  void cannotBlockWithoutReasonAndRejectedBlockLeavesStateUnchanged() throws Exception {
    Fixture fixture = new Fixture(Category.BUG, TaskStatus.IN_PROGRESS);

    assertThrows(IllegalArgumentException.class,
        () -> fixture.service.blockTask(fixture.task.getId(), "  "));

    assertEquals(TaskStatus.IN_PROGRESS, currentStatus(fixture));
    assertTrue(fixture.transitionInMemoryRepository.findAll().isEmpty());
    assertTrue(fixture.notifier.getNotifications().isEmpty());
  }

  private TaskStatus currentStatus(Fixture fixture) {
    return fixture.taskInMemoryRepository.findById(fixture.task.getId())
        .orElseThrow()
        .getTaskStatus();
  }

  private static class Fixture {

    private final InMemoryRepository<Task> taskInMemoryRepository = new InMemoryRepository<>();
    private final InMemoryRepository<TaskTransition> transitionInMemoryRepository = new InMemoryRepository<>();
    private final InMemoryRepository<Bug> bugInMemoryRepository = new InMemoryRepository<>();
    private final InMemoryRepository<Feature> featureInMemoryRepository = new InMemoryRepository<>();
    private final InMemoryRepository<Review> reviewInMemoryRepository = new InMemoryRepository<>();
    private final RecordingTaskNotifier notifier = new RecordingTaskNotifier();

    private final Task task;
    private final TaskTransitionService service;

    private Fixture(Category category, TaskStatus startingStatus) throws IOException {
      task = new Task(
          "Failure test",
          "Task for testing a failed transition",
          category,
          Priority.MEDIUM,
          startingStatus,
          UUID.randomUUID(),
          UUID.randomUUID(),
          Optional.of(UUID.randomUUID())
      );
      task.setId();
      taskInMemoryRepository.save(task);
      saveCategoryDetails(category);
      Map<Category, TaskCompletionPolicy> completionPolicyMap = new EnumMap<>(Category.class);
      completionPolicyMap.put(Category.BUG, new BugTaskHandler((bugInMemoryRepository)));
      completionPolicyMap.put(Category.FEATURE, new FeatureTaskHandler(featureInMemoryRepository));
      service = new TaskTransitionService(
          taskInMemoryRepository,
          transitionInMemoryRepository,
          completionPolicyMap,
          notifier
      );

    }

    private void saveCategoryDetails(Category category) throws IOException {
      if (category == Category.BUG) {
        bugInMemoryRepository.save(
            new Bug(task.getId(), Severity.MEDIUM, "Steps to reproduce")
        );
      } else if (category == Category.FEATURE) {
        featureInMemoryRepository.save(new Feature(task.getId(), "- [ ] Woeks as expected"));
      } else if (category == Category.REVIEW) {
        reviewInMemoryRepository.save(
            new Review(task.getId(), UUID.randomUUID())
        );
      }
    }
  }


  private static class InMemoryRepository<T extends Identifiable> implements CrudRepository<T> {

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
