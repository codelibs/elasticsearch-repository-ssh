package org.codelibs.elasticsearch.repository.ssh.utils;

public class SshConfig {
  private String host;
  private int port;
  private String username;
  private String password;
  private String knownHosts;
  private boolean ignoreHostKeyChecking;
  private String privateKey;
  private String passphrase;
  private String location;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getKnownHosts() {
    return knownHosts;
  }

  public void setKnownHosts(String knownHosts) {
    this.knownHosts = knownHosts;
  }

  public boolean isIgnoreHostKeyChecking() {
    return ignoreHostKeyChecking;
  }

  public void setIgnoreHostKeyChecking(boolean ignoreHostKeyChecking) {
    this.ignoreHostKeyChecking = ignoreHostKeyChecking;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }
}
