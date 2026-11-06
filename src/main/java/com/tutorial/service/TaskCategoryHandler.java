package com.tutorial.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tutorial.model.Category;
import java.util.UUID;
import java.util.Optional;

public interface TaskCategoryHandler {

  boolean supports(Category category);

  void validate(JsonNode details) throws Exception;

  default void validateForCreation(JsonNode details, UUID projectId,
      Optional<UUID> assigneeId) throws Exception {
    validate(details);
  }

  void save(UUID taskId, JsonNode details) throws Exception;

}
