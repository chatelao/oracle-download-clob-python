package com.oracle.tool;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Progress reporter that prints a progress bar to the console.
 * Thread-safe for concurrent updates.
 */
public class ConsoleProgressReporter implements ProgressReporter {
  private final PrintWriter out;
  private final AtomicInteger current = new AtomicInteger(0);
  private volatile int total = 0;
  private volatile boolean finished = false;
  private static final int BAR_WIDTH = 50;

  public ConsoleProgressReporter() {
    this.out = new PrintWriter(System.err, true);
  }

  @Override
  public synchronized void setTotal(int total) {
    this.total = total;
    this.current.set(0);
    this.finished = false;
    render();
  }

  @Override
  public void update(int value) {
    int newVal = this.current.addAndGet(value);
    synchronized (this) {
      render();
    }
    if (newVal >= this.total && this.total > 0) {
      finish();
    }
  }

  @Override
  public synchronized void finish() {
    if (!finished) {
      if (total > 0) {
        out.println();
      }
      finished = true;
    }
  }

  private void render() {
    if (total <= 0) {
      return;
    }
    int currentVal = current.get();
    double percent = (double) currentVal / total;
    int progress = (int) (percent * BAR_WIDTH);

    StringBuilder sb = new StringBuilder("\rDownloading [");
    for (int i = 0; i < BAR_WIDTH; i++) {
      if (i < progress) {
        sb.append("=");
      } else if (i == progress) {
        sb.append(">");
      } else {
        sb.append(" ");
      }
    }
    sb.append(String.format("] %d/%d (%.0f%%)", currentVal, total, percent * 100));
    out.print(sb.toString());
    out.flush();
  }
}
