/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.mongodb.metrics;

import com.appdynamics.extensions.mongodb.helpers.MongoDBOpsManagerUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.mongodb.helpers.Constants.METRIC_SEPARATOR;

/**
 * Created by aditya.jagtiani on 7/12/17.
 */

class MongoDbMetricProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbMetricProcessor.class);
    private String hostName;
    private List<Map> metricsFromConfig;
    private List<JsonNode> metricsFromHost;
    private String metricType;
    private String entityName;

    MongoDbMetricProcessor(String hostName, String metricType, List<JsonNode> metricsFromHost,
                           List<Map> metricsFromConfig, String entityName) {
        this.hostName = hostName;
        this.metricType = metricType;
        this.metricsFromHost = metricsFromHost;
        this.metricsFromConfig = metricsFromConfig;
        this.entityName = entityName;
    }

    Map<String, BigDecimal> populateMetrics() {
        String currentMetricPath;
        if (!MongoDBOpsManagerUtils.isValidationSuccessful(metricsFromConfig, metricsFromHost, metricType)) {
            return Maps.newHashMap();
        }
        Map<String, BigDecimal> mongoDbMetrics = Maps.newHashMap();
        for (JsonNode node : metricsFromHost) {
            String currentMetricNameFromHost = node.findValue("name").asText();
            if (!entityName.equals("")) {
                currentMetricPath = "Hosts" + METRIC_SEPARATOR + hostName + METRIC_SEPARATOR + metricType +
                        METRIC_SEPARATOR + entityName + METRIC_SEPARATOR;
            } else {
                currentMetricPath = "Hosts" + METRIC_SEPARATOR + hostName + METRIC_SEPARATOR + metricType + METRIC_SEPARATOR;
            }
            logger.info("Fetching " + metricType + " metrics for host " + hostName);
            for (Map metric : metricsFromConfig) {
                Map.Entry<String, String> entry = (Map.Entry) metric.entrySet().iterator().next();
                String currentMetricNameFromCfg = entry.getKey();
                if(node.findValue("dataPoints").findValue("value") != null) {
                    logger.debug("Data point found for metric: {} in host: {}. Attempting to fetch value", currentMetricNameFromHost, hostName);
                    JsonNode value = node.findValue("dataPoints").findValue("value");
                    if (node.findValue("name").asText().equals(currentMetricNameFromCfg) && !value.asText().equals("null")) {
                        mongoDbMetrics.put(currentMetricPath + currentMetricNameFromCfg, MongoDBOpsManagerUtils.convertDoubleToBigDecimal(value.asDouble()));
                        if (entry.getValue() != null) {
                            MetricPropertiesBuilder.buildMetricPropsMap(metric, currentMetricNameFromCfg, currentMetricPath);
                        }
                        else {
                            logger.debug("Null value found for metric: {} in host: {}. Skipping", currentMetricNameFromHost, hostName);
                        }
                    }
                }
                else {
                    logger.debug("Metric " + currentMetricNameFromHost + " does not have any data points in : " + hostName + ". Skipping.");
                }
            }
        }
        return mongoDbMetrics;
    }
}
