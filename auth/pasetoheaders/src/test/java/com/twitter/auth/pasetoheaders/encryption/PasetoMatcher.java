package com.twitter.auth.pasetoheaders.encryption;

import dev.paseto.jpaseto.Paseto;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

class PasetoMatcher extends TypeSafeMatcher<Paseto> {

  private final Paseto expected;

  PasetoMatcher(Paseto expected) {
    this.expected = expected;
  }

  @Override
  protected boolean matchesSafely(Paseto actual) {
    return expected.equals(actual);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("token with:")
        .appendText("\n\t\tversion: ").appendValue(expected.getVersion())
        .appendText("\n\t\tpurpose: ").appendValue(expected.getPurpose())
        .appendText("\n\t\tpayload: ").appendValue(expected.getClaims())
                .appendText("\n\t\tfooter:  ")
        .appendText("\n\t\t\tvalue:  ").appendValue(expected.getFooter().value())
                .appendText("\n\t\t\tclaims: ").appendValue(expected.getFooter());
  }

  @Override
  protected void describeMismatchSafely(Paseto item, Description mismatchDescription) {
    mismatchDescription.appendText("was token with:");
    mismatchDescription.appendText("\n\t\tversion: ").appendValue(item.getVersion());
    if (expected.getVersion() != item.getVersion()) {
      mismatchDescription.appendText(" << does not match");
    }
    mismatchDescription.appendText("\n\t\tpurpose: ").appendValue(item.getPurpose());
    if (expected.getPurpose() != item.getPurpose()) {
      mismatchDescription.appendText(" << does not match");
    }
    mismatchDescription.appendText("\n\t\tpayload: ").appendValue(item.getClaims());
    if (expected.getClaims() != item.getClaims()) {
      mismatchDescription.appendText(" << does not match");
    }
    mismatchDescription.appendText("\n\t\tfooter:  ");
    if (expected.getFooter() != item.getFooter()) {
      mismatchDescription.appendText(" << does not match");
    }

    if (item.getFooter() != null) {
      mismatchDescription.appendText("\n\t\t\tvalue:  ")
          .appendValue(item.getFooter().value());
      if (expected.getFooter() == null
          || item.getFooter().value() != expected.getFooter().value()) {
        mismatchDescription.appendText(" << does not match");
      }
      mismatchDescription.appendText("\n\t\t\tclaims: ")
          .appendValue(item.getFooter().entrySet());
      if (expected.getFooter() == null
          || item.getFooter() != expected.getFooter()) {
        mismatchDescription.appendText(" << does not match");
      }
    }
  }

  static PasetoMatcher paseto(Paseto paseto) {
    return new PasetoMatcher(paseto);
  }
}

