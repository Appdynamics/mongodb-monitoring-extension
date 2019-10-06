/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.mongodb.metrics;

import com.appdynamics.extensions.mongodb.helpers.HttpHelper;
import com.appdynamics.extensions.mongodb.helpers.MongoDBOpsManagerUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.text.SimpleDateFormat;
import java.time.*;

import static com.appdynamics.extensions.mongodb.helpers.Constants.*;
import static com.appdynamics.extensions.mongodb.helpers.MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType;

/**
 * Created by aditya.jagtiani on 7/12/17.
 * Updated by Mark.Walmsley on 6/29/18.
 */
public class MongoDeploymentMetricManager {
    private static final Logger logger = LoggerFactory.getLogger(MongoDeploymentMetricManager.class);
    private CloseableHttpClient httpClient;
    private String serverUrl;
    private String metricTemplateFromCfg;
    private List <String> databasesFromCfg;
    private List <String> projectsFromCfg;

    public MongoDeploymentMetricManager(String serverUrl, CloseableHttpClient httpClient, String inTemplate, List <String> inDb, List <String> inPr) {
        this.httpClient = httpClient;
        this.serverUrl = serverUrl;
        this.databasesFromCfg = inDb;
        this.projectsFromCfg = inPr;
        this.metricTemplateFromCfg = inTemplate;
    }

    public Map<String, BigDecimal> populateStats(Map<String, Map> includedMetrics) throws IOException {


        logger.debug("MongoDB Trace: Starting MongoDB data collection");
        
      
    	Map<String, BigDecimal> deploymentMetrics = Maps.newHashMap();
    	
    	// Fetch the list of groups/projects from Ops Manager
        String groupsUrl = buildUrl(serverUrl, GROUPS_ENDPOINT);
        List<JsonNode> groups = fetchMongoEntity(groupsUrl, "results");
        logger.debug("MongoDB Trace: fetched Projects");
        
    	// loop through all the groups/projects processing the metrics for each of them
        for (JsonNode group : groups) {
            String groupId = group.get("id").asText();
            String groupName = group.get("name").asText();
            if (projectsFromCfg != null) {
            	if (!projectsFromCfg.contains(groupName)) {
            		System.out.println("Projects: " + projectsFromCfg + " project: " + groupName);
            		continue;
            	}
            }
            logger.debug("MongoDB Trace: Processing Project" + groupName);
            
            String alertsUrl = buildUrl(serverUrl, GROUPS_ENDPOINT + groupId + ALERTS_PROJECT_ENDPOINT);
            List<JsonNode> projectAlerts = fetchMongoEntity(alertsUrl, "results");
           
            alertsUrl = buildUrl(serverUrl, ALERTS_ENDPOINT);           
            List<JsonNode> alerts = fetchMongoEntity(alertsUrl, "results");
            
            String hostsUrl = buildUrl(groupsUrl, groupId + HOSTS_ENDPOINT);
            List<JsonNode> hosts = fetchMongoEntity(hostsUrl, "results");
            
            String clusterUrl = buildUrl(groupsUrl, groupId + CLUSTERS_ENDPOINT);
            List<JsonNode> clusters = fetchMongoEntity(clusterUrl, "results");           


            // Create a hashset to hold the cluster names as we can have duplicate 
            // clusters with the same name
            HashSet clusterHs = new HashSet();
            String clusterName;
            for (JsonNode cluster : clusters) {
            	clusterName = cluster.findValue("clusterName").asText();
            	clusterHs.add(clusterName);
            }
            
            // Process cluster only alerts
        	String agentType = "ClusterOnly";
            for (Object cl : clusterHs) {
            	deploymentMetrics.putAll(getAlertStats("projectalerts", groupId, groupName, "", (String)cl, agentType, projectAlerts, includedMetrics));
            	deploymentMetrics.putAll(getAlertStats("alerts", groupId, groupName, "", (String)cl, agentType, alerts, includedMetrics));
            }
            
            // Process alerts that are of type Agent that don't have a host in the alert API
            HashSet agentHosts = new HashSet();
            agentHosts = getHostsForAgent(buildUrl(groupsUrl, groupId + AGENTS_MONITORING_ENDPOINT));
            for (JsonNode host : hosts) {
                String hostName = host.findValue("hostname").asText();
                agentType = "monitoringAgentUp";
                if (agentHosts.contains(hostName)) {
                	agentType = "monitoringAgentDown";
                }
            	deploymentMetrics.putAll(getAlertStats("projectalerts", groupId, groupName, hostName, "", agentType, projectAlerts, includedMetrics));
            	deploymentMetrics.putAll(getAlertStats("alerts", groupId, groupName, hostName, "", agentType, alerts, includedMetrics));
            }

        	agentHosts = getHostsForAgent(buildUrl(groupsUrl, groupId + AGENTS_BACKUP_ENDPOINT));
            for (JsonNode host : hosts) {
                String hostName = host.findValue("hostname").asText();
                agentType = "backupAgentUp";
                if (agentHosts.contains(hostName)) {
                	agentType = "backupAgentDown";
                }
            	deploymentMetrics.putAll(getAlertStats("projectalerts", groupId, groupName, hostName, "", agentType, projectAlerts, includedMetrics));
            	deploymentMetrics.putAll(getAlertStats("alerts", groupId, groupName, hostName, "", agentType, alerts, includedMetrics));
            }
            
            agentHosts = getHostsForAgent(buildUrl(groupsUrl, groupId + AGENTS_AUTOMATION_ENDPOINT));
            for (JsonNode host : hosts) {
                String hostName = host.findValue("hostname").asText();
                agentType = "automationAgentUp";
                if (agentHosts.contains(hostName)) {
                	agentType = "automationAgentDown";
                }
            	deploymentMetrics.putAll(getAlertStats("projectalerts", groupId, groupName, hostName, "", agentType, projectAlerts, includedMetrics));
            	deploymentMetrics.putAll(getAlertStats("alerts", groupId, groupName, hostName, "", agentType, alerts, includedMetrics));
            }
            
            agentType = "Agent";
        	deploymentMetrics.putAll(getAlertStats("projectalerts", groupId, groupName, "", "", agentType, projectAlerts, includedMetrics));
        	deploymentMetrics.putAll(getAlertStats("alerts", groupId, groupName, "", "", agentType, alerts, includedMetrics));

              
            agentType = "Host";
            for (JsonNode host : hosts) {
                String hostName = MongoDBOpsManagerUtils.getJsonValue(host, "hostname");
                String hostId = MongoDBOpsManagerUtils.getJsonValue(host, "id");
                String clusterIdFromHost = MongoDBOpsManagerUtils.getJsonValue(host, "clusterId");
                
                clusterName = "";
                for (JsonNode cluster : clusters) {
                	String clusterId = MongoDBOpsManagerUtils.getJsonValue(cluster, "id");
                	if (clusterId.equals(clusterIdFromHost)) {
                		clusterName = MongoDBOpsManagerUtils.getJsonValue(cluster, "clusterName");
                		break;
                	}
                }
                
            	deploymentMetrics.putAll(getAlertStats("projectalerts", groupId, groupName, hostName, clusterName, agentType, projectAlerts, includedMetrics));
            	deploymentMetrics.putAll(getAlertStats("alerts", groupId, groupName, hostName, clusterName, agentType, alerts, includedMetrics));
                
                //deploymentMetrics.putAll(getHostAndSystemStats(hostName, hostId, hostsUrl, groupName, includedMetrics));
                //deploymentMetrics.putAll(getDBStats(hostName, hostId, hostsUrl, groupName, includedMetrics));
                //deploymentMetrics.putAll(getDiskPartitionStats(hostName, hostId, hostsUrl, groupName, includedMetrics));
            }
            logger.debug("MongoDB Trace: Processed Project" + groupName);
        }
        return deploymentMetrics;
    }
    private Map<String, BigDecimal> getHostAndSystemStats(String hostName, 
    													  String hostId, 
    													  String hostsUrl,
    													  String groupName,
                                                          Map includedMetrics) throws IOException {
        logger.info("MongoDB Trace: Fetching Host/System stats from host : " + hostName);
        Map<String, BigDecimal> systemMetrics = Maps.newHashMap();
        String hostMeasurementsUrl = buildUrl(hostsUrl, hostId + MEASUREMENTS_ENDPOINT);
        List<JsonNode> measurements = fetchMongoEntity(hostMeasurementsUrl, "measurements");
        Map <String, List<Map>> hostMetricsFromCfg = (Map) includedMetrics.get("hosts");
        if (hostMetricsFromCfg == null) {
        	return systemMetrics;
        }
        for(Map.Entry<String, List<Map>> entry : hostMetricsFromCfg.entrySet()) {
            systemMetrics.putAll(new MongoDbMetricProcessor(hostName, 
            												entry.getKey(),
            												groupName,
            												metricTemplateFromCfg,
            												getMeasurementsOnlyForCurrentMetricType(entry.getKey(), measurements),
            												entry.getValue(),
            												"").populateMetrics());
        }
        return systemMetrics;
    }

    private Map<String, BigDecimal> getDBStats(String hostName, 
    										   String hostId, 
    										   String hostsUrl, 
    										   String groupName,
    										   Map includedMetrics) throws IOException {
        logger.info("MongoDB Trace: Fetching DB stats from host : " + hostName);
        Map<String, BigDecimal> dbMetrics = Maps.newHashMap();
        String dbUrl = buildUrl(hostsUrl, hostId + DB_ENDPOINT);
        List<JsonNode> databases = fetchMongoEntity(dbUrl, "results");
        for (JsonNode database : databases) {
            String dbName = database.findValue("databaseName").asText();
            if(databasesFromCfg.contains(dbName)) {
                String dbMeasurementsUrl = buildUrl(dbUrl, dbName + MEASUREMENTS_ENDPOINT);
                List<JsonNode> dbMeasurements = fetchMongoEntity(dbMeasurementsUrl, "measurements");
                dbMetrics.putAll(new MongoDbMetricProcessor(hostName, 
                											"database",
                											groupName,
                											metricTemplateFromCfg,
                											getMeasurementsOnlyForCurrentMetricType("database", dbMeasurements),
                											(List) includedMetrics.get("database"), 
                											dbName).populateMetrics());
            }
            else {
                logger.debug("Skipping database : " +dbName+ ". Please add it to the config.yml if you'd like it to be monitored.") ;
            }
        }
        return dbMetrics;
    }

    private Map<String, BigDecimal> getDiskPartitionStats(String hostName, 
    													  String hostId, 
    													  String hostsUrl,
    													  String groupName,
    													  Map includedMetrics) throws IOException {
        logger.info("MongoDB Trace: Fetching disk and partition stats from host : " + hostName);
        Map<String, BigDecimal> diskPartitionMetrics = Maps.newHashMap();
        String disksUrl = buildUrl(hostsUrl, hostId + DISKS_ENDPOINT);
        List<JsonNode> disks = fetchMongoEntity(disksUrl, "results");
        for (JsonNode disk : disks) {
            String partitionName = disk.findValue("partitionName").asText();
            String partitionUrl = buildUrl(disksUrl, partitionName + MEASUREMENTS_ENDPOINT);
            List<JsonNode> diskMeasurements = fetchMongoEntity(partitionUrl, "measurements");
            
            diskPartitionMetrics.putAll(new MongoDbMetricProcessor(hostName, 
            													   "disk",
            													   groupName,
            													   metricTemplateFromCfg,
            													   getMeasurementsOnlyForCurrentMetricType("disk", diskMeasurements),
            													   (List) includedMetrics.get("disks"), 
            													   partitionName).populateMetrics());
        }
        return diskPartitionMetrics;
    }

    private Map<String, BigDecimal> getAlertStats(String alertPathType,
    											  String groupId,
    											  String groupName,
    											  String hostName, 
    											  String clusterName, 
    											  String alertType,
    											  List<JsonNode> alertsFromOpsmanager,
    											  Map includedMetrics) throws IOException {
        logger.info("MongoDB Trace: Fetching alert stats for type: " + alertType + " Group name: " + groupName + " Host name: " + hostName + " Path Type: " + alertPathType);
        Map<String, BigDecimal> alertMetrics = Maps.newHashMap();
        List alertsFromCfg = (List) includedMetrics.get(alertPathType);
        MongoDbAlertProcessor alertProcessor = new MongoDbAlertProcessor(alertPathType, groupId, groupName, hostName, clusterName, alertType, metricTemplateFromCfg, alertsFromOpsmanager, alertsFromCfg);
        alertMetrics = alertProcessor.populateMetrics();
        return alertMetrics;
    }

    private String buildUrl(String serverUrl, String suffix) {
        StringBuilder url = new StringBuilder(serverUrl);
        return url.append(suffix).toString();
    }

    private List<JsonNode> fetchMongoEntity(String url, String entityName) throws IOException {
        logger.info("MongoDB Trace: API call with url: " + url);
        CloseableHttpResponse httpResponse = null;
        List<JsonNode> details = Lists.newArrayList();
        try {
        	JsonNode jsonNode;
        	
    		String nextUrl;
    		int i = 2;
    		
        	if (url.endsWith(ALERTS_ENDPOINT) | url.endsWith(ALERTS_PROJECT_ENDPOINT)) {
        		httpResponse = HttpHelper.doGet(httpClient, url);
    			jsonNode = MongoDBOpsManagerUtils.getJsonNode(httpResponse);
    			int count = jsonNode.get("totalCount").asInt(); 
    			
    			if (jsonNode.size() > 0) {                  	
            		// get the json data requested by the caller
            		for (JsonNode node : jsonNode.get(entityName)) {
            			details.add(node);
            		}
            	} 	
        		
        		while (count > 100) {
        			nextUrl = url + "?pageNum=" + i;
        			i += 1;
        			count -= 100;
            		httpResponse = HttpHelper.doGet(httpClient, nextUrl);
        			jsonNode = MongoDBOpsManagerUtils.getJsonNode(httpResponse);
            		
        			if (jsonNode.size() > 0) {                  	
                		// get the json data requested by the caller
                		for (JsonNode node : jsonNode.get(entityName)) {
                			details.add(node);
                		}
                	}
        		}
        	}
        	else {
        		httpResponse = HttpHelper.doGet(httpClient, url);
            	jsonNode = MongoDBOpsManagerUtils.getJsonNode(httpResponse); 
        		 
            	if (jsonNode.size() > 0) {  
                	
            		// get the json data requested by the caller
            		for (JsonNode node : jsonNode.get(entityName)) {
            			details.add(node);
            		}  
            	}
        	}
        	
        	// Test harness to mimic Ops Manager REST API results
        	// comment out before delivering
        	//details = Lists.newArrayList();
        	//jsonNode = MongoDBOpsManagerUtils.getHttpTestData(url);
        	//for (JsonNode node : jsonNode.get(entityName)) {
        	//	details.add(node);
        	//}
       } catch (Exception ex) {
            logger.error("Error while fetching results from url " + url);
            logger.error("http Response is: " + httpResponse);
            logger.error("Exception is: " + ex);
       } finally {
            HttpHelper.closeHttpResponse(httpResponse);
       }

       logger.info("MongoDB Trace: API called: ");
       return details;
    }

    private HashSet getHostsForAgent(String url) throws IOException {
    	HashSet hsHostNames = new HashSet(); 
        String hostName;
        String lastConf;  
        long tenMinutes = 60*10;
        long staleTime = (System.currentTimeMillis() / 1000) - tenMinutes;
    	
    	// Get the list of agents from Ops MAnager
        List<JsonNode> agents = fetchMongoEntity(url, "results"); 
        
        // Go through the agents looking for any that are not active
        // not active means thay have not had a confcall for over 10 minutes
        for (JsonNode agent : agents) {
        	hostName = agent.findValue("hostname").asText();
        	lastConf = agent.findValue("lastConf").asText();
        	long lastConfSeconds = ZonedDateTime.parse(lastConf).toEpochSecond();
        	logger.debug("stale: " + staleTime + " LastConf: " + lastConfSeconds);
        	if (lastConfSeconds < staleTime) {
            	logger.debug("Host: " + hostName + " stale: " + staleTime + " LastConf: " + lastConfSeconds);
        		hsHostNames.add(hostName);
        	}
        }
        
        return hsHostNames;
    }
}
