package com.oracle.tool;

/**
 * Interface for progress reporting.
 */
public interface ProgressReporter {
  void setTotal(int total);

  void update(int delta);

  void finish();
}
