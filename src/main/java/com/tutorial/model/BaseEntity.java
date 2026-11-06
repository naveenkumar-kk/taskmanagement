package com.tutorial.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public abstract class BaseEntity implements Identifiable {

  private UUID id;

  private Instant createdAt;

  @Override
  public final int hashCode() {
    return getClass().hashCode();
  }

  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public void setId() {
    this.id = UUID.randomUUID();
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaseEntity that = (BaseEntity) o;
    return id != null && id.equals(that.id);
  }
}
