package com.tutorial.service;

import java.util.Optional;
import java.util.UUID;

public interface TaskCategoryReader<T> {

  Optional<T> getByTaskId(UUID taskId) throws Exception;
}
