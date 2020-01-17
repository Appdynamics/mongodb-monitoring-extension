/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.mongodb;

import com.appdynamics.extensions.conf.MonitorConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

public class MongoDBOpsManagerMonitorTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MongoDBOpsManagerMonitorTask.class);
    private MonitorConfiguration configuration;
    private Map server;

    MongoDBOpsManagerMonitorTask(MonitorConfiguration configuration, Map server) {
        this.configuration = configuration;
        this.server = server;
    }

    public void run() {
        try {
            populateAndPrintStats();
            logger.info("MongoDB Metric Upload Complete");
        } catch (Exception ex) {
            configuration.getMetricWriter().registerError(ex.getMessage(), ex);
            logger.error("Error while running the task", ex);
        }
    }

    private void populateAndPrintStats() {
        try {        
            MongoDBOpsManagerStats mongoDBStats = new MongoDBOpsManagerStats(configuration, server);
            Map<String, BigDecimal> allMetrics = mongoDBStats.populateMetrics();
            mongoDBStats.printMetrics(allMetrics);
            logger.info("Successfully completed the MongoDB Monitoring Task for server:  " + server.get("name").toString());
        } catch (Exception ex) {
            logger.error("MongoDB Monitoring Task Failed for server: " +server.get("name").toString(), ex.getMessage());
            logger.error("Error while Populating the metrics", ex);
        }
    }
}
