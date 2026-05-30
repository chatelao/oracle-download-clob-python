package com.oracle.tool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ConsoleProgressReporterTest {

  @Test
  void testConcurrentUpdates() throws InterruptedException {
    ConsoleProgressReporter reporter = new ConsoleProgressReporter();
    int total = 1000;
    reporter.setTotal(total);

    ExecutorService executor = Executors.newFixedThreadPool(10);
    for (int i = 0; i < total; i++) {
      executor.submit(() -> reporter.update(1));
    }

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);

    assertDoesNotThrow(reporter::finish);
  }

  @Test
  void testNoTotal() {
    ConsoleProgressReporter reporter = new ConsoleProgressReporter();
    assertDoesNotThrow(() -> reporter.update(1));
    assertDoesNotThrow(reporter::finish);
  }
}
