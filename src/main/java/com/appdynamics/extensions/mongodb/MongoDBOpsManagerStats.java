package com.appdynamics.extensions.mongodb;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.mongodb.metrics.MongoDeploymentMetricManager;
import com.google.common.collect.Maps;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by aditya.jagtiani on 6/27/17.
 */

public class MongoDBOpsManagerStats {
    private MonitorConfiguration configuration;
    private String serverUrl;

    public MongoDBOpsManagerStats(MonitorConfiguration configuration, Map server) {
        this.configuration = configuration;
        this.serverUrl = UrlBuilder.fromYmlServerConfig(server).build();
    }

    public Map<String, BigDecimal> populateMetrics() throws IOException{
        Map<String, BigDecimal> opsManagerMetrics = Maps.newHashMap();
        CloseableHttpClient httpClient = configuration.getHttpClient();
        Map<String, ?> config = configuration.getConfigYml();
        MongoDeploymentMetricManager mongoDeploymentMetricManager = new MongoDeploymentMetricManager(serverUrl, httpClient);
        opsManagerMetrics.putAll(mongoDeploymentMetricManager.populateStats((Map)config.get("metrics")));
        return opsManagerMetrics;
    }

    public void printMetrics(Map<String, BigDecimal> mongoDBMetrics) {

    }
}
