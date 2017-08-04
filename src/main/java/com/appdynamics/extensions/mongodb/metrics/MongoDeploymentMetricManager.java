package com.appdynamics.extensions.mongodb.metrics;

import com.appdynamics.extensions.mongodb.helpers.HttpHelper;
import com.appdynamics.extensions.mongodb.helpers.MongoDBOpsManagerUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.mongodb.helpers.Constants.*;
import static com.appdynamics.extensions.mongodb.helpers.MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType;

/**
 * Created by aditya.jagtiani on 7/12/17.
 */
public class MongoDeploymentMetricManager {
    private static final Logger logger = LoggerFactory.getLogger(MongoDeploymentMetricManager.class);
    private CloseableHttpClient httpClient;
    private String serverUrl;

    public MongoDeploymentMetricManager(String serverUrl, CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        this.serverUrl = serverUrl;
    }

    public Map<String, BigDecimal> populateStats(Map<String, Map> includedMetrics, List<String> databasesFromCfg) throws IOException {
        Map<String, BigDecimal> deploymentMetrics = Maps.newHashMap();
        String groupsUrl = buildUrl(serverUrl, GROUPS_ENDPOINT);
        List<JsonNode> groups = fetchMongoEntity(groupsUrl, "results");
        for (JsonNode group : groups) {
            String groupId = group.get("id").asText();
            String hostsUrl = buildUrl(groupsUrl, groupId + HOSTS_ENDPOINT);
            List<JsonNode> hosts = fetchMongoEntity(hostsUrl, "results");
            for (JsonNode host : hosts) {
                String hostName = host.findValue("hostname").asText();
                String hostId = host.findValue("id").asText();
                deploymentMetrics.putAll(getHostAndSystemStats(hostName, hostId, hostsUrl, includedMetrics));
                deploymentMetrics.putAll(getDBStats(hostName, hostId, hostsUrl, includedMetrics, databasesFromCfg));
                deploymentMetrics.putAll(getDiskPartitionStats(hostName, hostId, hostsUrl, includedMetrics));
            }
        }
        return deploymentMetrics;
    }
    private Map<String, BigDecimal> getHostAndSystemStats(String hostName, String hostId, String hostsUrl,
                                                          Map includedMetrics) throws IOException {
        logger.debug("Fetching Host/System stats from host : " +hostName);
        Map<String, BigDecimal> systemMetrics = Maps.newHashMap();
        String hostMeasurementsUrl = buildUrl(hostsUrl, hostId + MEASUREMENTS_ENDPOINT);
        List<JsonNode> measurements = fetchMongoEntity(hostMeasurementsUrl, "measurements");
        Map <String, List<Map>> hostMetricsFromCfg = (Map) includedMetrics.get("hosts");
        for(Map.Entry<String, List<Map>> entry : hostMetricsFromCfg.entrySet()) {
            systemMetrics.putAll(new MongoDbMetricProcessor(hostName, entry.getKey(),
                    getMeasurementsOnlyForCurrentMetricType(entry.getKey(), measurements), entry.getValue(), "").populateMetrics());
        }
        return systemMetrics;
    }

    private Map<String, BigDecimal> getDBStats(String hostName, String hostId, String hostsUrl, Map includedMetrics,
                                               List<String> databasesFromCfg) throws IOException {
        logger.debug("Fetching DB stats from host : " +hostName);
        Map<String, BigDecimal> dbMetrics = Maps.newHashMap();
        String dbUrl = buildUrl(hostsUrl, hostId + DB_ENDPOINT);
        List<JsonNode> databases = fetchMongoEntity(dbUrl, "results");
        for (JsonNode database : databases) {
            String dbName = database.findValue("databaseName").asText();
            if(databasesFromCfg.contains(dbName)) {
                String dbMeasurementsUrl = buildUrl(dbUrl, dbName + MEASUREMENTS_ENDPOINT);
                List<JsonNode> dbMeasurements = fetchMongoEntity(dbMeasurementsUrl, "measurements");
                dbMetrics.putAll(new MongoDbMetricProcessor(hostName, "database",
                        getMeasurementsOnlyForCurrentMetricType("database", dbMeasurements),
                        (List) includedMetrics.get("database"), dbName).populateMetrics());
            }
            else {
                logger.debug("Skipping database : " +dbName+ ". Please add it to the config.yml if you'd like it to be monitored.") ;
            }
        }
        return dbMetrics;
    }

    private Map<String, BigDecimal> getDiskPartitionStats(String hostName, String hostId, String hostsUrl, Map includedMetrics) throws IOException {
        logger.debug("Fetching disk and partition stats from host : " +hostName);
        Map<String, BigDecimal> diskPartitionMetrics = Maps.newHashMap();
        String disksUrl = buildUrl(hostsUrl, hostId + DISKS_ENDPOINT);
        List<JsonNode> disks = fetchMongoEntity(disksUrl, "results");
        for (JsonNode disk : disks) {
            String partitionName = disk.findValue("partitionName").asText();
            String partitionUrl = buildUrl(disksUrl, partitionName + MEASUREMENTS_ENDPOINT);
            List<JsonNode> diskMeasurements = fetchMongoEntity(partitionUrl, "measurements");
            diskPartitionMetrics.putAll(new MongoDbMetricProcessor(hostName, "disk",
                    getMeasurementsOnlyForCurrentMetricType("disk", diskMeasurements),
                    (List) includedMetrics.get("disks"), partitionName).populateMetrics());
        }
        return diskPartitionMetrics;
    }

    private String buildUrl(String serverUrl, String suffix) {
        StringBuilder url = new StringBuilder(serverUrl);
        return url.append(suffix).toString();
    }

    private List<JsonNode> fetchMongoEntity(String url, String entityName) throws IOException {
        CloseableHttpResponse httpResponse = null;
        List<JsonNode> details = Lists.newArrayList();
        try {
            httpResponse = HttpHelper.doGet(httpClient, url);
            JsonNode jsonNode = MongoDBOpsManagerUtils.getJsonNode(httpResponse);
            for (JsonNode node : jsonNode.get(entityName)) {
                details.add(node);
            }
        } catch (Exception ex) {
            logger.error("Error while fetching results from url " + url);
        } finally {
            HttpHelper.closeHttpResponse(httpResponse);
        }
        return details;
    }
}
