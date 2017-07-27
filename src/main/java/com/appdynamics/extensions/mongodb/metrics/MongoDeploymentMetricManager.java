package com.appdynamics.extensions.mongodb.metrics;

import com.appdynamics.extensions.mongodb.helpers.HttpHelper;
import com.appdynamics.extensions.mongodb.helpers.MongoDBOpsManagerUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.util.JSON;
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

    public MongoDeploymentMetricManager(String serverUrl, CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        this.serverUrl = serverUrl;
    }

    public Map<String, BigDecimal> populateStats(Map<String, Map> includedMetrics) throws IOException {
        Map<String, BigDecimal> deploymentMetrics = Maps.newHashMap();
        String groupsUrl = buildUrl(serverUrl, GROUPS_ENDPOINT);
        List<JsonNode> groups = fetchMongoEntity(groupsUrl, "results");
        for(JsonNode group : groups) {
            String groupId = group.get("id").asText();
            String hostsUrl = buildUrl(groupsUrl, groupId + HOSTS_ENDPOINT);
            List<JsonNode> hosts = fetchMongoEntity(hostsUrl, "results");
            for(JsonNode host : hosts) {
                String hostName = host.findValue("hostname").asText();
                String hostId = host.findValue("id").asText();
                String measurementsUrl = buildUrl(hostsUrl, hostId + MEASUREMENTS_ENDPOINT);
                List<JsonNode> measurements = fetchMongoEntity(measurementsUrl, "measurements");
                /*deploymentMetrics.putAll(new MongoDbMetricProcessor(hostName, "asserts",
                        MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("assert", measurements),
                        (List) includedMetrics.get("asserts")).populateMetrics());
                deploymentMetrics.putAll(new MongoDbMetricProcessor(hostName, "memory",
                        MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("memory", measurements),
                        (List) includedMetrics.get("memory")).populateMetrics());
                deploymentMetrics.putAll(new MongoDbMetricProcessor(hostName, "network",
                        MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("network", measurements),
                        (List) includedMetrics.get("network")).populateMetrics());
                deploymentMetrics.putAll(new MongoDbMetricProcessor(hostName, "connections",
                        MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("connections", measurements),
                        (List) includedMetrics.get("connections")).populateMetrics());
                deploymentMetrics.putAll(new MongoDbMetricProcessor(hostName, "operations",
                        MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("opcounter", measurements),
                        (List) includedMetrics.get("operations")).populateMetrics());
*/

                deploymentMetrics.putAll(getDBStats(hostName, hostsUrl, hostId, includedMetrics.get("database")));

            }
        }
        return deploymentMetrics;
    }

    


    private Map<String, BigDecimal> getDBStats(String hostName, String hostsUrl, String hostId, Map includedDbMetrics) throws IOException {
        Map<String, BigDecimal> dbMetrics = Maps.newHashMap();
        String dbUrl = buildUrl(hostsUrl, hostId + DB_ENDPOINT);
        List<JsonNode> databases = fetchMongoEntity(dbUrl, "results");
        for(JsonNode database : databases) {
            String dbName = database.findValue("databaseName").asText();
            String dbMeasurementsUrl = buildUrl(dbUrl, dbName + MEASUREMENTS_ENDPOINT);
            List<JsonNode> dbMeasurements = fetchMongoEntity(dbMeasurementsUrl, "measurements");
            dbMetrics.putAll(new MongoDbMetricProcessor(hostName, "database",
                    MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("database", dbMeasurements),
                    (List) includedDbMetrics).populateMetrics());
            return dbMetrics;
        }
    }



    private String buildUrl(String serverUrl, String suffix) {
        StringBuilder url = new StringBuilder(serverUrl);
        return url.append(suffix).toString();
    }

    private List<JsonNode> fetchMongoEntity(String url, String entityName) throws IOException {
        CloseableHttpResponse httpResponse = null;
        List<JsonNode> entities = Lists.newArrayList();
        try {
            httpResponse = HttpHelper.doGet(httpClient, url);
            JsonNode jsonNode = MongoDBOpsManagerUtils.getJsonNode(httpResponse);
            for(JsonNode node : jsonNode.get(entityName)) {
                entities.add(node);
            }
        }
        catch(Exception ex) {
            logger.error("Error while fetching results from url " +url);
        }
        finally {
            HttpHelper.closeHttpResponse(httpResponse);
        }
        return entities;
    }
}
