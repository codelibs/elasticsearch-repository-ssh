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

import org.codelibs.elasticsearch.repository.ssh.blobstore.JSchClient.JschChannel;
import org.elasticsearch.common.blobstore.BlobMetaData;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.blobstore.support.AbstractBlobContainer;
import org.elasticsearch.common.blobstore.support.PlainBlobMetaData;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.MapBuilder;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

/**
 *
 */
public class SshBlobContainer extends AbstractBlobContainer {

    protected final SshBlobStore blobStore;

    public SshBlobContainer(final SshBlobStore blobStore,
            final BlobPath blobPath) throws IOException, JSchException,
            SftpException {
        super(blobPath);
        this.blobStore = blobStore;

        try (JschChannel channel = blobStore.getClient().getChannel()) {
            channel.mkdirs(blobPath);
        }
    }

    @Override
    public ImmutableMap<String, BlobMetaData> listBlobs() throws IOException {
        return listBlobsByPrefix(null);
    }

    @Override
    public ImmutableMap<String, BlobMetaData> listBlobsByPrefix(
            String blobNamePrefix) throws IOException {
        try (JschChannel channel = blobStore.getClient().getChannel()) {
            final Vector<LsEntry> entries = channel.ls(path());
            if (entries.isEmpty()) {
                return ImmutableMap.of();
            }

            final String namePrefix = blobNamePrefix == null ? ""
                    : blobNamePrefix;
            final MapBuilder<String, BlobMetaData> builder = MapBuilder
                    .newMapBuilder();
            for (final LsEntry entry : entries) {
                if (entry.getAttrs().isReg()
                        && entry.getFilename().startsWith(namePrefix)) {
                    builder.put(entry.getFilename(), new PlainBlobMetaData(
                            entry.getFilename(), entry.getAttrs().getSize()));
                }
            }
            return builder.immutableMap();
        } catch (SftpException | JSchException e) {
            throw new IOException("Failed to load files in "
                    + path().buildAsString("/"), e);
        }
    }

    @Override
    public boolean deleteBlob(final String blobName) throws IOException {
        final BlobPath path = path().add(blobName);
        try (JschChannel channel = blobStore.getClient().getChannel()) {
            channel.rm(path);
            return true;
        } catch (SftpException | JSchException e) {
            return false;
        }
    }

    @Override
    public boolean blobExists(final String blobName) {
        final BlobPath path = path().add(blobName);
        try (JschChannel channel = blobStore.getClient().getChannel()) {
            final Vector<LsEntry> entries = channel.ls(path);
            return !entries.isEmpty();
        } catch (SftpException | JSchException | IOException e) {
            return false;
        }
    }

    @Override
    public InputStream openInput(final String blobName) throws IOException {
        final BlobPath path = path().add(blobName);
        try {
            final JschChannel channel = blobStore.getClient().getChannel();
            return channel.get(path);
        } catch (SftpException | JSchException e) {
            throw new IOException("Failed to load " + path.buildAsString("/"),
                    e);
        }
    }

    @Override
    public OutputStream createOutput(final String blobName) throws IOException {
        final BlobPath path = path().add(blobName);
        try {
            final JschChannel channel = blobStore.getClient().getChannel();
            return channel.put(path);
        } catch (SftpException | JSchException e) {
            throw new IOException("Failed to open " + path.buildAsString("/"),
                    e);
        }
    }

}
