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
 * Created by mark.walmsley on 4/12/2019.
 */

class MongoDbAlertProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbAlertProcessor.class);
    private List<Map> alertsFromConfig;
    private List<JsonNode> alertsFromOpsmanager;
    private String alertPathType;
    private String groupName;
    private String hostName;
    private String clusterName;
    private String alertProcessType;
    private String groupId;
    private String metricTemplate;

    MongoDbAlertProcessor(String alertPathType,
    					  String groupId,
    					  String groupName,
    					  String hostName,
    					  String clusterName,
    					  String alertProcessType,
    					  String metricTemplate,
    					  List<JsonNode> alertsFromOpsmanager,
                          List<Map> alertsFromConfig) {
    	this.alertPathType = alertPathType;
    	this.groupId = groupId;
    	this.groupName = groupName;
    	this.hostName = hostName;
    	this.clusterName = clusterName;
    	this.alertProcessType = alertProcessType;
    	this.metricTemplate = metricTemplate;
        this.alertsFromOpsmanager = alertsFromOpsmanager;
        this.alertsFromConfig = alertsFromConfig;
    }

    Map<String, BigDecimal> populateMetrics() throws IOException {
        if (!MongoDBOpsManagerUtils.isValidationSuccessful(alertsFromConfig, alertsFromOpsmanager, "alerts")) {
            return Maps.newHashMap();
        }
        Map<String, BigDecimal> mongoDbAlerts = Maps.newHashMap();
        
        String alertNameFromOpsmanager;
        String alertStatus;
        String alertGroupId;
        String alertHost = "";
        String alertCluster = "";
        String alertNameFromCfg;
        String alertMetricFromCfg = "";
        String alertMetricFromOpsmanager;
        boolean isMetricAlert;
        BigDecimal alertValue;
        BigDecimal alertActive = new BigDecimal(1);
        BigDecimal alertInactive = new BigDecimal(0);
    	for (Map alert : alertsFromConfig) {
    		alertValue = alertInactive;
           	Map.Entry<String, String> entry = (Map.Entry) alert.entrySet().iterator().next();
           	alertNameFromCfg = entry.getKey();
           	
            Map<String, String> propsFromCfg = (Map) alert.get(alertNameFromCfg);
            String alertTypeFromCfg = propsFromCfg.get("type");
            boolean hasHost = true; 
            boolean useHost = true; 
            if (alertProcessType.equals("ClusterOnly")) {
            	if  (!alertTypeFromCfg.equals("BACKUP") && !alertTypeFromCfg.equals("CLUSTER")) {
            		continue;
            	}
            	hasHost = false;
            }
            else if (alertProcessType.contains("monitoringAgent")) { 
            	// only process monitoring agent alerts
            	if (!alertTypeFromCfg.equals("MONITORING_AGENT")) {
            		continue;
            	}
            	useHost = false;
            }
            else if (alertProcessType.contains("backupAgent")) {
            	// only process backup agent alerts
            	if (!alertTypeFromCfg.equals("BACKUP_AGENT")) {
            		continue;
            	}
            	useHost = false;
            }
            else if (alertProcessType.contains("automationAgent")) {
            	// only process automation agent alerts
            	if (!alertTypeFromCfg.equals("AUTOMATION_AGENT")) {
            		continue;
            	}
            	useHost = false;
            }
            // Need to add code to process back up agent too many conf call failures
            else if (alertProcessType.equals("Agent")) {
            	// only process the Agent type alerts
            	if (!alertTypeFromCfg.equals("AGENT")) {
            		continue;
            	}
            	hasHost = false;
            }
            // process the alerts that are of type HOST	
            else if (alertProcessType.equals("Host")) {
            	// only process Host type alerts and Replica set
            	if (!alertTypeFromCfg.equals("HOST") && !alertTypeFromCfg.equals("REPLICA_SET")) {
            		continue;
            	}
            }
            
           	isMetricAlert = false;
           	if (alertNameFromCfg.contains("@")) {
           		alertMetricFromCfg = alertNameFromCfg.split("@", 2)[1];
           		alertNameFromCfg = alertNameFromCfg.split("@", 2)[0];
           		isMetricAlert = true;
           	}
           	

           	for (JsonNode node : alertsFromOpsmanager) {
           		alertStatus = MongoDBOpsManagerUtils.getJsonValue(node,"status");
           		alertGroupId = MongoDBOpsManagerUtils.getJsonValue(node,"groupId");
           		alertHost = MongoDBOpsManagerUtils.getJsonValue(node,"hostnameAndPort");
           		alertHost = alertHost.split(":")[0];
           		alertNameFromOpsmanager = MongoDBOpsManagerUtils.getJsonValue(node,"eventTypeName");
           		alertCluster = MongoDBOpsManagerUtils.getJsonValue(node,"clusterName");
               	if (alertNameFromOpsmanager.equals(alertNameFromCfg)) {
               		if (alertStatus.equals("OPEN") && alertGroupId.equals(groupId)) {            			
             			if ((alertHost.equals(hostName) && hasHost) || !hasHost || !useHost) { 
             				if (isMetricAlert) {
             					alertMetricFromOpsmanager = node.findValue("metricName").asText();
               					if (alertMetricFromOpsmanager.equals(alertMetricFromCfg)) {
               						alertValue = alertActive;
               						break;
               					}
               				}
               				else {
               					// if an agent alert and we determined this host was not down then keep alert as inactive
               					if (!alertProcessType.endsWith("Down") && !alertProcessType.equals("Agent")) {
               						break;
               					}
               					// for cluster only we need to match on cluster
               					if (alertProcessType.equals("ClusterOnly") && !alertCluster.equals(clusterName)) {
               						break;
               					}
           						alertValue = alertActive;
               				}
               			}
               		}
             	}
    		}
           			
           	if (isMetricAlert) {
               	alertNameFromCfg += "@" + alertMetricFromCfg;
            }
             
           	String alertPath;
           	if (hasHost) {
           		alertPath = MongoDBOpsManagerUtils.processTemplate(metricTemplate, 
               														  hostName, 
               														  groupName, 
               														  clusterName, 
               														  "");
           	}
           	else {
           		alertPath = MongoDBOpsManagerUtils.processTemplate(metricTemplate, 
               														  "", 
               														  groupName, 
               														  clusterName, 
               														  "");
           	}
           	
            alertPath += METRIC_SEPARATOR;
            if (alertPathType.equals("projectalerts")) {
               	alertPath += "projectalerts";
            }
            else {
               	alertPath += "alerts";
            }
                    
            mongoDbAlerts.put(alertPath + METRIC_SEPARATOR + alertNameFromCfg, alertValue);
           	MetricPropertiesBuilder.buildMetricPropsMap(alert, alertNameFromCfg, alertPath);
        }
        logger.debug("mongoDBAlerts: " + mongoDbAlerts);
        return mongoDbAlerts;
    }
}
