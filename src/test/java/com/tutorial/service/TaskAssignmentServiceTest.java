package com.tutorial.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tutorial.model.Bug;
import com.tutorial.model.Category;
import com.tutorial.model.Identifiable;
import com.tutorial.model.Priority;
import com.tutorial.model.Project;
import com.tutorial.model.Review;
import com.tutorial.model.Severity;
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

class TaskAssignmentServiceTest {

  @Test
  void rejectsAssigneeWhoIsNotAProjectMemberWithoutChangingTaskOrNotifying() throws Exception {
    UUID projectId = UUID.randomUUID();
    User outsider = user("outsider");
    Task task = task(Category.BUG, projectId, UUID.randomUUID(), TaskStatus.TODO,
        Optional.empty());
    Fixture fixture = new Fixture(projectId, List.of(), List.of(outsider), List.of(task),
        List.of(new Bug(task.getId(), Severity.MEDIUM, "Steps to reproduce")));

    assertThrows(IllegalArgumentException.class,
        () -> fixture.service.updateAssignee(task.getId(), outsider.getId()));

    assertNull(fixture.tasks.findById(task.getId()).orElseThrow().getAssignee());
    assertTrue(fixture.notifier.getNotifications().isEmpty());
  }

  @Test
  void assignsProjectMemberAndNotifiesAfterSuccessfulChange() throws Exception {
    UUID projectId = UUID.randomUUID();
    User member = user("member");
    Task task = task(Category.BUG, projectId, UUID.randomUUID(), TaskStatus.TODO,
        Optional.empty());
    Fixture fixture = new Fixture(projectId, List.of(member), List.of(member), List.of(task),
        List.of(new Bug(task.getId(), Severity.MEDIUM, "Steps to reproduce")));

    fixture.service.updateAssignee(task.getId(), member.getId());

    assertEquals(member.getId(), fixture.tasks.findById(task.getId()).orElseThrow().getAssignee());
    assertEquals(List.of(task.getId()), fixture.notifier.getAssignedTaskIds());
    assertEquals(List.of(member.getId()), fixture.notifier.getAssignedUserIds());
  }

  @Test
  void rejectsReassignmentAfterCompletionWithoutNotifying() throws Exception {
    UUID projectId = UUID.randomUUID();
    User firstMember = user("first");
    User secondMember = user("second");
    Task task = task(Category.BUG, projectId, firstMember.getId(), TaskStatus.DONE,
        Optional.of(firstMember.getId()));
    Fixture fixture = new Fixture(projectId, List.of(firstMember, secondMember),
        List.of(firstMember, secondMember), List.of(task),
        List.of(new Bug(task.getId(), Severity.MEDIUM, "Steps to reproduce")));

    assertThrows(IllegalStateException.class,
        () -> fixture.service.updateAssignee(task.getId(), secondMember.getId()));

    assertEquals(firstMember.getId(), fixture.tasks.findById(task.getId()).orElseThrow()
        .getAssignee());
    assertTrue(fixture.notifier.getNotifications().isEmpty());
  }

  @Test
  void reviewerCannotBeTheCreatorOfTheReferencedTask() throws Exception {
    UUID projectId = UUID.randomUUID();
    User creator = user("creator");
    User anotherMember = user("reviewer");
    Task referencedTask = task(Category.FEATURE, projectId, creator.getId(), TaskStatus.DONE,
        Optional.empty());
    Task reviewTask = task(Category.REVIEW, projectId, creator.getId(), TaskStatus.TODO,
        Optional.empty());
    Review review = new Review(reviewTask.getId(), referencedTask.getId());
    Fixture fixture = new Fixture(projectId, List.of(creator, anotherMember),
        List.of(creator, anotherMember), List.of(referencedTask, reviewTask), List.of(review));

    assertThrows(IllegalArgumentException.class,
        () -> fixture.service.updateAssignee(reviewTask.getId(), creator.getId()));

    assertNull(fixture.tasks.findById(reviewTask.getId()).orElseThrow().getAssignee());
    assertTrue(fixture.notifier.getNotifications().isEmpty());
  }

  private static User user(String name) {
    User user = new User(name, name + "@example.com");
    user.setId();
    return user;
  }

  private static Task task(Category category, UUID projectId, UUID creator, TaskStatus status,
      Optional<UUID> assignee) {
    Task task = new Task("Task", "Description", category, Priority.MEDIUM, status,
        projectId, creator, assignee);
    task.setId();
    return task;
  }

  private static class Fixture {

    private final InMemoryRepository<Project> projects = new InMemoryRepository<>();
    private final InMemoryRepository<User> users = new InMemoryRepository<>();
    private final InMemoryRepository<Task> tasks = new InMemoryRepository<>();
    private final InMemoryRepository<Review> reviews = new InMemoryRepository<>();
    private final RecordingTaskNotifier notifier = new RecordingTaskNotifier();
    private final TaskAssignmentService service;

    private Fixture(UUID projectId, List<User> projectMembers, List<User> savedUsers,
        List<Task> savedTasks, List<? extends Identifiable> taskDetails) throws IOException {
      Project project = new Project("Project", null, Optional.of(projectMembers));
      project.setId(projectId);
      projects.save(project);
      for (User user : savedUsers) {
        users.save(user);
      }
      for (Task task : savedTasks) {
        tasks.save(task);
      }
      for (Identifiable detail : taskDetails) {
        if (detail instanceof Review review) {
          reviews.save(review);
        }
      }
      service = new TaskAssignmentService(
          new UserService(users),
          new ProjectService(projects),
          tasks,
          new ReviewTaskHandler(reviews, tasks),
          notifier
      );
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
        throw new IllegalArgumentException("Duplicate ID");
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
