package com.tutorial.repository;

import com.tutorial.model.Identifiable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrudRepository<T extends Identifiable> {

  void save(T entity) throws IOException;

  Optional<T> findById(UUID id);


  List<T> findAll();

  void update(UUID id, T entity);

  void delete(UUID id);

}
