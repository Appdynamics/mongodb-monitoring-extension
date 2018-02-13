/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.mongodb.metrics;

import com.appdynamics.extensions.mongodb.helpers.MongoDBOpsManagerUtils;
import com.appdynamics.extensions.yml.YmlReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


/**
 * Created by aditya.jagtiani on 8/1/17.
 */
public class MongoDbMetricProcessorTest {
    private String hostName;
    private List<JsonNode> metricsFromHost;
    private ObjectMapper mapper;
    private Map allMetrics;

    @Before
    public void setup() throws IOException {
        hostName = "host1";
        mapper = new ObjectMapper();
        metricsFromHost = Lists.newArrayList();
        Map<String, ?> config = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        allMetrics = (Map) config.get("metrics");
    }

    @Test
    public void populateMetricsTest_hostMetrics() throws IOException {
        metricsFromHost.add(mapper.readValue(new File("src/test/resources/hostMeasurements.json"), JsonNode.class));
        List<JsonNode> dataPoints = getMeasurements(metricsFromHost);

        Map hostMetrics = (Map) allMetrics.get("hosts");
        List<Map> assertMetricsFromCfg = (List) hostMetrics.get("asserts");
        MongoDbMetricProcessor mongoDbMetricProcessor = new MongoDbMetricProcessor(hostName, "assert",
                MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("assert", dataPoints), assertMetricsFromCfg, "");
        Map<String, BigDecimal> results = mongoDbMetricProcessor.populateMetrics();
        Assert.assertTrue(results.size() == 4);
        Assert.assertTrue(results.containsKey("Hosts|host1|assert|ASSERT_MSG"));
        Assert.assertTrue(results.containsKey("Hosts|host1|assert|ASSERT_REGULAR"));
        Assert.assertTrue(results.containsKey("Hosts|host1|assert|ASSERT_USER"));
        Assert.assertTrue(results.containsKey("Hosts|host1|assert|ASSERT_WARNING"));
        Assert.assertTrue(results.get("Hosts|host1|assert|ASSERT_MSG").equals(new BigDecimal(1002)));
        Assert.assertTrue(results.get("Hosts|host1|assert|ASSERT_REGULAR").equals(new BigDecimal(1000)));
        Assert.assertTrue(results.get("Hosts|host1|assert|ASSERT_USER").equals(new BigDecimal(1003)));
        Assert.assertTrue(results.get("Hosts|host1|assert|ASSERT_WARNING").equals(new BigDecimal(1001)));
    }

    @Test
    public void populateMetricsTest_dbMetrics() throws IOException {
        metricsFromHost.add(mapper.readValue(new File("src/test/resources/dbMeasurements.json"), JsonNode.class));
        List<JsonNode> dataPoints = getMeasurements(metricsFromHost);
        List<Map> dbMetricsFromCfg = (List) allMetrics.get("database");
        MongoDbMetricProcessor mongoDbMetricProcessor = new MongoDbMetricProcessor(hostName, "database",
                MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("database", dataPoints), dbMetricsFromCfg, "local");
        Map<String, BigDecimal> dbMetrics = mongoDbMetricProcessor.populateMetrics();
        Assert.assertTrue(dbMetrics.size() == 6);
        Assert.assertTrue(dbMetrics.containsKey("Hosts|host1|database|local|DATABASE_STORAGE_SIZE"));
        Assert.assertTrue(dbMetrics.containsKey("Hosts|host1|database|local|DATABASE_INDEX_COUNT"));
        Assert.assertTrue(dbMetrics.containsKey("Hosts|host1|database|local|DATABASE_DATA_SIZE"));
        Assert.assertTrue(dbMetrics.containsKey("Hosts|host1|database|local|DATABASE_INDEX_SIZE"));
        Assert.assertTrue(dbMetrics.containsKey("Hosts|host1|database|local|DATABASE_COLLECTION_COUNT"));
        Assert.assertTrue(dbMetrics.containsKey("Hosts|host1|database|local|DATABASE_AVERAGE_OBJECT_SIZE"));

        Assert.assertTrue(dbMetrics.get("Hosts|host1|database|local|DATABASE_STORAGE_SIZE").equals(new BigDecimal(32768)));
        Assert.assertTrue(dbMetrics.get("Hosts|host1|database|local|DATABASE_INDEX_COUNT").equals(new BigDecimal(2)));
        Assert.assertTrue(dbMetrics.get("Hosts|host1|database|local|DATABASE_DATA_SIZE").equals(new BigDecimal(1694)));
        Assert.assertTrue(dbMetrics.get("Hosts|host1|database|local|DATABASE_INDEX_SIZE").equals(new BigDecimal(32768)));
        Assert.assertTrue(dbMetrics.get("Hosts|host1|database|local|DATABASE_COLLECTION_COUNT").equals(new BigDecimal(2)));
        Assert.assertTrue(dbMetrics.get("Hosts|host1|database|local|DATABASE_AVERAGE_OBJECT_SIZE").equals(new BigDecimal(847)));
    }

    @Test
    public void populateMetricsTest_diskMetrics() throws IOException {
        metricsFromHost.add(mapper.readValue(new File("src/test/resources/diskMeasurements.json"), JsonNode.class));
        List<JsonNode> dataPoints = getMeasurements(metricsFromHost);
        List<Map> diskMetricsFromCfg = (List) allMetrics.get("disks");
        MongoDbMetricProcessor mongoDbMetricProcessor = new MongoDbMetricProcessor(hostName, "disk",
                MongoDBOpsManagerUtils.getMeasurementsOnlyForCurrentMetricType("disk", dataPoints), diskMetricsFromCfg, "disk1");
        Map<String, BigDecimal> diskMetrics = mongoDbMetricProcessor.populateMetrics();
        Assert.assertTrue(diskMetrics.size() == 4);
        Assert.assertFalse(diskMetrics.containsKey("Hosts|host1|disk|disk1|DISK_PARTITION_IOPS_READ"));

        Assert.assertTrue(diskMetrics.containsKey("Hosts|host1|disk|disk1|DISK_PARTITION_LATENCY_WRITE"));
        Assert.assertTrue(diskMetrics.containsKey("Hosts|host1|disk|disk1|DISK_PARTITION_SPACE_PERCENT_USED"));
        Assert.assertTrue(diskMetrics.containsKey("Hosts|host1|disk|disk1|DISK_PARTITION_LATENCY_READ"));
        Assert.assertTrue(diskMetrics.containsKey("Hosts|host1|disk|disk1|DISK_PARTITION_IOPS_WRITE"));

        Assert.assertTrue(diskMetrics.get("Hosts|host1|disk|disk1|DISK_PARTITION_LATENCY_WRITE").equals(new BigDecimal(501)));
        Assert.assertTrue(diskMetrics.get("Hosts|host1|disk|disk1|DISK_PARTITION_SPACE_PERCENT_USED").equals(new BigDecimal(58)));
        Assert.assertTrue(diskMetrics.get("Hosts|host1|disk|disk1|DISK_PARTITION_LATENCY_READ").equals(new BigDecimal(501)));
        Assert.assertTrue(diskMetrics.get("Hosts|host1|disk|disk1|DISK_PARTITION_IOPS_WRITE").equals(new BigDecimal(501)));
    }

    private List<JsonNode> getMeasurements(List<JsonNode> metricsFromHost) {
        List<JsonNode> dataPoints = Lists.newArrayList();
        for (JsonNode node : metricsFromHost.get(0).get("measurements")) {
            dataPoints.add(node);
        }
        return dataPoints;
    }
}
