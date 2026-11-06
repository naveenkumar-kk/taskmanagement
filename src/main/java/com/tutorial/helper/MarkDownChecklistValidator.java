package com.tutorial.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkDownChecklistValidator {

  private static final Pattern CHECKBOX_PATTERN = Pattern.compile(
      "^\\s*[-*+]\\s+\\[([ xX])\\](?=\\s|$)",
      Pattern.MULTILINE
  );

  public static boolean hasCheckBox(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return false;
    }
    return CHECKBOX_PATTERN.matcher(markdown).find();
  }

  public static boolean areAllChecked(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return false;
    }

    Matcher matcher = CHECKBOX_PATTERN.matcher(markdown);
    boolean foundCheckBox = false;
    while (matcher.find()) {
      foundCheckBox = true;
      String state = matcher.group(1);
      if (!state.equalsIgnoreCase("x")) {
        return false;
      }
    }
    return foundCheckBox;
  }
}
