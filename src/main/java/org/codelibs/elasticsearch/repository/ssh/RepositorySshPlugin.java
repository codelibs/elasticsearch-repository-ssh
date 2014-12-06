package org.codelibs.elasticsearch.repository.ssh;

import org.codelibs.elasticsearch.repository.ssh.module.SshRepositoryModule;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.repositories.RepositoriesModule;

public class RepositorySshPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "RepositorySshPlugin";
    }

    @Override
    public String description() {
        return "This plugin provides SSH repository for Snapshot/Restore.";
    }

    public void onModule(final RepositoriesModule module) {
        module.registerRepository(SshRepository.TYPE, SshRepositoryModule.class);
    }

}
