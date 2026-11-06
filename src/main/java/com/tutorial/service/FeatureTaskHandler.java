package com.tutorial.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tutorial.helper.MarkDownChecklistValidator;
import com.tutorial.model.Category;
import com.tutorial.model.Feature;
import com.tutorial.model.Task;
import com.tutorial.repository.CrudRepository;
import java.util.Optional;
import java.util.UUID;

public class FeatureTaskHandler implements TaskCategoryHandler,
    TaskCompletionPolicy,
    TaskCategoryReader<Feature> {

  private final CrudRepository<Feature> repository;

  public FeatureTaskHandler(CrudRepository<Feature> repository) {
    this.repository = repository;
  }

  @Override
  public boolean supports(Category category) {
    return category == Category.FEATURE;
  }

  @Override
  public void validate(JsonNode details) throws Exception {
    if (details == null || !details.isObject() || !details.has("acceptanceCriteria")) {
      throw new Exception("Invalid details for acceptanceCriteria");
    }
    String criteria = details.get("acceptanceCriteria").asText("");

    if (!MarkDownChecklistValidator.hasCheckBox(criteria)) {
      throw new IllegalArgumentException("Acceptance Criteria should be a checkbox");
    }
  }

  @Override
  public void save(UUID taskId, JsonNode details) throws Exception {
    String criteria = details.get("acceptanceCriteria").asText();
    repository.save(new Feature(taskId, criteria));
  }

  @Override
  public Optional<Feature> getByTaskId(UUID taskId) throws Exception {
    return repository.findAll().stream()
        .filter(feature -> taskId.equals(feature.getTaskId()))
        .findFirst();
  }

  @Override
  public void complete(Task task, Optional<String> details) throws Exception {
    boolean allCriteriaChecked = getByTaskId(task.getId())
        .map(feature ->
            MarkDownChecklistValidator.areAllChecked(feature.getAcceptanceCriteria()))
        .orElse(false);

    if (!allCriteriaChecked) {
      throw new IllegalStateException(
          "Cannot complete the feature task: acceptance criteria are incomplete.");
    }

  }

}
