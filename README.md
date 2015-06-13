Elasticsearch Repository SSH
=======================

## Overview

Repository SSH Plugin provides SSH repository for Elasticsearch's Snapshot/Restore feature.

## Version

| Version   | Elasticsearch |
|:---------:|:-------------:|
| master    | 1.6.X         |
| 1.6.0     | 1.6.1         |
| 1.4.0     | 1.4.1         |

### Issues/Questions

Please file an [issue](https://github.com/codelibs/elasticsearch-repository-ssh/issues "issue").
(Japanese forum is [here](https://github.com/codelibs/codelibs-ja-forum "here").)

## Installation

### Install Repository SSH Plugin

    $ $ES_HOME/bin/plugin --install org.codelibs/elasticsearch-repository-ssh/1.6.0

## References

### Register Repository

This plugin provides "ssh" type for a snapshot repository.
You can register ssh-based repository as below:

    curl -s -XPUT localhost/_snapshot/my_backup?pretty -d '{
        "type": "ssh",
        "settings": {
            "location": "/mnt/snapshot",
            "host": "123.123.123.123",
            "port": 22,
            "username": "snapshot_user",
            "private_key": "/home/snapshot_user/.ssh/id_rsa",
            "known_hosts": "/home/snapshot_user/.ssh/known_hosts",
            "compress": true
        }
    }'

where

| Name     | Type | Description |
|:---------|:-:|:-------------|
| location | string | a snapshot directory on SSH server |
| host | string | Host name for SSH server |
| port | int | Port number for SSH server |
| username | string | User name |
| private_key | string | Private key for "username" |
| known\_hosts | string | known\_hosts file |
| password | string | Password for "username" if not using private\_key |
| ignore\_host\_key | boolean | true if ignoring known\_host file |

### Create/Restore Snapshot

The usage is the same as Elasticsearch's one.
See [snapshot and restore](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/modules-snapshots.html "snapshot and restore").
