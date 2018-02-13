## AppDynamics Monitoring Extension for use with MongoDB Ops Manager

### Use Case

MongoDB Ops Manager is a service for managing, monitoring and backing up a MongoDB infrastructure. In addition, Ops Manager allows Administrators to maintain a server pool to facilitate the deployment of MongoDB.

Ops Manager provides the services described here.

#### Monitoring

Ops Manager Monitoring provides real-time reporting, visualization, and alerting on key database and hardware indicators.

How it Works: A lightweight Monitoring Agent runs within your infrastructure and collects statistics from the nodes in your MongoDB deployment. The agent transmits database statistics back to Ops Manager to provide real-time reporting. You can set alerts on indicators you choose.

#### Automation

Ops Manager Automation provides an interface for configuring MongoDB nodes and clusters and for upgrading your MongoDB deployment.

#### Backup

Ops Manager Backup provides scheduled snapshots and point-in-time recovery of your MongoDB replica sets and sharded clusters.


![alt text](https://docs.opsmanager.mongodb.com/current/_images/how-it-works-ops.png)


### Prerequisites

1. This extension requires an AppDynamics Java Machine Agent up and running. 
2. Ops Manager needs to be configured either locally or remotely and have at least one Mongo deployment (standalone or cluster)
3. The extension accesses MongoDB's public API. Please generate an API access key from your Ops Manager homepage -> Settings -> Public API access and save it, as this key will never be visible again. This key is used will be used as your password in the config.yml. 
4. On the same page, make sure to whitelist your domain for API access. 0.0.0.0/0 can be used to whitelist any accessing hosts. 

### Installing the Extension
1.  Unzip the contents of 'MongoDBOpsManagerMonitor'-<version>.zip file and copy the directory to `<your-machine-agent-dir>/monitors</your-machine-agent-dir>`.</version>
2.  Edit the config.yml file. An example config.yml file follows these installation instructions.
3.  Restart the Machine Agent.

**Sample config.yaml:** The following is a sample config.yaml file that uses a MongoDB Ops Manager server to monitor data. Once you have configured the history server across your various nodes, they can be added to the servers tab. The metrics shown in the file are customizable. You can choose to remove metrics or en entire section (jobs, stages etc) and they won't be reported. You can also add properties to individual metrics. The following properties can be added: 
1. alias: The actual name of the metric as you would see it in the metric browser
2. multiplier: Used to transform the metric value, particularly for cases where memory is reported in bytes. 1.0 by default. 
3. delta: Used to display a 'delta' or a difference between metrics that have an increasing value every minute. False by default.
4. cluster: The cluster-rollup qualifier specifies how the Controller aggregates metric values in a tier (a cluster of nodes). The value is an enumerated type. Valid values are **INDIVIDUAL** (default) or **COLLECTIVE**. 
5. aggregation: The aggregator qualifier specifies how the Machine Agent aggregates the values reported during a one-minute period. Valid values are **AVERAGE** (default) or **SUM** or **OBSERVATION**. 
6. time: The time-rollup qualifier specifies how the Controller rolls up the values when it converts from one-minute granularity tables to 10-minute granularity and 60-minute granularity tables over time. Valid values are **AVERAGE** (default) or **SUM** or **CURRENT**.
7. delta: Enable this if you wish to see the progression of a metric value over one minute intervals. 
<pre>
#This will publish metrics to one tier (highly recommended)
#Instructions on how to retrieve the Component ID can be found in the Metric Path section of https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
metricPrefix: "Server|Component:<COMPONENT_ID>|Custom Metrics|MongoDB Ops Manager Monitor|"

#Add your MongoDB OPS Manager server here.
servers:
   - uri: "http://54.218.226.84:8080/api/public/v1.0/"
     name: "Ops Manager 1"
     username: ""
     password: ""

encryptionKey:

#Generic metric prefix used to publish metrics to all tiers (NOT RECOMMENDED)
#metricPrefix: "Custom Metrics|MongoDB Ops Manager Monitor|"

numberOfThreads: 5

databases: [local, test]

## This section can be used to configure metrics published by the extension. You have the ability to add multipliers & modify the metric qualifiers for each metric.
## Valid 'cluster' rollup values: INDIVIDUAL, COLLECTIVE
## Valid 'aggregation' types: AVERAGE, SUM, OBSERVATION
## Valid 'time' rollup types: AVERAGE, SUM, CURRENT
## You can choose to not add any or all of these fields to any metric and the default values for each of the above will be used (INDIVIDUAL, AVERAGE & AVERAGE for cluster, aggregation & time respectively).
metrics:
     hosts:
        assert:
           - ASSERT_REGULAR:
               alias: "ASSERT_REGULAR"
               multiplier: "1.0"
               cluster: "INDIVIDUAL"
               aggregation: "AVERAGE"
               time: "SUM"
               delta: "false"
           - ASSERT_WARNING:
               alias: "ASSERT_WARNING"
           - ASSERT_MSG:
               alias: "ASSERT_MSG"
           - ASSERT_USER:
               alias: "ASSERT_USER"

        memory:
           - MEMORY_RESIDENT:
               alias: "MEMORY_RESIDENT" #Amount of RAM (MB) currently used by the database process
           - MEMORY_VIRTUAL:
               alias: "MEMORY_VIRTUAL" #MB currently used by the mongod process
           - MEMORY_MAPPED:
               alias: "MEMORY_MAPPED" #Amount of mapped memory (MB) used by the database

        network:
           - NETWORK_BYTES_IN:
               alias: "NETWORK_BYTES_IN" #bytes per second
           - NETWORK_BYTES_OUT:
               alias: "NETWORK_BYTES_OUT" #bytes per second
           - NETWORK_NUM_REQUESTS:
               alias: "NETWORK_NUM_REQUESTS" #scalar per second

        connections:
           - CONNECTIONS:
               alias: "CONNECTIONS" #Total number of connections to the DB server

        opcounter:
           - OPCOUNTER_CMD:
               alias: "OPCOUNTER_CMD" #Total number of commands issued to the DB
           - OPCOUNTER_INSERT:
               alias: "OPCOUNTER_INSERT" #Total number of insert operations
           - OPCOUNTER_QUERY:
               alias: "OPCOUNTER_QUERY" #Total number of query operations
           - OPCOUNTER_UPDATE:
               alias: "OPCOUNTER_UPDATE" #Total number of update operations
           - OPCOUNTER_DELETE:
               alias: "OPCOUNTER_DELETE" #Total number of delete operations
           - OPCOUNTER_GETMORE:
               alias: "OPCOUNTER_GETMORE" #Total number of getmore operations


     disks:
        - DISK_PARTITION_IOPS_READ:
            alias: "DISK_PARTITION_IOPS_READ" #Total number of commands issued to the DB
        - DISK_PARTITION_IOPS_WRITE:
            alias: "DISK_PARTITION_IOPS_WRITE" #Total number of insert operations
        - DISK_PARTITION_IOPS_UTILIZATION:
            alias: "DISK_PARTITION_IOPS_UTILIZATION" #Total number of query operations
        - DISK_PARTITION_LATENCY_READ:
            alias: "DISK_PARTITION_LATENCY_READ" #Total number of update operations
        - DISK_PARTITION_LATENCY_WRITE:
            alias: "DISK_PARTITION_LATENCY_WRITE" #Total number of delete operations
        - DISK_PARTITION_SPACE_PERCENT_USED:
            alias: "DISK_PARTITION_SPACE_PERCENT_USED" #Total number of getmore operations

     database:
        - DATABASE_AVERAGE_OBJECT_SIZE:
            alias: "DATABASE_AVERAGE_OBJECT_SIZE" #Total number of commands issued to the DB
        - DATABASE_COLLECTION_COUNT:
            alias: "DATABASE_COLLECTION_COUNT" #Total number of insert operations
        - DATABASE_DATA_SIZE:
            alias: "DATABASE_DATA_SIZE" #Total number of query operations
        - DATABASE_STORAGE_SIZE:
            alias: "DATABASE_STORAGE_SIZE" #Total number of update operations
        - DATABASE_INDEX_SIZE:
            alias: "DATABASE_INDEX_SIZE" #Total number of delete operations
        - DATABASE_INDEX_COUNT:
            alias: "DATABASE_INDEX_COUNT" #Total number of getmore operations

</pre>

### Workbench

Workbench is a feature by which you can preview the metrics before registering it with the controller. This is useful if you want to fine tune the configurations. Workbench is embedded into the extension jar.  
To use the workbench

1.  Follow all the installation steps
2.  Start the workbench with the command

    <pre>      java -jar <machine-agent-dir>/monitors/MongoDBOpsManagerMonitor/mongodb-opsmanager-monitoring-extension.jar</machine-agent-dir> </pre>

    This starts an http server at http://host:9090/. This can be accessed from the browser.
3.  If the server is not accessible from outside/browser, you can use the following end points to see the list of registered metrics and errors.

    <pre>#Get the stats
        curl http://localhost:9090/api/stats
        #Get the registered metrics
        curl http://localhost:9090/api/metric-paths
    </pre>

4.  You can make the changes to config.yml and validate it from the browser or the API
5.  Once the configuration is complete, you can kill the workbench and start the Machine Agent.

### Support

Please contact [help@appdynamics.com](mailto:help@appdynamics.com) with the following details

1.  config.yml
2.  debug logs

### Compatibility

<table border="0" cellpadding="0">

<tbody>

<tr>

<td style="text-align: right; width: 210px">Version</td>

<td>2.1.0</td>

</tr>

<tr>

<td style="text-align: right">Machine Agent Compatibility</td>

<td>4.0+</td>

</tr>

</tbody>

</table>

### Codebase

You can contribute your development ideas [here.](https://github.com/Appdynamics/mongodb-opsmanager-monitoring-extension)
