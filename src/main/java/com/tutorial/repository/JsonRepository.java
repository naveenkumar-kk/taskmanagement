package com.tutorial.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tutorial.model.BaseEntity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JsonRepository<T extends BaseEntity> implements CrudRepository<T> {

  private final File file;
  private final ObjectMapper objectMapper;
  private final Class<T> clazz;

  public JsonRepository(String filepath, Class<T> clazz) {
    this(new File(filepath), clazz);
  }

  public JsonRepository(File filepath, Class<T> clazz) {
    this.file = new File(filepath.toURI());
    this.objectMapper = new ObjectMapper().registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT);
    this.clazz = clazz;
    initialise();
  }


  @Override
  public synchronized void save(T entity) {

    if (entity.getId() == null) {
      entity.setId();
    }
    List<T> entities = findAll();
    boolean exists = entities.stream().anyMatch(e -> Objects.equals(e.getId(), entity.getId()));
    if (exists) {
      throw new IllegalArgumentException("Entity with id " + entity.getId() + " already exists");
    }
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(Instant.now());
    }
    entities.add(entity);
    write(entities);
  }


  @Override
  public Optional<T> findById(UUID id) {
    return findAll().stream().filter(e -> Objects.equals(e.getId(), id)).findFirst();
  }

  @Override
  public List<T> findAll() {
    try {
      JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
      return objectMapper.readValue(file, listType);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void update(UUID id, T entity) {
    List<T> entities = findAll();

    for (int i = 0; i < entities.size(); i++) {
      if (Objects.equals(entities.get(i).getId(), id)) {
        entities.set(i, entity);
        write(entities);
        return;
      }
    }

    throw new NoSuchElementException("Entity with id " + id + " does not exist");
  }

  @Override
  public void delete(UUID id) {
    List<T> entities = findAll();
    boolean deleted = entities.removeIf(e -> Objects.equals(e.getId(), id));
    if (!deleted) {
      throw new NoSuchElementException("Entity with id " + id + " does not exist");
    }
    write(entities);
  }

  private void initialise() {
    try {
      Path path = file.toPath();
      Path parent = path.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      if (!file.exists() || file.length() == 0L) {
        objectMapper.writeValue(file, new ArrayList<>());
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void write(List<T> entities) {
    try {
      objectMapper.writeValue(file, entities);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
