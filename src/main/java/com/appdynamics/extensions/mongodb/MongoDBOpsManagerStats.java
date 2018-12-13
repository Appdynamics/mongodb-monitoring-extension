/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.mongodb;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.mongodb.metrics.MetricProperties;
import com.appdynamics.extensions.mongodb.metrics.MetricPropertiesBuilder;
import com.appdynamics.extensions.mongodb.metrics.MongoDeploymentMetricManager;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.google.common.collect.Maps;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.mongodb.helpers.Constants.*;
import static com.appdynamics.extensions.mongodb.metrics.MetricPropertiesBuilder.DELTA_CALCULATOR;

/**
 * Created by aditya.jagtiani on 6/27/17.
 */

class MongoDBOpsManagerStats {
    private MonitorConfiguration configuration;
    private String serverUrl;

    MongoDBOpsManagerStats(MonitorConfiguration configuration, Map server) {
        this.configuration = configuration;
        this.serverUrl = UrlBuilder.fromYmlServerConfig(server).build();
    }

    Map<String, BigDecimal> populateMetrics() throws IOException {
        Map<String, BigDecimal> opsManagerMetrics = Maps.newHashMap();
        CloseableHttpClient httpClient = configuration.getHttpClient();
        Map<String, ?> config = configuration.getConfigYml();
        List<String> databasesFromCfg = (List) config.get("databases");
        MongoDeploymentMetricManager mongoDeploymentMetricManager = new MongoDeploymentMetricManager(serverUrl, httpClient);
        opsManagerMetrics.putAll(mongoDeploymentMetricManager.populateStats((Map) config.get("metrics"), databasesFromCfg));
        return opsManagerMetrics;
    }

    void printMetrics(Map<String, BigDecimal> mongoDBMetrics) {
        MetricWriteHelper metricWriter = configuration.getMetricWriter();
        String metricPrefix = configuration.getMetricPrefix();
        String aggregationType = DEFAULT_AGGREGATION_TYPE;
        String clusterRollupType = DEFAULT_CLUSTER_ROLLUP_TYPE;
        String timeRollupType = DEFAULT_TIME_ROLLUP_TYPE;
        Map<String, MetricProperties> metricOverrides = MetricPropertiesBuilder.getMetricPropsMap();

        for (Map.Entry<String, BigDecimal> metric : mongoDBMetrics.entrySet()) {
            String metricPath = metricPrefix + METRIC_SEPARATOR + metric.getKey();
            String metricName = metric.getKey();
            BigDecimal metricValue = metric.getValue();
            if (metricOverrides.containsKey(metricName)) {
                MetricProperties propertiesForCurrentMetric = metricOverrides.get(metricName);
                metricPath = metricPrefix + METRIC_SEPARATOR + propertiesForCurrentMetric.getMetricPath() + propertiesForCurrentMetric.getAlias();
                metricValue = metric.getValue().multiply(new BigDecimal(propertiesForCurrentMetric.getMultiplier()));
                if (propertiesForCurrentMetric.getDelta()) {
                    metricValue = DELTA_CALCULATOR.calculateDelta(metricPath, metricValue);
                }
                aggregationType = propertiesForCurrentMetric.getAggregationType();
                clusterRollupType = propertiesForCurrentMetric.getClusterRollupType();
                timeRollupType = propertiesForCurrentMetric.getTimeRollupType();
            }
            if (metricValue != null) {
                metricWriter.printMetric(metricPath, String.valueOf(metricValue.setScale(0, RoundingMode.CEILING)), aggregationType, timeRollupType, clusterRollupType);
            }
        }
    }
}
