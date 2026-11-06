package com.tutorial.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tutorial.model.Project;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonRepositoryCompatibilityTest {

  @TempDir
  Path temporaryDirectory;

  @Test
  void readsProjectMembersWithLegacyUserProjectIdProperty() throws Exception {
    Path file = temporaryDirectory.resolve("projects.json");
    Files.writeString(file, """
        [{
          "id": "e8bc8bce-29e2-4eaa-8f27-1b0a17b7f87b",
          "title": "Project",
          "members": [{
            "id": "c0b4ff67-0399-47ae-9de2-79c68ce43d12",
            "username": "member",
            "email": "member@example.com",
            "projectId": ["e8bc8bce-29e2-4eaa-8f27-1b0a17b7f87b"]
          }]
        }]
        """);

    JsonRepository<Project> repository = new JsonRepository<>(file.toFile(), Project.class);

    assertEquals("member", repository.findAll().getFirst().getMembers().getFirst().getUsername());
  }
}
