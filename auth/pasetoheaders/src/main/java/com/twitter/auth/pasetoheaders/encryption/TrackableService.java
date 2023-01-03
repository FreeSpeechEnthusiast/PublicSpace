package com.twitter.auth.pasetoheaders.encryption;

import java.util.Map;
import java.util.Optional;

public class TrackableService {
  protected Optional<LoggingInterface> logger;
  protected Optional<StatsInterface> stats;
  protected String serviceName;

  public void incrMetric(String metricName, Long delta, Optional<Map<String, String>> metadata) {
    stats.ifPresent(statsInterface ->
        statsInterface.counter(serviceName, metricName,
            metadata).incr(delta));
  }

  public void trace(String message, Optional<Map<String, String>> metadata) {
    logger.ifPresent(loggingInterface ->
        loggingInterface.trace(serviceName, message,
            metadata));
  }

  public void debug(String message, Optional<Map<String, String>> metadata) {
    logger.ifPresent(loggingInterface ->
        loggingInterface.debug(serviceName, message,
            metadata));
  }

  public void info(String message, Optional<Map<String, String>> metadata) {
    logger.ifPresent(loggingInterface ->
        loggingInterface.info(serviceName, message,
            metadata));
  }

  public void warn(String message, Optional<Map<String, String>> metadata) {
    logger.ifPresent(loggingInterface ->
        loggingInterface.warn(serviceName, message,
            metadata));
  }

  public void error(String message, Optional<Map<String, String>> metadata) {
    logger.ifPresent(loggingInterface ->
        loggingInterface.error(serviceName, message,
            metadata));
  }
}
