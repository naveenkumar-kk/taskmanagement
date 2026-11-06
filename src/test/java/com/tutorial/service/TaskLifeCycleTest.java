package com.tutorial.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

public class TaskLifeCycleTest {

  private static final String CHECKED_CRITERIA = "- [x] Works as expected";

  @Test
  void bugCanCompleteDirectlyAfterStarting() throws Exception {
    assertSuccessfulLifecycle(Category.BUG, false);
  }

  @Test
  void bugCanCompleteAfterBeingBlockedAndResumed() throws Exception {
    assertSuccessfulLifecycle(Category.BUG, true);
  }

  @Test
  void featureCanCompleteDirectlyAfterStarting() throws Exception {
    assertSuccessfulLifecycle(Category.FEATURE, false);
  }

  @Test
  void featureCanCompleteAfterBeingBlockedAndResumed() throws Exception {
    assertSuccessfulLifecycle(Category.FEATURE, true);
  }

  @Test
  void reviewCanCompleteDirectlyAfterStarting() throws Exception {
    assertSuccessfulLifecycle(Category.REVIEW, false);
  }

  @Test
  void reviewCanCompleteAfterBeingBlockedAndResumed() throws Exception {
    assertSuccessfulLifecycle(Category.REVIEW, true);
  }


  private void assertSuccessfulLifecycle(Category category, boolean includeBlockedRoute)
      throws Exception {

    UUID assigneeId = UUID.randomUUID();
    Task task = new Task("Lifecycle test", "Description", category, Priority.MEDIUM,
        TaskStatus.TODO, UUID.randomUUID(), UUID.randomUUID(), Optional.of(assigneeId));
    task.setId();

    InMemoryRepository<Task> taskInMemoryRepository = new InMemoryRepository<>();
    taskInMemoryRepository.save(task);

    InMemoryRepository<TaskTransition> transitionInMemoryRepository = new InMemoryRepository<>();
    InMemoryRepository<Bug> bugInMemoryRepository = new InMemoryRepository<>();
    InMemoryRepository<Feature> featureInMemoryRepository = new InMemoryRepository<>();
    InMemoryRepository<Review> reviewInMemoryRepository = new InMemoryRepository<>();

    if (category == Category.BUG) {
      bugInMemoryRepository.save(new Bug(task.getId(), Severity.MEDIUM, "Steps to reproduce"));
    } else if (category == Category.FEATURE) {
      featureInMemoryRepository.save(new Feature(task.getId(), CHECKED_CRITERIA));
    } else {
      reviewInMemoryRepository.save(new Review(task.getId(), UUID.randomUUID()));
    }

    Map<Category, TaskCompletionPolicy> policyMap = new EnumMap<>(Category.class);
    policyMap.put(Category.BUG, new BugTaskHandler(bugInMemoryRepository));
    policyMap.put(Category.FEATURE, new FeatureTaskHandler(featureInMemoryRepository));

    RecordingTaskNotifier notifier = new RecordingTaskNotifier();
    TaskTransitionService service = new TaskTransitionService(
        taskInMemoryRepository, transitionInMemoryRepository, policyMap, notifier
    );

    service.updateStatus(task.getId(), TaskStatus.IN_PROGRESS, null, null);

    if (includeBlockedRoute) {
      service.updateStatus(
          task.getId(), TaskStatus.BLOCKED, "Waiting for dependency", null
      );
      service.updateStatus(task.getId(), TaskStatus.IN_PROGRESS, null, null);
    }

    String completionDetails = category == Category.BUG ? "Fixed and verified" : null;
    service.updateStatus(task.getId(), TaskStatus.DONE, null, completionDetails);
    assertEquals(
        TaskStatus.DONE,
        taskInMemoryRepository.findById(task.getId()).orElseThrow().getTaskStatus()
    );
    List<TaskStatus> expectedStatuses = includeBlockedRoute ? List.of(
        TaskStatus.IN_PROGRESS,
        TaskStatus.BLOCKED,
        TaskStatus.IN_PROGRESS,
        TaskStatus.DONE
    ) : List.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE);

    List<TaskStatus> recordedStatuses = transitionInMemoryRepository.findAll().stream()
        .map(TaskTransition::getToStatus)
        .toList();
    assertEquals(expectedStatuses, recordedStatuses);
    assertEquals(expectedStatuses, notifier.getTransitionStatuses());

    if (category == Category.BUG) {
      assertEquals("Fixed and verified",
          bugInMemoryRepository.findAll().getFirst().getResolution());
    } else if (category == Category.FEATURE) {
      assertEquals(task.getId(), featureInMemoryRepository.findAll().getFirst().getTaskId());
    } else {
      assertEquals(task.getId(), reviewInMemoryRepository.findAll().getFirst().getTaskId());
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
