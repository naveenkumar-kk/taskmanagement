package com.tutorial;

import com.tutorial.model.Bug;
import com.tutorial.model.Category;
import com.tutorial.model.Feature;
import com.tutorial.model.Notification;
import com.tutorial.model.Priority;
import com.tutorial.model.Project;
import com.tutorial.model.Review;
import com.tutorial.model.Task;
import com.tutorial.model.TaskTransition;
import com.tutorial.model.User;
import com.tutorial.repository.CrudRepository;
import com.tutorial.repository.JsonRepository;
import com.tutorial.service.BugTaskHandler;
import com.tutorial.service.ConsoleTaskNotifier;
import com.tutorial.service.FeatureTaskHandler;
import com.tutorial.service.NotificationService;
import com.tutorial.service.ProjectService;
import com.tutorial.service.ReviewTaskHandler;
import com.tutorial.service.TaskAssignmentService;
import com.tutorial.service.TaskCategoryHandler;
import com.tutorial.service.TaskCompletionPolicy;
import com.tutorial.service.TaskNotifier;
import com.tutorial.service.TaskService;
import com.tutorial.service.TaskTransitionService;
import com.tutorial.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Main {

  static void main(String[] args) throws Exception {
    CrudRepository<Task> taskRepository =
        new JsonRepository<>("data/tasks.json", Task.class);
    CrudRepository<Project> projectRepository =
        new JsonRepository<>("data/projects.json", Project.class);
    CrudRepository<User> userRepository =
        new JsonRepository<>("data/user.json", User.class);
    CrudRepository<Feature> featureRepository =
        new JsonRepository<>("data/feature.json", Feature.class);
    CrudRepository<Bug> bugRepository =
        new JsonRepository<>("data/bug.json", Bug.class);
    CrudRepository<Review> reviewRepository =
        new JsonRepository<>("data/review.json", Review.class);
    CrudRepository<TaskTransition> taskTransitionRepository =
        new JsonRepository<>("data/task-transitions.json", TaskTransition.class);
    CrudRepository<Notification> notificationRepository = new JsonRepository<>(
        "data/notification.json", Notification.class);

    FeatureTaskHandler featureHandler = new FeatureTaskHandler(featureRepository);
    BugTaskHandler bugHandler = new BugTaskHandler(bugRepository);
    ReviewTaskHandler reviewHandler =
        new ReviewTaskHandler(reviewRepository, taskRepository);

    ProjectService projectService = new ProjectService(projectRepository);
    UserService userService = new UserService(userRepository);
    NotificationService notificationService = new NotificationService(notificationRepository);
    TaskNotifier consoleNotifier = new ConsoleTaskNotifier(notificationService);

    Map<Category, TaskCategoryHandler> handlers = Map.of(
        Category.FEATURE, featureHandler,
        Category.BUG, bugHandler,
        Category.REVIEW, reviewHandler
    );
    Map<Category, TaskCompletionPolicy> taskCompletionPolicyMap = Map.of(
        Category.FEATURE, featureHandler,
        Category.BUG, bugHandler
    );
    TaskService taskService = new TaskService(
        taskRepository,
        projectService,
        userService,
        handlers,
        new TaskTransitionService(taskRepository, taskTransitionRepository,
            taskCompletionPolicyMap, consoleNotifier),
        consoleNotifier,
        new TaskAssignmentService(userService, projectService, taskRepository,
            reviewHandler, consoleNotifier)
    );

    String runId = UUID.randomUUID().toString();

    System.out.println("=== Create project and user ===");
    Project project = projectService.save(
        "Demo project " + runId,
        null,
        Optional.empty()
    );

    UUID projectId = project.getId();
    User user = new User("Demo user", "demo@example.com");
    user.setId();
    userRepository.save(user);
    User reviewer = new User("Demo reviewer", "reviewer@example.com");
    reviewer.setId();
    userRepository.save(reviewer);

    Project updatedProject = projectService.updateProject(
        projectId,
        Optional.empty(),
        Optional.empty(),
        Optional.of(List.of(user, reviewer))
    ).orElseThrow();

    System.out.println("Project created: " + project.getTitle());
    System.out.println("Project members: " + updatedProject.getMembers().size());
    projectService.findById(projectId).ifPresent(
        found -> System.out.println("Project lookup: " + found.getTitle())
    );
    System.out.println("Number of projects: " + projectService.findAll().size());
    projectService.findByTitle(project.getTitle()).ifPresent(
        found -> System.out.println("Project found by title: " + found.getTitle())
    );
    userService.findById(user.getId()).ifPresent(
        found -> System.out.println("User lookup: " + found.getUsername())
    );

    System.out.println("\n=== Create tasks ===");
    String featureTitle = "Demo feature " + runId;
    String bugTitle = "Demo bug " + runId;
    String reviewTitle = "Demo review " + runId;

    Task feature = taskService.save(
        projectId,
        Priority.MEDIUM,
        Category.FEATURE,
        user.getId(),
        "Feature description",
        featureTitle,
        Optional.empty(),
        "{\"acceptanceCriteria\":\"- [x] Feature works\"}"
    );

    Task bug = taskService.save(
        projectId,
        Priority.HIGH,
        Category.BUG,
        user.getId(),
        "Bug description",
        bugTitle,
        Optional.empty(),
        "{\"severity\":\"MEDIUM\",\"reproductionSteps\":\"Steps to reproduce\"}"
    );

    showRejected("start an unassigned bug", () -> taskService.startTask(bug.getId()));
    showRejected("create a review of the creator's own task", () ->
        taskService.save(
            projectId,
            Priority.LOW,
            Category.REVIEW,
            user.getId(),
            "Invalid self-review",
            "Demo rejected review " + runId,
            Optional.of(user.getId()),
            "{\"referenceTaskId\":\"" + feature.getId() + "\"}"
        ));

    Task review = taskService.save(
        projectId,
        Priority.LOW,
        Category.REVIEW,
        user.getId(),
        "Review description",
        reviewTitle,
        Optional.empty(),
        "{\"referenceTaskId\":\"" + feature.getId() + "\"}"
    );

    User outsider = new User("Outsider", "outsider@example.com");
    outsider.setId();
    userRepository.save(outsider);
    showRejected("assign a task to a non-member", () ->
        taskService.assignTask(bug.getId(), outsider.getId()));

    System.out.println("Created feature: " + featureTitle);
    System.out.println("Created bug: " + bugTitle);
    System.out.println("Created review: " + reviewTitle);

    System.out.println("\n=== Assign and update task statuses ===");
    taskService.assignTask(feature.getId(), user.getId());
    taskService.assignTask(bug.getId(), user.getId());
    taskService.assignTask(review.getId(), reviewer.getId());

    taskService.startTask(feature.getId());
    taskService.completeTask(feature.getId(), null);

    taskService.startTask(bug.getId());
    taskService.blockTask(bug.getId(), "Waiting for verification");
    taskService.resumeTask(bug.getId());
    taskService.completeTask(bug.getId(), "Fixed and verified");

    taskService.startTask(review.getId());
    taskService.completeTask(review.getId(), null);

    showRejected("reassign a completed task", () ->
        taskService.assignTask(feature.getId(), reviewer.getId()));

    Set<UUID> demoTaskIds = Set.of(feature.getId(), bug.getId(), review.getId());
    List<Task> demoTasks = taskService.findAll().stream()
        .filter(task -> demoTaskIds.contains(task.getId()))
        .toList();

    printTasks("\n=== Tasks created during this run ===", demoTasks);
    taskService.findById(feature.getId()).ifPresent(
        found -> printTasks("\n=== Find task by ID ===", List.of(found))
    );
    taskService.findByTitle(featureTitle).ifPresent(
        found -> printTasks("\n=== Find task by title ===", List.of(found))
    );
    printTasks("\n=== Tasks with MEDIUM priority ===",
        taskService.findAllByPriority(Priority.MEDIUM));
    System.out.println("\n=== Task counts by status (all saved tasks) ===");
    taskService.getCountByStatus().forEach(
        (status, count) -> System.out.println(status + ": " + count)
    );
    printTasks("\n=== BUG tasks ===",
        taskService.findByFilters(Map.of("category", "BUG")));
    printTasks("\n=== Tasks in this project ===",
        taskService.findByProjectId(projectId).orElse(List.of()));

    System.out.println("\n=== Notification records saved ===");
    notificationRepository.findAll().stream()
        .filter(notification -> demoTaskIds.contains(notification.getTaskId()))
        .forEach(Main::printNotification);
  }

  private static void showRejected(String operation, CheckedOperation action) {
    try {
      action.run();
      System.out.println("Unexpectedly accepted: " + operation);
    } catch (Exception exception) {
      System.out.println("Rejected " + operation + ": " + exception.getMessage());
    }
  }

  private static void printTasks(String heading, List<Task> tasks) {
    System.out.println(heading);
    if (tasks.isEmpty()) {
      System.out.println("No tasks found.");
      return;
    }
    tasks.forEach(Main::printTask);
  }

  private static void printTask(Task task) {
    System.out.printf("%s | %s | %s | %s | assignee: %s%n",
        task.getId(),
        task.getTitle(),
        task.getCategory(),
        task.getTaskStatus(),
        task.getAssignee() == null ? "none" : task.getAssignee());
  }

  private static void printNotification(Notification notification) {
    String recipient = notification.getRecipientId()
        .map(UUID::toString)
        .orElse("none");
    System.out.printf("Task %s | recipient: %s | %s%n",
        notification.getTaskId(), recipient, notification.getMessage());
  }

  @FunctionalInterface
  private interface CheckedOperation {

    void run() throws Exception;
  }
}
