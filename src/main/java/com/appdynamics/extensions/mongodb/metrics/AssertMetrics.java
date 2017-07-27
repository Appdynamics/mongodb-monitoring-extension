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
public class AssertMetrics {
    private static final Logger logger = LoggerFactory.getLogger(AssertMetrics.class);
    private static final String METRIC_TYPE = "ASSERTS";
    private String hostName;
    private List<Map>assertMetricsFromConfig;
    private List<JsonNode> assertsFromHost;

    public AssertMetrics(String hostName, List<JsonNode> assertsFromHost, List<Map> assertMetricsFromConfig) {
        this.hostName = hostName;
        this.assertsFromHost = assertsFromHost;
        this.assertMetricsFromConfig = assertMetricsFromConfig;
    }

    public Map<String, BigDecimal> populateMetrics() throws IOException {
        if(!MongoDBOpsManagerUtils.isValidationSuccessful(assertMetricsFromConfig, assertsFromHost, METRIC_TYPE)) {
            return Maps.newHashMap();
        }
        Map<String, BigDecimal> assertMetrics = Maps.newHashMap();
        for(JsonNode node : assertsFromHost) {
            String assertName = node.findValue("name").asText();
            String currentAssertMetricPath = "Hosts" + METRIC_SEPARATOR + hostName + METRIC_SEPARATOR + "Asserts" + METRIC_SEPARATOR;
            logger.info("Fetching assert metrics for host " +hostName);
            for(Map metric : assertMetricsFromConfig) {
                Map.Entry<String, String> entry = (Map.Entry) metric.entrySet().iterator().next();
                String metricName = entry.getKey();
                if(assertName.equals(metricName) && !node.get("dataPoints").findValue("value").asText().equals("null")) {
                    assertMetrics.put(currentAssertMetricPath + metricName, MongoDBOpsManagerUtils.convertDoubleToBigDecimal(node.get("dataPoints").findValue("value").asDouble()));
                    MetricPropertiesBuilder.buildMetricPropsMap(metric, metricName, currentAssertMetricPath);
                }
                else {
                    logger.debug("Metric " +metricName+ "not found for host " +hostName+ ". Please verify whether correct metric names have been configured in the config.yml");
                }
            }
        }
        return assertMetrics;
    }
}
