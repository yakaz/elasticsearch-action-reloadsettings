Reload Settings Action Plugin
=============================

The Reload Settings action plugin provides with a new REST end-point that reads configuration files and applies corresponding dynamic settings, and reports the other changes that could not be applied.

Installation
------------

Simply run at the root of your ElasticSearch installation:

	bin/plugin -install com.yakaz.elasticsearch.plugins/elasticsearch-action-reloadsettings/1.0.0

This will download the plugin from the Central Maven Repository.

In order to declare this plugin as a dependency, add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>com.yakaz.elasticsearch.plugins</groupId>
    <artifactId>elasticsearch-action-reloadsettings</artifactId>
    <version>1.0.0</version>
</dependency>
```

Version matrix:

	┌────────┬──────────────────────┐
	│ Plugin │     ElasticSearch    │
	├────────┼──────────────────────┤
	│ master │ 0.90.0.RC1 ─► master │
	├────────┼──────────────────────┤
	│ 1.0.0  │ 0.90.0.RC1 ─► master │
	└────────┴──────────────────────┘

Description
-----------

When you change `config/elasticsearch.yml`, some of the settings can be applied right away using the [Cluster Update Settings API][updateAPI], while the others will require a restart.

This plugins helps to apply the settings that can be applied without restart, and can lists the settings that will require a proper restart.

See also
--------

http://www.elasticsearch.org/guide/reference/api/admin-cluster-update-settings.html

[updateAPI]: http://www.elasticsearch.org/guide/reference/api/admin-cluster-update-settings.html
