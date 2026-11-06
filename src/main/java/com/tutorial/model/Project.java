package com.tutorial.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public class Project extends BaseEntity {


  private String title;
  private String description;
  private List<User> members;
  private Instant updatedAt;

  @JsonCreator
  public Project(@JsonProperty("title") String title,
      @JsonProperty("description") String description,
      @JsonProperty("members") Optional<List<User>> members) {
    this.members = new ArrayList<>(
        new LinkedHashSet<>(members == null ? List.of() : members.orElse(List.of()))
    );
    this.title = title;
    this.description = description;
  }

  public List<User> getMembers() {
    return List.copyOf(members);
  }

  public void setMembers(List<User> members) {
    this.members = new ArrayList<>(new LinkedHashSet<>(members));
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Instant getUpdatedAt() {
    return this.updatedAt;
  }

  public void setUpdatedAt() {
    this.updatedAt = Instant.now();
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
