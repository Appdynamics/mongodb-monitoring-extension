/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.mongodb.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.mongodb.helpers.Constants.*;

public class MongoDBOpsManagerUtils {
    private static final Logger logger = LoggerFactory.getLogger(MongoDBOpsManagerUtils.class);

    public static JsonNode getJsonNode(CloseableHttpResponse response) throws IOException {
        String data = EntityUtils.toString(response.getEntity(), "UTF-8");
        return getJsonNode(data);
    }

    private static JsonNode getJsonNode(String data) throws IOException {
        if (data == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(data, JsonNode.class);
    }

    public static BigDecimal convertDoubleToBigDecimal(Double value) {
        return new BigDecimal(Math.round(value));
    }

    public static boolean isValidationSuccessful(List<Map> configuredMetrics, List<JsonNode> mongoEntitiesInApplication, String entityType) {
        if (configuredMetrics == null || configuredMetrics.isEmpty()) {
            logger.info("No metrics have been configured for : " + entityType + " in your config.yml.");
            return false;
        } 
        else if (entityType.equals("alerts")) {
        	return true;
        } 
        else if (mongoEntitiesInApplication == null || mongoEntitiesInApplication.isEmpty()) {
            logger.info("No " + entityType + " found for the current Mongo Deployment");
            return false;
        }
        return true;
    }

    public static List<JsonNode> getMeasurementsOnlyForCurrentMetricType(String metricType, List<JsonNode> allMeasurements) {
        List<JsonNode> measurementsForCurrentMetricType = Lists.newArrayList();
        for (JsonNode node : allMeasurements) {
            if (node.findValue("name").asText().toLowerCase().contains(metricType.toLowerCase())) {
                measurementsForCurrentMetricType.add(node);
            }
        }
        return measurementsForCurrentMetricType;
    }

    public static String processTemplate(String template, 
    									 String host, 
    									 String project, 
    									 String cluster,
    									 String entity) {
    	StringBuffer outb = new StringBuffer(template);
    	replaceString(outb,"<project>",project);
    	replaceString(outb,"<cluster>",cluster);
    	replaceString(outb,"<host>",host);
    	replaceString(outb,"<entity>",entity);
        return outb.toString();
    }

    private static void replaceString(StringBuffer buf, String from, String to) {
    	int begin = buf.indexOf(from);
    	int end = begin + from.length();
    	if (begin >= 0) {
    		String before = METRIC_SEPARATOR;
    		if (begin != 0) {
    			before = buf.substring(begin-1, begin);
    		}
    		String after = METRIC_SEPARATOR;
    		if (buf.length() > end) {
    			after = buf.substring(end, end+1);
    		}
    		if (!before.equals(METRIC_SEPARATOR) & !before.equals(">") & !to.equals("")) {
        		to = METRIC_TEMPLATE_SEPARATOR + to;
    		}
    		if (!after.equals(METRIC_SEPARATOR) & !after.equals("<") & !to.equals("")) {
    			to = to + METRIC_TEMPLATE_SEPARATOR;
    		}
    		if (!before.equals(METRIC_SEPARATOR) & !before.equals(">") &!after.equals(METRIC_SEPARATOR) & !after.equals("<") &  to.equals("")) {
        		to = METRIC_TEMPLATE_SEPARATOR;
    		}
    		buf.replace(begin, end, to);
    	}
        return;
    }

    public static String getJsonValue(JsonNode node, String inKey) {
    	JsonNode valueNode = node.findValue(inKey);
    	String value = "";
    	if (valueNode != null) {
    		value = valueNode.asText();
    	}
        return value;
    }

    // Test utility for building json to mimic Rest API data from Ops Manager
    public static JsonNode getHttpTestData(String url) throws IOException {
    	String http = "";
    	if (url.endsWith(GROUPS_ENDPOINT)) {
    		http = "{\"results\": [{\"id\": \"g1\",\"name\": \"Group1\"}]}]}";
    	}
    	if (url.endsWith(ALERTS_ENDPOINT) | url.endsWith(ALERTS_PROJECT_ENDPOINT)) {
    		http="{\"results\":[{\"eventTypeName\":\"OUTSIDE_METRIC_THRESHOLD\",\"clusterName\":\"cl1\",\"groupId\":\"g1\",\"hostId\":\"hid1\",\"hostnameAndPort\":\"h1:27017\",\"metricName\":\"NORMALIZED_SYSTEM_CPU_STEAL\",\"status\":\"OPEN\"},{\"eventTypeName\":\"MONITORING_AGENT_DOWN\",\"clusterName\":\"cl2\",\"groupId\":\"g1\",\"hostId\":\"hid1\",\"hostnameAndPort\":\"h1:27017\",\"metricName\":\"NORMALIZED_SYSTEM_CPU_STEAL\",\"status\":\"OPEN\"}]}";
    	}
    	if (url.endsWith(HOSTS_ENDPOINT)) {
    	 	http = "{\"results\": [{\"id\": \"hid1\",\"hostname\": \"h1\",\"clusterId\": \"cli3\"},{\"id\": \"hid2\",\"hostname\": \"h2\",\"clusterId\": \"cli2\"}]}";
    	}
    	if (url.endsWith(CLUSTERS_ENDPOINT)) {
    		http = "{\"results\":[{\"clusterName\":\"cl1\",\"id\":\"cli3\"},{\"clusterName\":\"cl2\",\"id\":\"cli2\"}]}";
    	}
    	
    	if (url.endsWith(MEASUREMENTS_ENDPOINT)) {
    		http = "{\"measurements\":[{\"dataPoints\":[{\"value\": 5.0}],\"name\": \"CONNECTIONS\"},{\"dataPoints\":[{\"value\": 5.0}],\"name\": \"DATABASE_DATA_SIZE\"},{\"dataPoints\":[{\"value\": 3.4}],\"name\": \"DISK_PARTITION_IOPS_UTILIZATION\"},{\"dataPoints\": [{\"value\": 5.2}],\"name\": \"NETWORK_BYTES_IN\"}]}";
    	}
    	if (url.endsWith(DB_ENDPOINT)) {
    		http = "{\"results\": [{\"databaseName\": \"test\"},{\"databaseName\": \"local\"}]}";
    	}
    	if (url.endsWith(DISKS_ENDPOINT)) {
    		http = "{\"results\": [{\"partitionName\": \"pt1\"},{\"partitionName\": \"pt2\"}]}";
    	}
    	if (url.endsWith(AGENTS_BACKUP_ENDPOINT) ||
    			url.endsWith(AGENTS_AUTOMATION_ENDPOINT)
    			) {
    		http = "{\"results\":[{\"hostname\":\"h1\",\"stateName\":\"s1\"},{\"hostname\":\"h2\",\"stateName\":\"c2\"}]}";
    	}
    	if (url.endsWith(AGENTS_MONITORING_ENDPOINT)) {
    		http = "{\"results\":[{\"hostname\":\"h5\",\"stateName\":\"s1\"},{\"hostname\":\"h2\",\"stateName\":\"ACTIVE\"}]}";
    	}
    	
        return getJsonNode(http);
    }
}

