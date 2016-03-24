/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codelibs.elasticsearch.repository.ssh.blobstore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import com.jcraft.jsch.*;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.codelibs.elasticsearch.repository.ssh.utils.SshConfig;
import org.codelibs.elasticsearch.repository.ssh.utils.SshPool;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.repositories.RepositorySettings;
import org.elasticsearch.threadpool.ThreadPool;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * JSchClient manages SSH connections on JSch.
 *
 * @author shinsuke
 */
public class JSchClient {

    private SshConfig config;

    private SshPool sshPool;

    public JSchClient(final Settings componentSettings,
        final RepositorySettings repositorySettings,
        final ThreadPool threadPool) throws JSchException {

        SshConfig config = new SshConfig();
        config.setHost(repositorySettings.settings().get("host", componentSettings.get("host")));
        config.setPort(
            repositorySettings.settings().getAsInt("port", componentSettings.getAsInt("port", 22)));
        config.setUsername(
            repositorySettings.settings().get("username", componentSettings.get("username")));
        config.setPassword(
            repositorySettings.settings().get("password", componentSettings.get("password")));
        config.setLocation(
            repositorySettings.settings().get("location", componentSettings.get("location", "~/")));
        config.setPrivateKey(
            repositorySettings.settings().get("private_key", componentSettings.get("private_key")));
        config.setPassphrase(
            repositorySettings.settings().get("passphrase", componentSettings.get("passphrase")));
        config.setKnownHosts(
            repositorySettings.settings().get("known_hosts", componentSettings.get("known_hosts")));
        config.setIgnoreHostKeyChecking(repositorySettings.settings()
            .getAsBoolean("ignore_host_key",
                componentSettings.getAsBoolean("ignore_host_key", false)));

        if (config.getPassword() == null && config.getPrivateKey() == null) {
            throw new JSchException(
                "A password and private key for SSH are empty.");
        }

        this.config = config;

        GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
        poolConfig.setMaxTotalPerKey(5);
        poolConfig.setMinEvictableIdleTimeMillis(repositorySettings.settings()
            .getAsLong("session_expire", componentSettings.getAsLong("session_expire", 60000L)));

        TimeValue cleanInterval = repositorySettings.settings().getAsTime("clean_interval",
            componentSettings.getAsTime("clean_interval", TimeValue.timeValueMinutes(1)));
        poolConfig.setJmxEnabled(false);
        poolConfig.setTimeBetweenEvictionRunsMillis(cleanInterval.getMillis());
        this.sshPool = new SshPool(config, poolConfig);

    }

    public String getInfoString() {
        return config.getUsername() + "@" + config.getHost() + ":" + config.getLocation();
    }

    private ChannelExec openExecChannel(Session session) throws JSchException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        return channel;
    }

    private ChannelSftp openSftpChannel(Session session) throws JSchException {
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        return channel;
    }

    public void closeChannel(ChannelSftp channel) {
        if (channel != null) {
            channel.disconnect();
        }
    }

    public void closeChannel(ChannelExec channel) {
        if (channel != null) {
            channel.disconnect();
        }
    }

    public void mkdirs(final BlobPath blobPath) throws SftpException, JSchException {
        final String[] paths = blobPath.toArray();
        if (paths.length == 0) {
            return;
        }

        Session session = sshPool.getSession();
        ChannelSftp channel = openSftpChannel(session);

        final StringBuilder buf = new StringBuilder();
        buf.append(config.getLocation());
        try {
            for (final String p : paths) {
                buf.append('/').append(p);
                final String path = buf.toString();
                int retry = 5;
                while (retry > 0) {
                    try {
                        mkdirIfNotExists(channel, path);
                        retry = 0;
                    } catch (final SftpException e) {
                        try {
                            Thread.sleep(1000L);
                        } catch (final InterruptedException e1) {
                            // ignore
                        }
                        if (retry == 0) {
                            throw e;
                        }
                        retry--;
                    }
                }
            }
        } finally {
            closeChannel(channel);
            sshPool.returnSession(session);
        }
    }

    private void mkdirIfNotExists(ChannelSftp channel, final String path) throws SftpException {
        try {
            channel.ls(path);
        } catch (SftpException e) {
            channel.mkdir(path);
        }
    }

    public void rmdir(final BlobPath blobPath) throws JSchException {
        //TODO: any better solution?
        Session session = sshPool.getSession();
        ChannelExec channel = null;
        try {
            channel = openExecChannel(session);
            channel.setCommand(
                "/bin/rm -rf " + config.getLocation() + "/" + blobPath.buildAsString("/"));
            channel.connect();
            //channel.rmdir(location + "/" + blobPath.buildAsString("/"));
        } finally {
            closeChannel(channel);
            sshPool.returnSession(session);
        }
    }

    public InputStream get(final BlobPath blobPath) throws SftpException, JSchException {
        final Session session = sshPool.getSession();
        final ChannelSftp channel = openSftpChannel(session);
        final InputStream is = channel.get(config.getLocation() + "/"
            + blobPath.buildAsString("/"));
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return is.read();
            }

            @Override
            public int read(byte b[], int off, int len) throws IOException {
                return is.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                is.close();
                closeChannel(channel);
                sshPool.returnSession(session);
            }

        };
    }

    public OutputStream put(final BlobPath blobPath) throws SftpException, JSchException {
        final Session session = sshPool.getSession();
        final ChannelSftp channel = openSftpChannel(session);
        final OutputStream os = channel.put(config.getLocation() + "/"
            + blobPath.buildAsString("/"));
        return new OutputStream() {

            @Override
            public void write(final int b) throws IOException {
                os.write(b);
            }

            @Override
            public void write(byte b[], int off, int len) throws IOException {
                os.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                os.close();
                closeChannel(channel);
                sshPool.returnSession(session);
            }
        };
    }

    public Vector<LsEntry> ls(final BlobPath blobPath) throws SftpException, JSchException {
        Session session = sshPool.getSession();
        ChannelSftp channel = openSftpChannel(session);
        try {
            @SuppressWarnings("unchecked")
            final Vector<LsEntry> entities =
                channel.ls(config.getLocation() + "/" + blobPath.buildAsString("/"));
            return entities;
        } finally {
            closeChannel(channel);
            sshPool.returnSession(session);
        }
    }

    public void rm(final BlobPath blobPath) throws SftpException, JSchException {
        Session session = sshPool.getSession();
        ChannelSftp channel = openSftpChannel(session);
        try {
            channel.rm(config.getLocation() + "/" + blobPath.buildAsString("/"));
        } finally {
            closeChannel(channel);
            sshPool.returnSession(session);
        }
    }

    public void move(final String sourceBlobName, final String targetBlobName)
        throws SftpException, JSchException {
        Session session = sshPool.getSession();
        ChannelSftp channel = openSftpChannel(session);
        try {
            channel.rename(config.getLocation() + "/" + sourceBlobName,
                config.getLocation() + "/" + targetBlobName);
        } finally {
            closeChannel(channel);
            sshPool.returnSession(session);
        }
    }

    public void close() {
        sshPool.close();
    }

}
