# Design decisions

1. The runnable example uses JSON files through JsonRepository; tests use in-memory repository
   fakes.
2. Feature, bug, and review details are handled by separate category handlers. Feature and bug
   completion checks live with their respective handlers.
3. Lifecycle and assignment rules are coordinated by services. Task state operations return new task
   values, and task fields do not have public setters, so query results cannot directly mutate the
   stored task.
4. TaskNotifier is injected into services. The console implementation prints notifications for the
   demo, while the recording implementation is used by tests.
5. Users are equal by UUID identity. Project membership is held by the project, deduplicated by that
   identity, and exposed as an immutable list.

