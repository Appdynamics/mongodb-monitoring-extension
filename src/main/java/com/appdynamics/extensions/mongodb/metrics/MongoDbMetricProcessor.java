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

public class MongoDbMetricProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbMetricProcessor.class);
    private String hostName;
    private List<Map>metricsFromConfig;
    private List<JsonNode> metricsFromHost;
    private String metricType;

    public MongoDbMetricProcessor(String hostName, String metricType, List<JsonNode> metricsFromHost, List<Map> metricsFromConfig) {
        this.hostName = hostName;
        this.metricType = metricType;
        this.metricsFromHost = metricsFromHost;
        this.metricsFromConfig = metricsFromConfig;
    }

    public Map<String, BigDecimal> populateMetrics() throws IOException {
        if(!MongoDBOpsManagerUtils.isValidationSuccessful(metricsFromConfig, metricsFromHost, metricType)) {
            return Maps.newHashMap();
        }
        Map<String, BigDecimal> mongoDbMetrics = Maps.newHashMap();
        for(JsonNode node : metricsFromHost) {
            String currentMetricNameFromHost = node.findValue("name").asText();
            String currentMetricPath = "Hosts" + METRIC_SEPARATOR + hostName + METRIC_SEPARATOR + metricType + METRIC_SEPARATOR;
            logger.info("Fetching assert metrics for host " +hostName);
            for(Map metric : metricsFromConfig) {
                Map.Entry<String, String> entry = (Map.Entry) metric.entrySet().iterator().next();
                String currentMetricNameFromCfg = entry.getKey();
                if(currentMetricNameFromHost.equals(currentMetricNameFromCfg) && !node.get("dataPoints").findValue("value").asText().equals("null")) {
                    mongoDbMetrics.put(currentMetricPath + currentMetricNameFromCfg, MongoDBOpsManagerUtils.convertDoubleToBigDecimal(node.get("dataPoints").findValue("value").asDouble()));
                    MetricPropertiesBuilder.buildMetricPropsMap(metric, currentMetricNameFromCfg, currentMetricPath);
                }
                else {
                    logger.debug("Metric " +currentMetricNameFromCfg+ "not found for host " +hostName+ ". Please verify whether correct metric names have been configured in the config.yml");
                }
            }
        }
        return mongoDbMetrics;
    }
}
