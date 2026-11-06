# Team Task Tracker

A plain Java task tracker with projects, members, feature/bug/review tasks, task
lifecycles, JSON file storage, notifications, and automated tests.

## Requirements

- JDK 25
- Maven installed and available as `mvn` for the terminal commands below

## Run from IntelliJ IDEA

1. Open this project or its `pom.xml` in IntelliJ IDEA and let Maven import the
   dependencies.
2. Set the project SDK to JDK 25.
3. Open `src/main/java/com/tutorial/Main.java`.
4. Run `Main.main()`.

The demo prints task results, rejected operations, and notifications to the
console. It creates or updates JSON files under `data/`; running it again adds
another demo set to those local files. The `data/` directory is excluded from
Git.

## Run from a terminal

Run the automated tests:

```bash
mvn test
```

Run the console demonstration:

```bash
mvn compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=com.tutorial.Main
```

Run one test class:

```bash
mvn -Dtest=TaskLifeCycleTest test
```
