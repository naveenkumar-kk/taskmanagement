package com.tutorial.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tutorial.model.Bug;
import com.tutorial.model.Category;
import com.tutorial.model.Identifiable;
import com.tutorial.model.Notification;
import com.tutorial.model.Priority;
import com.tutorial.model.Project;
import com.tutorial.model.Severity;
import com.tutorial.model.Task;
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

class TaskServiceCreationTest {

  @Test
  void createsUnassignedBugAndRecordsCreationMessageWithNoRecipient() throws Exception {
    Fixture fixture = new Fixture();

    Task task = fixture.service.save(fixture.project.getId(), Priority.HIGH, Category.BUG,
        fixture.creator.getId(), "Description", "Bug", Optional.empty(),
        "{\"severity\":\"HIGH\",\"reproductionSteps\":\"Reproduce it\"}");

    Notification notification = fixture.notifier.getNotifications().getFirst();
    assertEquals(task.getId(), notification.getTaskId());
    assertTrue(notification.getRecipientId().isEmpty());
    assertTrue(notification.getMessage().contains("has been created"));
    assertEquals(1, fixture.tasks.findAll().size());
    assertEquals(1, fixture.bugs.findAll().size());
  }

  @Test
  void rejectsBlankBugStepsBeforeSavingOrNotifying() throws Exception {
    Fixture fixture = new Fixture();

    assertThrows(IllegalArgumentException.class, () -> fixture.service.save(
        fixture.project.getId(), Priority.HIGH, Category.BUG, fixture.creator.getId(),
        "Description", "Bad bug", Optional.empty(),
        "{\"severity\":\"HIGH\",\"reproductionSteps\":\"   \"}"));

    assertTrue(fixture.tasks.findAll().isEmpty());
    assertTrue(fixture.bugs.findAll().isEmpty());
    assertTrue(fixture.notifier.getNotifications().isEmpty());
  }

  private static class Fixture {

    private final InMemoryRepository<Task> tasks = new InMemoryRepository<>();
    private final InMemoryRepository<Project> projects = new InMemoryRepository<>();
    private final InMemoryRepository<User> users = new InMemoryRepository<>();
    private final InMemoryRepository<Bug> bugs = new InMemoryRepository<>();
    private final RecordingTaskNotifier notifier = new RecordingTaskNotifier();
    private final User creator = new User("Creator", "creator@example.com");
    private final Project project = new Project("Project", "Description", Optional.empty());
    private final TaskService service;

    private Fixture() throws IOException {
      creator.setId();
      project.setId();
      users.save(creator);
      projects.save(project);
      BugTaskHandler bugHandler = new BugTaskHandler(bugs);
      service = new TaskService(tasks, new ProjectService(projects), new UserService(users),
          Map.of(Category.BUG, bugHandler), null, notifier, null);
    }
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
      entities.put(id, entity);
    }

    @Override
    public void delete(UUID id) {
      entities.remove(id);
    }
  }
}
