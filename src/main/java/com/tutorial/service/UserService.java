package com.tutorial.service;

import com.tutorial.model.User;
import com.tutorial.repository.CrudRepository;
import java.util.Optional;
import java.util.UUID;

public class UserService {

  private final CrudRepository<User> repository;

  public UserService(CrudRepository<User> repository) {
    this.repository = repository;
  }

  public Optional<User> findById(UUID userId) {
    return repository.findById(userId);
  }
}
