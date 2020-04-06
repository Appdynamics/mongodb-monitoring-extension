/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.mongodb;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.mongodb.metrics.MetricPropertiesBuilder;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Maps;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Created by aditya.jagtiani on 8/1/17.
 */
public class MongoDBOpsManagerStatsTest {
    private Map server;
    private MonitorConfiguration configuration = mock(MonitorConfiguration.class);
    private MetricWriteHelper metricWriter = mock(MetricWriteHelper.class);
    private Map<String, BigDecimal> mongoDBMetrics = Maps.newHashMap();
    private MongoDBOpsManagerStats mongoDBOpsManagerStats;

    @Before
    public void setup() {
        buildMetricPropsMapForTest();
        when(configuration.getMetricPrefix()).thenReturn("metricPrefix");
        when(configuration.getMetricWriter()).thenReturn(metricWriter);
        mongoDBOpsManagerStats = new MongoDBOpsManagerStats(configuration, server);
    }

    @Test
    public void printMetricsTest_whenDeltaIsTrue() {
        buildMetricMapForDelta(new BigDecimal(5));
        mongoDBOpsManagerStats.printMetrics(mongoDBMetrics);
        verify(metricWriter, never()).printMetric(anyString(), anyString(), anyString(), anyString(), anyString());
        buildMetricMapForDelta(new BigDecimal(10));
        mongoDBOpsManagerStats.printMetrics(mongoDBMetrics);
        verify(metricWriter, times(1)).printMetric(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    private void buildMetricPropsMapForTest() {
        Map<String, ?> config = YmlReader.readFromFile(new File("src/test/resources/conf/config_delta.yml"));
        Map allMetrics = (Map) config.get("metrics");
        List<Map> servers = (List) config.get("servers");
        server = servers.get(0);
        List<Map> jobMetricsFromCfg = (List) allMetrics.get("asserts");
        MetricPropertiesBuilder.buildMetricPropsMap(jobMetricsFromCfg.get(0), "ASSERT_REGULAR", "metricPrefix|");
    }

    private void buildMetricMapForDelta(BigDecimal value) {
        mongoDBMetrics.put("metricPrefix|ASSERT_REGULAR", value);
    }


}
