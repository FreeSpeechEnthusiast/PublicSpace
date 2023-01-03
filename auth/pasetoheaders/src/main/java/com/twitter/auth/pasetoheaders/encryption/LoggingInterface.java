package com.twitter.auth.pasetoheaders.encryption;

import java.util.Map;
import java.util.Optional;

public interface LoggingInterface {
  /**
   * Logs debug level message with specific scope and metadata
   *
   * @param scope
   * @param message
   * @param metadata
   */
  void debug(String scope, String message, Optional<Map<String, String>> metadata);
  /**
   * Logs info level message with specific scope and metadata
   *
   * @param scope
   * @param message
   * @param metadata
   */
  void info(String scope, String message, Optional<Map<String, String>> metadata);
  /**
   * Logs warn level message with specific scope and metadata
   *
   * @param scope
   * @param message
   * @param metadata
   */
  void warn(String scope, String message, Optional<Map<String, String>> metadata);
  /**
   * Logs error level message with specific scope and metadata
   *
   * @param scope
   * @param message
   * @param metadata
   */
  void error(String scope, String message, Optional<Map<String, String>> metadata);
  /**
   * Logs trace level message with specific scope and metadata
   *
   * @param scope
   * @param message
   * @param metadata
   */
  void trace(String scope, String message, Optional<Map<String, String>> metadata);
}
