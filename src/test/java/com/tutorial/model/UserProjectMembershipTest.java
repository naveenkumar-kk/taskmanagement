package com.tutorial.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserProjectMembershipTest {

  private static void copyId(BaseEntity source, BaseEntity target) throws Exception {
    Field idField = BaseEntity.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(target, source.getId());
  }

  @Test
  void usersWithTheSameIdAreEqual() throws Exception {
    User first = new User("naveen", "n@example.com");
    first.setId();

    User second = new User("naveen", "n@example.com");
    copyId(first, second);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void projectDoesNotKeepDuplicateMembers() throws Exception {
    User first = new User("naveen", "n@example.com");
    first.setId();

    User sameUserWithSameId = new User("naveen", "n@example.com");
    copyId(first, sameUserWithSameId);

    Project project = new Project(
        "Project",
        null,
        Optional.of(List.of(first, sameUserWithSameId))
    );

    assertEquals(1, project.getMembers().size());
  }

  @Test
  void callersCannotModifyExposedMembershipCollections() {
    User user = new User("naveen", "n@example.com");
    user.setId();
    List<User> sourceMembers = new ArrayList<>(List.of(user));

    Project project = new Project(
        "Project",
        null,
        Optional.of(sourceMembers)
    );
    sourceMembers.clear();

    List<User> exposedMembers = project.getMembers();

    assertThrows(
        UnsupportedOperationException.class,
        () -> exposedMembers.clear()
    );
    assertEquals(1, project.getMembers().size());
  }
}
