package com.tutorial.service;


import com.tutorial.model.Project;
import com.tutorial.model.User;
import com.tutorial.repository.CrudRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ProjectService {

  private final CrudRepository<Project> repository;


  public ProjectService(CrudRepository<Project> repository) {
    this.repository = repository;
  }

  public Optional<Project> findById(UUID id) {
    return repository.findById(id);
  }

  public List<Project> findAll() {
    return repository.findAll();
  }

  public Optional<Project> updateProject(UUID id, Optional<String> title,
      Optional<String> description,
      Optional<List<User>> members)
      throws IOException {
    try {
      Optional<Project> result = findById(id);

      if (result.isEmpty()) {
        return Optional.empty();
      }

      Project project = result.get();

      title.ifPresent(project::setTitle);
      description.ifPresent(project::setDescription);
      members.ifPresent(project::setMembers);
      project.setUpdatedAt();

      repository.update(id, project);
      return Optional.of(project);

    } catch (RuntimeException e) {
      throw new RuntimeException(e);
    }
  }

  public Optional<Project> findByTitle(String title) {
    try {
      return repository.findAll().stream().filter(project -> project.getTitle().equals(title))
          .findFirst();
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public Project save(String title, String description, Optional<List<User>> members)
      throws IOException {
    try {
      if (title == null || title.trim().isEmpty()) {
        throw new IllegalArgumentException("Title cannot be empty");
      }
      Project project = new Project(title, description, members);
      repository.save(project);
      return project;
    } catch (RuntimeException e) {
      throw new RuntimeException(e);
    }
  }

}
