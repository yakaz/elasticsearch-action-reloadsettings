Reload Action Plugin
====================

The Reload action plugin provides with a new REST end-point that reads configuration files and applies corresponding dynamic settings, and reports the other changes that could not be applied.

Installation
------------

Simply run at the root of your ElasticSearch v0.20.2+ installation:

	bin/plugin -install com.yakaz.elasticsearch.plugins/elasticsearch-action-reload/1.0.0

This will download the plugin from the Central Maven Repository.

For older versions of ElasticSearch, you can still use the longer:

	bin/plugin -url http://oss.sonatype.org/content/repositories/releases/com/yakaz/elasticsearch/plugins/elasticsearch-action-reload/1.0.0/elasticsearch-action-reload-1.0.0.zip install elasticsearch-action-reload

In order to declare this plugin as a dependency, add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>com.yakaz.elasticsearch.plugins</groupId>
    <artifactId>elasticsearch-action-reload</artifactId>
    <version>1.0.0</version>
</dependency>
```

Version matrix:

	┌──────────────────────┬────────────────┐
	│ Reload Action Plugin │ ElasticSearch  │
	├──────────────────────┼────────────────┤
	│ master               │ 0.90 ─► master │
	├──────────────────────┼────────────────┤
	│ 1.0.0                │ 0.19 ─► master │
	└──────────────────────┴────────────────┘

Description
-----------

When you change `config/elasticsearch.yml`, some of the settings can be applied right away using the [Cluster Update Settings API][updateAPI], while the others will require a restart.

This plugins helps to apply the settings that can be applied without restart, and can lists the settings that will require a proper restart.

See also
--------

http://www.elasticsearch.org/guide/reference/api/admin-cluster-update-settings.html

[updateAPI]: http://www.elasticsearch.org/guide/reference/api/admin-cluster-update-settings.html
