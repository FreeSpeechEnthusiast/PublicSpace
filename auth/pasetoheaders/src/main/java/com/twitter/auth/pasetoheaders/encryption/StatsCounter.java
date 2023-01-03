package com.twitter.auth.pasetoheaders.encryption;

public interface StatsCounter {
  /**
   * Increments metric in specific counter by specific value
   *
   * @param delta
   */
  void incr(Long delta);
}
