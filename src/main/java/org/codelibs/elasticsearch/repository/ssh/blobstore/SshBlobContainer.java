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
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.elasticsearch.common.blobstore.BlobMetaData;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.blobstore.support.AbstractBlobContainer;
import org.elasticsearch.common.blobstore.support.PlainBlobMetaData;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.MapBuilder;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.elasticsearch.common.io.Streams;

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

        blobStore.getClient().mkdirs(blobPath);
    }

    @Override
    public Map<String, BlobMetaData> listBlobs() throws IOException {
        return listBlobsByPrefix(null);
    }

    @Override
    public Map<String, BlobMetaData> listBlobsByPrefix(
        String blobNamePrefix) throws IOException {
        try {
            final Vector<LsEntry> entries = blobStore.getClient().ls(path());
            if (entries.isEmpty()) {
                return new HashMap<>();
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
        } catch (Exception e) {
            throw new IOException("Failed to load files in " + path().buildAsString("/"), e);
        }
    }

    @Override
    public void move(String sourceBlobName, String targetBlobName) throws IOException {
        final BlobPath sourcePath = path().add(sourceBlobName);
        final BlobPath targetPath = path().add(targetBlobName);
        try {
            JSchClient client = blobStore.getClient();
            client.move(sourcePath.buildAsString("/"), targetPath.buildAsString("/"));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void deleteBlob(final String blobName) throws IOException {
        final BlobPath path = path().add(blobName);
        try {
            JSchClient client = blobStore.getClient();
            client.rm(path);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean blobExists(final String blobName) {
        final BlobPath path = path().add(blobName);
        try {
            JSchClient client = blobStore.getClient();
            final Vector<LsEntry> entries = client.ls(path);
            return !entries.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public InputStream readBlob(String blobName) throws IOException {
        final BlobPath path = path().add(blobName);
        try {
            JSchClient client = blobStore.getClient();
            return client.get(path);
        } catch (Exception e) {
            throw new IOException("Failed to load " + path.buildAsString("/"), e);
        }
    }

    @Override
    public void writeBlob(String blobName, InputStream inputStream, long blobSize)
        throws IOException {
        OutputStream stream = createOutput(blobName);
        Streams.copy(inputStream, stream);
    }

    @Override
    public void writeBlob(String blobName, BytesReference bytes) throws IOException {
        try (OutputStream stream = createOutput(blobName)) {
            bytes.writeTo(stream);
        }

    }


    private OutputStream createOutput(final String blobName) throws IOException {
        final BlobPath path = path().add(blobName);
        try {
            JSchClient client = blobStore.getClient();
            return client.put(path);
        } catch (Exception e) {
            throw new IOException("Failed to open " + path.buildAsString("/"), e);
        }
    }

}
