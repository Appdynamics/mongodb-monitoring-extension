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

public class NetworkMetrics {
    private static final Logger logger = LoggerFactory.getLogger(NetworkMetrics.class);
    private static final String METRIC_TYPE = "NETWORK";
    private String hostName;
    private List<Map>networkMetricsFromConfig;
    private List<JsonNode> networkMetricsFromHost;

    public NetworkMetrics(String hostName, List<JsonNode> networkMetricsFromHost, List<Map> networkMetricsFromConfig) {
        this.hostName = hostName;
        this.networkMetricsFromHost = networkMetricsFromHost;
        this.networkMetricsFromConfig = networkMetricsFromConfig;
    }

    public Map<String, BigDecimal> populateMetrics() throws IOException {
        if(!MongoDBOpsManagerUtils.isValidationSuccessful(networkMetricsFromConfig, networkMetricsFromHost, METRIC_TYPE)) {
            return Maps.newHashMap();
        }
        Map<String, BigDecimal> networkMetrics = Maps.newHashMap();
        for(JsonNode node : networkMetricsFromHost) {
            String networkMetricName = node.findValue("name").asText();
            String currentNetworkMetricPath = "Hosts" + METRIC_SEPARATOR + hostName + METRIC_SEPARATOR + "Network" + METRIC_SEPARATOR;
            logger.info("Fetching network metrics for host " +hostName);
            for(Map metric : networkMetricsFromConfig) {
                Map.Entry<String, String> entry = (Map.Entry) metric.entrySet().iterator().next();
                String metricName = entry.getKey();
                if(networkMetricName.equals(metricName) && !node.get("dataPoints").findValue("value").asText().equals("null")) {
                    networkMetrics.put(currentNetworkMetricPath + metricName, MongoDBOpsManagerUtils.convertDoubleToBigDecimal(node.get("dataPoints").findValue("value").asDouble()));
                    MetricPropertiesBuilder.buildMetricPropsMap(metric, metricName, currentNetworkMetricPath);
                }
                else {
                    logger.debug("Metric " +metricName+ "not found for host " +hostName+ ". Please verify whether correct metric names have been configured in the config.yml");
                }
            }
        }
        return networkMetrics;
    }
}
