package com.tutorial.service;

import com.tutorial.model.Task;
import java.util.Optional;

public interface TaskCompletionPolicy {

  void complete(Task task, Optional<String> details) throws Exception;

}
