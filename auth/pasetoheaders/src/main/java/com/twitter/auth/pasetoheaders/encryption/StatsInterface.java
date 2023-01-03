package com.twitter.auth.pasetoheaders.encryption;

import java.util.Map;
import java.util.Optional;

public interface StatsInterface {
  /**
   * Provides access to the counter with specific scope, name and metadata
   *
   * @param scope
   * @param name
   * @param metadata
   * @return
   */
  StatsCounter counter(String scope, String name, Optional<Map<String, String>> metadata);
}
