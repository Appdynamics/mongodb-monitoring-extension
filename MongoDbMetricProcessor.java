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
 * Updated by Mark.Walmsley on 6/29/18.
 */

class MongoDbMetricProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbMetricProcessor.class);
    private String hostName;
    private List<Map> metricsFromConfig;
    private List<JsonNode> metricsFromHost;
    private String metricType;
    private String entityName;
    private String groupName;
    private String metricTemplate;

    MongoDbMetricProcessor(String hostName, 
    					   String metricType, 
    					   String groupName,
    					   String metricTemplate,
    					   List<JsonNode> metricsFromHost,
                           List<Map> metricsFromConfig, 
                           String entityName) {
        this.hostName = hostName;
        this.groupName = groupName;
        this.metricTemplate = metricTemplate;
        this.metricType = metricType;
        this.metricsFromHost = metricsFromHost;
        this.metricsFromConfig = metricsFromConfig;
        this.entityName = entityName;
    }

    Map<String, BigDecimal> populateMetrics() throws IOException {
        String currentMetricPath;
        if (!MongoDBOpsManagerUtils.isValidationSuccessful(metricsFromConfig, metricsFromHost, metricType)) {
            return Maps.newHashMap();
        }
        Map<String, BigDecimal> mongoDbMetrics = Maps.newHashMap();
        for (JsonNode node : metricsFromHost) {
            String currentMetricNameFromHost = node.findValue("name").asText();
            currentMetricPath = MongoDBOpsManagerUtils.processTemplate(metricTemplate, 
					  			hostName, 
					  			groupName, 
					  			"", 
					  			entityName);
            currentMetricPath += METRIC_SEPARATOR + metricType + METRIC_SEPARATOR;

            logger.debug("Fetching " + metricType + " metrics for host " + hostName);
            for (Map metric : metricsFromConfig) {
                Map.Entry<String, String> entry = (Map.Entry) metric.entrySet().iterator().next();
                String currentMetricNameFromCfg = entry.getKey();
                JsonNode value = node.findValue("dataPoints").findValue("value");
                if (node.findValue("name").asText().equals(currentMetricNameFromCfg) && value != null && !value.asText().equals("null")) {
                    mongoDbMetrics.put(currentMetricPath + currentMetricNameFromCfg, MongoDBOpsManagerUtils.convertDoubleToBigDecimal(value.asDouble()));
                    if (entry.getValue() != null) {
                        MetricPropertiesBuilder.buildMetricPropsMap(metric, currentMetricNameFromCfg, currentMetricPath);
                    }
                } else {
                    logger.debug("Metric " + currentMetricNameFromHost + " not found for host : " + hostName + " or it's value is null. Skipping.");
                }
            }
        }
        return mongoDbMetrics;
    }
}
