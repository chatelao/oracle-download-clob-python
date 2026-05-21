package com.oracle.tool;

import java.io.PrintWriter;

/**
 * Progress reporter that prints a progress bar to the console.
 */
public class ConsoleProgressReporter implements ProgressReporter {
  private final PrintWriter out;
  private int total = 0;
  private int current = 0;
  private static final int BAR_WIDTH = 50;

  public ConsoleProgressReporter() {
    this.out = new PrintWriter(System.err, true);
  }

  @Override
  public void setTotal(int total) {
    this.total = total;
    this.current = 0;
    render();
  }

  @Override
  public void update(int n) {
    this.current += n;
    render();
    if (this.current >= this.total) {
      out.println();
    }
  }

  private void render() {
    if (total <= 0) {
      return;
    }
    double percent = (double) current / total;
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
    sb.append(String.format("] %d/%d (%.0f%%)", current, total, percent * 100));
    out.print(sb.toString());
    out.flush();
  }
}
