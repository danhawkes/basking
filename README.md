# Basking

Sync a Grooveshark library to disk.

[![Build Status](http://arcs.co/jenkins/buildStatus/icon?job=basking)](http://arcs.co/jenkins/job/basking/)

## Features

* Command line API for use as a standalone service
  * `$ basking ~/music 'username' 'pass'`
* Playlist generation
  * `GS Favorites` and `GS Collection.m3u`  
* Robust system for keeping everything in sync with the API
  * Grooveshark identifier is embedded in IDv3 tag, so files can be renamed/retagged without getting lost.
  * Deletions/removed favorites are propagated to the local library

## Installation

### Java

Add the following maven repository and dependency to your POM:

```xml
<repositories>
	<repository>
		<id>arcs.co</id>
		<url>http://arcs.co/archiva/repository/internal</url>
	</repository>
</repositories>

<dependencies>
	<dependency>
		<groupId>co.arcs.groove</groupId>
		<artifactId>basking</artifactId>
		<version>1.2.0</version>
	</dependency>
</dependencies>
```

Alternatively, download the latest standalone Jar [here](http://arcs.co/archiva/browse/co.arcs.groove/basking/).

### Command line

Get the distribution package [here](http://arcs.co/archiva/browse/co.arcs.groove/basking/). 

The jar-with-dependencies and wrapper script need adding to the path. On linux that would look something like this:

```bash
unzip basking-1.2.0-dist.zip
sudo cp -r basking-1.2.0 /usr/local/bin
sudo ln -s -f /usr/local/bin/basking-1.2.0/basking /usr/local/bin/basking
```

## Usage

### Java

```java
Config config = new Config();
config.syncPath = new File("./music");
config.username = "user@email.com";
config.password = "password"

SyncService service = new SyncService(config);
ListenableFuture<SyncOutcome> outcome = service.start();
```

### Command line

```bash
Usage: basking [options]
  Options:
    -cfg, --config
       JSON configuration file to load.
    -dry, --dry-run
       Do not modify the disk.
       Default: false
    -h, --help
       Show this help.
       Default: false
    -num, --num-concurrent
       Number of concurrent downloads.
       Default: 1
  * -pass, --password
       Grooveshark user password.
  * -dir, --sync-dir
       Directory to sync. Will be created if it does not already exist.
  * -user, --username
       Grooveshark user name.
```

## Licence

Apache 2.0. See `LICENCE.txt` for details.
