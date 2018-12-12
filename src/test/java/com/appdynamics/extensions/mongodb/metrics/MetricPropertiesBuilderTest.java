/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.mongodb.metrics;

import com.appdynamics.extensions.yml.YmlReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.mongodb.helpers.Constants.*;


/**
 * Created by aditya.jagtiani on 8/1/17.
 */
public class MetricPropertiesBuilderTest {
    @Test
    public void buildMetricPropsMapTest_ValidQualifiers() {
        Map<String, ?> config = YmlReader.readFromFile(new File("src/test/resources/conf/config_valid_qualifiers.yml"));
        Map allMetrics = (Map) config.get("metrics");
        List<Map> assertMetricsFromCfg = (List) allMetrics.get("asserts");
        MetricPropertiesBuilder.buildMetricPropsMap(assertMetricsFromCfg.get(0), "ASSERT_REGULAR", "|metricPath|");
        Map<String, MetricProperties> overrides = MetricPropertiesBuilder.getMetricPropsMap();
        Assert.assertTrue(overrides.containsKey("|metricPath|ASSERT_REGULAR"));
        MetricProperties properties = overrides.get("|metricPath|ASSERT_REGULAR");
        Assert.assertTrue(properties.getMetricName().equals("ASSERT_REGULAR"));
        Assert.assertTrue(properties.getAlias().equals("ASSERT_REGULAR"));
        Assert.assertTrue(properties.getAggregationType().equals("AVERAGE"));
        Assert.assertTrue(properties.getClusterRollupType().equals("INDIVIDUAL"));
        Assert.assertTrue(properties.getTimeRollupType().equals("SUM"));
        Assert.assertTrue(properties.getMultiplier().equals("2.5"));
        Assert.assertTrue(properties.getDelta());
    }

    @Test
    public void buildMetricPropsMapTest_InvalidQualifiers() {
        Map<String, ?> config = YmlReader.readFromFile(new File("src/test/resources/conf/config_invalid_qualifiers.yml"));
        Map allMetrics = (Map) config.get("metrics");
        List<Map> assertMetricsFromCfg = (List) allMetrics.get("asserts");
        MetricPropertiesBuilder.buildMetricPropsMap(assertMetricsFromCfg.get(0), "ASSERT_REGULAR", "|metricPath|");
        Map<String, MetricProperties> overrides = MetricPropertiesBuilder.getMetricPropsMap();
        Assert.assertTrue(overrides.containsKey("|metricPath|ASSERT_REGULAR"));
        MetricProperties properties = overrides.get("|metricPath|ASSERT_REGULAR");
        Assert.assertTrue(properties.getMetricName().equals("ASSERT_REGULAR"));
        Assert.assertTrue(properties.getAlias().equals("ASSERT_REGULAR"));
        Assert.assertTrue(properties.getAggregationType().equals(DEFAULT_AGGREGATION_TYPE));
        Assert.assertTrue(properties.getClusterRollupType().equals(DEFAULT_CLUSTER_ROLLUP_TYPE));
        Assert.assertTrue(properties.getTimeRollupType().equals(DEFAULT_TIME_ROLLUP_TYPE));
        Assert.assertTrue(properties.getMultiplier().equals(DEFAULT_MULTIPLIER));
        Assert.assertFalse(properties.getDelta());
    }
}
