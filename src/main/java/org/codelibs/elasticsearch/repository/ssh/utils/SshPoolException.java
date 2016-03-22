package org.codelibs.elasticsearch.repository.ssh.utils;

public class SshPoolException extends RuntimeException {
  public SshPoolException(String message) {
    super(message);
  }

  public SshPoolException(Throwable e) {
    super(e);
  }

  public SshPoolException(String message, Throwable cause) {
    super(message, cause);
  }
}
