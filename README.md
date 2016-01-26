Reload Settings Action Plugin
=============================

The Reload Settings action plugin provides with a new REST end-point that reads configuration files and applies corresponding dynamic settings, and reports the other changes that could not be applied.

Installation
------------

Simply run at the root of your ElasticSearch installation:

	bin/plugin -install com.yakaz.elasticsearch.plugins/elasticsearch-action-reloadsettings/1.6.0

This will download the plugin from the Central Maven Repository.

In order to declare this plugin as a dependency, add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>com.yakaz.elasticsearch.plugins</groupId>
    <artifactId>elasticsearch-action-reloadsettings</artifactId>
    <version>1.6.0</version>
</dependency>
```

Version matrix:

	┌────────┬────────────────────────┐
	│ Plugin │     ElasticSearch      │
	├────────┼────────────────────────┤
	│        │ 2.0.0-beta1            │
	├────────┼────────────────────────┤
	│ 1.6.x  │ 1.4.0.Beta1 ─► (1.7.4) │
	├────────┼────────────────────────┤
	│ 1.5.x  │ 1.3.0 ─► (1.3.7)       │
	├────────┼────────────────────────┤
	│ 1.4.x  │ 1.2.0 ─► (1.2.4)       │
	├────────┼────────────────────────┤
	│ 1.3.x  │ 1.0.0.RC1 ─► (1.1.1)   │
	├────────┼────────────────────────┤
	│ 1.2.x  │ 0.90.4 ─► (0.90.11)    │
	├────────┼────────────────────────┤
	│ 1.1.x  │ 0.90.3                 │
	├────────┼────────────────────────┤
	│ 1.0.x  │ 0.90.0.RC1 ─► 0.90.2   │
	└────────┴────────────────────────┘

Description
-----------

When you change `config/elasticsearch.yml`, some of the settings can be applied right away using the [Cluster Update Settings API][updateAPI], while the others will require a restart.
This plugin can load the settings from the file, and help you see what changed.
It comes with a python script that can automatically analyse what differences can and should be applied, based on the time the configuration file changed.

Note: Some plugins can inject settings, this plugin cannot honor this situation well.
Supporting this flawlessly would imply reinstanciating all the plugins using the new configuration read from file.
Asking the already instanciated plugins for their settings may not be accurate as their behavior may depend on settings from the file.
Hence you may notice a difference between the startup settings and the file settings even if the file did not change.

Settings in ElasticSearch
-------------------------

ElasticSearch has multiple sources of settings:
* The startup settings, read from `config/elasticsearch.yml`
* The cluster settings, that can be changed on the fly
  It comes with two flavors:
  * transient settings: valid as long as the cluster lives
  * persistent settings: kept across full cluster restarts

(It also has per index settings, which this plugins ignores for now.)

When a settings is used, the value given on startup is used, except if the code explicitly handles dynamic cluster settings, in which case the value is updated whenever a new value is provided using the dynamic cluster settings.
In the dynamic cluster settings, the transient settings take precedence over the persistent ones.

Inside a cluster you may very well start nodes using different values in the configuration file.
In such a case, in the absence of a cluster setting, each node ends up using its startup value from the file.
Unvoluntary inconsistencies may arise in such a case.

This plugin helps you tracking the values provided from the different sources.
It also keeps a timestamp associated with the last cluster settings version, and reads the configuration file modification date on each node.
Along with this information, each setting is annotated with a hint whether it can be overridden using a dynamic cluster setting or not.

Typical example: splitbrain in case of netsplit
-----------------------------------------------

To prevent a cluster from being split brain in the event of a netsplit, you are encouraged to set `discovery.zen.minimum_master_nodes` to a quorum.
The quorum is `floor(number of master nodes in your cluster / 2) + 1`.
It ensures that each node functions if and only if it sees the majority of the master nodes, hence being on the largest part of a split network, along with the majority of the nodes.

When your cluster grows, you should update this setting to its new value.
When your cluster shrinks, you should update this setting to its new value too.
(Pay attention if you go from 2 to 1 node, or if you remove more than 1 node at a time.)

If you manage your cluster automatically, the configuration file of the new node will contain the new value for `discovery.zen.minimum_master_nodes`, the value that should be applied cluster-wide right after the node joined the cluster.
Using the script provided with this plugin, you can do so automatically, using a transient cluster dynamic setting.

The update script
-----------------

`elasticsearch-reload.py` will analyze the configuration of each node of the cluster, resolve possible simple conflicts, output a summary and apply the new settings.
It can run in dry-run, it can be used as a consistency check, and it can apply new settings using transient cluster dynamic settings.
It can be used as a standalone script or as a utility module.

If a setting is updated to different values on different nodes configuration file, compared to the last cluster setting timestamp, there is a conflict.
The script can resolve the case when the oldest files are consistent with the effective setting value and the newest files all specify the same desired value.
Note that a mere `touch` of the configuration file is sufficient to make the configuration file more up-to-date than the cluster settings.

Usage
-----

If you wish to investigate the settings of your cluster yourself, use the following REST call.
Otherwise, chances are that you are more interested by the python script, even for mere dry-run.
The python script has an integrated usage explanation, just run: `src/main/python/elasticsearch-reload.py --help`.

End-points: `/_nodes/settings/reload` or `/_nodes/nodeId1,nodeId2/settings/reload` if you want to restrict the nodes to run on.
The only parameter is a path parameter, it lets you select the nodes to query. See the [the nodes selective options][nodeSelection].
Output:
```json
{
    "settings": {
        "cluster": null / {
            "effective": {
                "SETTING NAME": "SETTING VALUE",
                ...
            },
            "transient": { ... },
            "persistent": { ... },
            "timestamp": null / "yyyy-MM-dd'T'HH:mm:ss.SSSZZ",
            "timestamp_in_millis": null / 0
        },
        "nodes": {
            "nodeId1": {
                "effective": missing / {
                    "SETTING NAME": "SETTING VALUE",
                    ...
                },
                "initial": { ... },
                "desired": missing / { ... },
                "file": { ... },
                "environment": { ... },
                "file_timestamp": null / "yyyy-MM-dd'T'HH:mm:ss.SSSZZ",
                "file_timestamp_in_millis": null / 0,
                "inconsistencies": missing / {
                    "SETTING NAME": {
                        "effective": "VALUE",
                        "desired": "VALUE",
                        "_updatable": true / false
                    },
                    ...
                }
            },
            ...
        },
        "consistencies": {
            "effective": {
                "SETTING NAME": "SETTING VALUE",
                ...
            },
            "initial": { ... },
            "desired": { ... },
        },
        "inconsistencies": {
            "effective": {
                "SETTING NAME": {
                    "SOURCE NODE ID": "VALUE",
                    ...,
                    "_updatable": true / false
                },
                ...
            },
            "initial": { ... },
            "desired": { ... },
        }
    }
}
```

See also
--------

http://www.elasticsearch.org/guide/reference/api/admin-cluster-update-settings.html

[updateAPI]: http://www.elasticsearch.org/guide/reference/api/admin-cluster-update-settings.html

[nodeSelection]: http://www.elasticsearch.org/guide/reference/api/#Nodes
