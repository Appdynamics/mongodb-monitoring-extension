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

/**
 * Created by aditya.jagtiani on 7/12/17.
 */
public class MongoDeploymentMetricManager {
    private static final Logger logger = LoggerFactory.getLogger(MongoDeploymentMetricManager.class);
    private CloseableHttpClient httpClient;
    private String serverUrl;
    private String hostName;
    private String hostId;
    private String currentEntityUrl;


    public MongoDeploymentMetricManager(String serverUrl, CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        this.serverUrl = serverUrl;
    }

    public Map<String, BigDecimal> populateStats(Map<String, Map> includedMetrics) throws IOException {
        Map<String, BigDecimal> deploymentMetrics = Maps.newHashMap();
        String groupsUrl = buildUrl(serverUrl, GROUPS_ENDPOINT);
        List<JsonNode> groups = fetchDetails(groupsUrl, "results");
        for (JsonNode group : groups) {
            String groupId = group.get("id").asText();
            String hostsUrl = buildUrl(groupsUrl, groupId + HOSTS_ENDPOINT);
            deploymentMetrics.putAll(getHostAndSystemStats(hostsUrl, includedMetrics));
            deploymentMetrics.putAll(getDBStats(hostsUrl, includedMetrics));
            deploymentMetrics.putAll(getDiskPartitionStats(hostsUrl, includedMetrics));
        }
        return deploymentMetrics;
    }


    private Map<String, BigDecimal> getHostAndSystemStats(String hostsUrl, Map includedMetrics) throws IOException {
        Map<String, BigDecimal> systemMetrics = Maps.newHashMap();
        List<JsonNode> hosts = fetchMongoEntities("", hostsUrl);
        for (JsonNode host : hosts) {
            hostName = host.findValue("hostname").asText();
            hostId = host.findValue("id").asText();
            String hostMeasurementsUrl = buildUrl(hostsUrl, hostId + MEASUREMENTS_ENDPOINT);
            List<JsonNode> measurements = fetchDetails(hostMeasurementsUrl, "measurements");
            systemMetrics.putAll(new MongoDbMetricProcessor(hostName, "asserts",
                    MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("assert", measurements),
                    (List) includedMetrics.get("asserts")).populateMetrics());
            systemMetrics.putAll(new MongoDbMetricProcessor(hostName, "memory",
                    MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("memory", measurements),
                    (List) includedMetrics.get("memory")).populateMetrics());
            systemMetrics.putAll(new MongoDbMetricProcessor(hostName, "network",
                    MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("network", measurements),
                    (List) includedMetrics.get("network")).populateMetrics());
            systemMetrics.putAll(new MongoDbMetricProcessor(hostName, "connections",
                    MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("connections", measurements),
                    (List) includedMetrics.get("connections")).populateMetrics());
            systemMetrics.putAll(new MongoDbMetricProcessor(hostName, "operations",
                    MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("opcounter", measurements),
                    (List) includedMetrics.get("operations")).populateMetrics());
        }
        return systemMetrics;
    }

    private Map<String, BigDecimal> getDBStats(String hostsUrl, Map includedMetrics) throws IOException {
        Map<String, BigDecimal> dbMetrics = Maps.newHashMap();
        List<JsonNode> databases = fetchMongoEntities(DB_ENDPOINT, hostsUrl);
        for (JsonNode database : databases) {
            String dbName = database.findValue("databaseName").asText();
            String dbMeasurementsUrl = buildUrl(currentEntityUrl, dbName + MEASUREMENTS_ENDPOINT);
            List<JsonNode> dbMeasurements = fetchMongoEntities(dbMeasurementsUrl, "measurements");
            dbMetrics.putAll(new MongoDbMetricProcessor(hostName, "database",
                    MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("database", dbMeasurements),
                    (List) includedMetrics.get("database")).populateMetrics());
        }

        return dbMetrics;
    }

    private Map<String, BigDecimal> getDiskPartitionStats(String hostsUrl, Map includedMetrics) throws IOException {
        Map<String, BigDecimal> diskPartitionMetrics = Maps.newHashMap();
        List<JsonNode> disks = fetchMongoEntities(DISKS_ENDPOINT, hostsUrl);
        for (JsonNode disk : disks) {
            String partitionName = disk.findValue("partitionName").asText();
            String partitionUrl = buildUrl(currentEntityUrl, partitionName + MEASUREMENTS_ENDPOINT);
            List<JsonNode> dbMeasurements = fetchDetails(partitionUrl, "measurements");
            diskPartitionMetrics.putAll(new MongoDbMetricProcessor(hostName, "disk",
                    MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("disk", dbMeasurements),
                    (List) includedMetrics.get("disk")).populateMetrics());
        }
        return diskPartitionMetrics;
    }

    private List<JsonNode> fetchMongoEntities(String endpoint, String hostsUrl) throws IOException {
        List<JsonNode> entities = Lists.newArrayList();
        List<JsonNode> hosts = fetchDetails(hostsUrl, "results");
        if(endpoint == null || endpoint.isEmpty()) {
            return hosts;
        }
        for (JsonNode host : hosts) {
            hostName = host.findValue("hostname").asText();
            hostId = host.findValue("id").asText();
            currentEntityUrl = buildUrl(hostsUrl, hostId + endpoint);
            entities = fetchDetails(currentEntityUrl, "results");
        }
        return entities;
    }

    private String buildUrl(String serverUrl, String suffix) {
        StringBuilder url = new StringBuilder(serverUrl);
        return url.append(suffix).toString();
    }

    private List<JsonNode> fetchDetails(String url, String entityName) throws IOException {
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
