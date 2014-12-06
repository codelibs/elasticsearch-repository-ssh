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

package org.codelibs.elasticsearch.repository.ssh;

import java.io.IOException;

import org.codelibs.elasticsearch.repository.ssh.blobstore.JSchClient;
import org.codelibs.elasticsearch.repository.ssh.blobstore.SshBlobStore;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.blobstore.BlobStore;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.index.snapshots.IndexShardRepository;
import org.elasticsearch.repositories.RepositoryException;
import org.elasticsearch.repositories.RepositoryName;
import org.elasticsearch.repositories.RepositorySettings;
import org.elasticsearch.repositories.blobstore.BlobStoreRepository;
import org.elasticsearch.threadpool.ThreadPool;

import com.jcraft.jsch.JSchException;

public class SshRepository extends BlobStoreRepository {

    public final static String TYPE = "ssh";

    private final SshBlobStore blobStore;

    private ByteSizeValue chunkSize;

    private final BlobPath basePath;

    private boolean compress;

    @Inject
    public SshRepository(final RepositoryName name,
            final RepositorySettings repositorySettings,
            final IndexShardRepository indexShardRepository,
            final ThreadPool threadPool) throws IOException {
        super(name.getName(), repositorySettings, indexShardRepository);

        try {
            blobStore = new SshBlobStore(componentSettings, new JSchClient(
                    componentSettings, repositorySettings, threadPool));
        } catch (final JSchException e) {
            throw new RepositoryException(name.name(),
                    "Failed to initialize SSH configuration.", e);
        }

        chunkSize = repositorySettings.settings().getAsBytesSize("chunk_size",
                componentSettings.getAsBytesSize("chunk_size", null));
        compress = repositorySettings.settings().getAsBoolean("compress",
                componentSettings.getAsBoolean("compress", false));
        basePath = BlobPath.cleanPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BlobStore blobStore() {
        return blobStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCompress() {
        return compress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ByteSizeValue chunkSize() {
        return chunkSize;
    }

    @Override
    protected BlobPath basePath() {
        return basePath;
    }
}
