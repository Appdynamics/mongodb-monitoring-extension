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
public class MemoryMetrics {
    private static final Logger logger = LoggerFactory.getLogger(MemoryMetrics.class);
    private static final String METRIC_TYPE = "MEMORY";
    private String hostName;
    private List<Map>memoryMetricsFromConfig;
    private List<JsonNode> memoryMetricsFromHost;

    public MemoryMetrics(String hostName, List<JsonNode> memoryMetricsFromHost, List<Map> memoryMetricsFromConfig) {
        this.hostName = hostName;
        this.memoryMetricsFromHost = memoryMetricsFromHost;
        this.memoryMetricsFromConfig = memoryMetricsFromConfig;
    }

    public Map<String, BigDecimal> populateMetrics() throws IOException {
        if(!MongoDBOpsManagerUtils.isValidationSuccessful(memoryMetricsFromConfig, memoryMetricsFromHost, METRIC_TYPE)) {
            return Maps.newHashMap();
        }
        Map<String, BigDecimal> memoryMetrics = Maps.newHashMap();
        for(JsonNode node : memoryMetricsFromHost) {
            String memoryMetricName = node.findValue("name").asText();
            String currentMemoryMetricPath = "Hosts" + METRIC_SEPARATOR + hostName + METRIC_SEPARATOR + "Memory" + METRIC_SEPARATOR;
            logger.info("Fetching memory metrics for host " +hostName);
            for(Map metric : memoryMetricsFromConfig) {
                Map.Entry<String, String> entry = (Map.Entry) metric.entrySet().iterator().next();
                String metricName = entry.getKey();
                if(memoryMetricName.equals(metricName) && !node.get("dataPoints").findValue("value").asText().equals("null")) {
                    memoryMetrics.put(currentMemoryMetricPath + metricName, MongoDBOpsManagerUtils.convertDoubleToBigDecimal(node.get("dataPoints").findValue("value").asDouble()));
                    MetricPropertiesBuilder.buildMetricPropsMap(metric, metricName, currentMemoryMetricPath);
                }
                else {
                    logger.debug("Metric " +metricName+ "not found for host " +hostName+ ". Please verify whether correct metric names have been configured in the config.yml");
                }
            }
        }
        return memoryMetrics;
    }
}
