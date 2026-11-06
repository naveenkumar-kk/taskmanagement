package com.tutorial.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tutorial.model.Category;
import com.tutorial.model.Identifiable;
import com.tutorial.model.Priority;
import com.tutorial.model.Project;
import com.tutorial.model.Task;
import com.tutorial.model.TaskStatus;
import com.tutorial.model.User;
import com.tutorial.repository.CrudRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskServiceQueryTest {

  @Test
  void combinesFiltersAndReturnsUnmodifiableResults() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Task matching = task(projectId, userId, Priority.HIGH, TaskStatus.IN_PROGRESS,
        Category.BUG, "Match this");
    Task wrongStatus = task(projectId, userId, Priority.HIGH, TaskStatus.TODO,
        Category.BUG, "Another");
    Task wrongAssignee = task(projectId, UUID.randomUUID(), Priority.HIGH,
        TaskStatus.IN_PROGRESS, Category.BUG, "Third");
    InMemoryRepository<Task> tasks = new InMemoryRepository<>();
    tasks.save(matching);
    tasks.save(wrongStatus);
    tasks.save(wrongAssignee);
    TaskService service = service(tasks);

    List<Task> result = service.findByFilters(Map.of(
        "priority", "HIGH",
        "status", "IN_PROGRESS",
        "assignee", userId.toString(),
        "category", "BUG"
    ));

    assertEquals(List.of(matching), result);
    assertThrows(UnsupportedOperationException.class, result::clear);
    assertThrows(UnsupportedOperationException.class, service.findAll()::clear);
    assertThrows(UnsupportedOperationException.class,
        () -> service.getCountByStatus().put(TaskStatus.DONE, 100L));
  }

  @Test
  void unknownTaskIdReturnsEmptyOptionalAndProjectResultsCannotBeEdited() throws Exception {
    UUID projectId = UUID.randomUUID();
    InMemoryRepository<Task> tasks = new InMemoryRepository<>();
    Task task = task(projectId, UUID.randomUUID(), Priority.LOW, TaskStatus.TODO,
        Category.FEATURE, "Feature");
    tasks.save(task);
    TaskService service = service(tasks);

    assertFalse(service.findById(UUID.randomUUID()).isPresent());
    List<Task> projectTasks = service.findByProjectId(projectId).orElseThrow();
    assertEquals(List.of(task), projectTasks);
    assertThrows(UnsupportedOperationException.class, projectTasks::clear);
  }

  @Test
  void repositoryRejectsDuplicateTaskIds() throws Exception {
    InMemoryRepository<Task> tasks = new InMemoryRepository<>();
    Task first = task(UUID.randomUUID(), UUID.randomUUID(), Priority.LOW, TaskStatus.TODO,
        Category.BUG, "First");
    Task duplicateId = new Task("Second", "Description", Category.BUG, Priority.LOW,
        TaskStatus.TODO, UUID.randomUUID(), UUID.randomUUID(), Optional.empty());
    duplicateId.setId(first.getId());
    tasks.save(first);

    assertThrows(IllegalArgumentException.class, () -> tasks.save(duplicateId));
  }

  @Test
  void callingTaskOperationsOnAnExposedTaskDoesNotChangeTheStoredTask() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();
    InMemoryRepository<Task> tasks = new InMemoryRepository<>();
    Task task = task(projectId, assigneeId, Priority.MEDIUM, TaskStatus.TODO,
        Category.FEATURE, "Feature");
    tasks.save(task);
    TaskService service = service(tasks);
    Task exposedTask = service.findById(task.getId()).orElseThrow();

    Task startedCopy = exposedTask.start();

    assertEquals(TaskStatus.TODO, exposedTask.getTaskStatus());
    assertEquals(TaskStatus.TODO,
        service.findById(task.getId()).orElseThrow().getTaskStatus());
    assertEquals(TaskStatus.IN_PROGRESS, startedCopy.getTaskStatus());
  }

  private static TaskService service(CrudRepository<Task> tasks) {
    return new TaskService(tasks, new ProjectService(new InMemoryRepository<>()),
        new UserService(new InMemoryRepository<>()), Map.of(), null,
        new RecordingTaskNotifier(), null);
  }

  private static Task task(UUID projectId, UUID assigneeId, Priority priority,
      TaskStatus status, Category category, String title) {
    Task task = new Task(title, "Description", category, priority, status, projectId,
        UUID.randomUUID(), Optional.of(assigneeId));
    task.setId();
    return task;
  }

  private static class InMemoryRepository<T extends Identifiable> implements CrudRepository<T> {

    private final Map<UUID, T> entities = new LinkedHashMap<>();

    @Override
    public void save(T entity) throws IOException {
      if (entity.getId() == null) {
        entity.setId();
      }
      if (entities.containsKey(entity.getId())) {
        throw new IllegalArgumentException("Duplicate ID: " + entity.getId());
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
      if (!entities.containsKey(id)) {
        throw new IllegalArgumentException("Unknown ID: " + id);
      }
      entities.put(id, entity);
    }

    @Override
    public void delete(UUID id) {
      entities.remove(id);
    }
  }
}
