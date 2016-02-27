package org.codelibs.elasticsearch.repository.ssh;

import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.snapshots.blobstore.BlobStoreIndexShardRepository;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.repositories.RepositoriesModule;

public class RepositorySshPlugin extends Plugin {

    @Override
    public String name() {
        return "repository-ssh";
    }

    @Override
    public String description() {
        return "This plugin provides SSH repository for Snapshot/Restore.";
    }

    @SuppressWarnings("unchecked")
    public void onModule(RepositoriesModule repositoriesModule) {
        Loggers.getLogger(RepositorySshPlugin.class).info("trying to register repository-ssh...");
        repositoriesModule.registerRepository(SshRepository.TYPE, SshRepository.class,
            BlobStoreIndexShardRepository.class);
    }

}
