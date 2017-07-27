package com.appdynamics.extensions.mongodb.helpers;

import java.util.Arrays;
import java.util.List;

/**
 * Created by aditya.jagtiani on 7/12/17.
 */
public class Constants {
    public static final List<String> VALID_CLUSTER_ROLLUP_TYPES = Arrays.asList("INDIVIDUAL", "COLLECTIVE");
    public static final List<String> VALID_AGGREGATION_TYPES = Arrays.asList("AVERAGE", "SUM", "OBSERVATION");
    public static final List<String> VALID_TIME_ROLLUP_TYPES = Arrays.asList("AVERAGE", "SUM", "CURRENT");
    public static final String DEFAULT_CLUSTER_ROLLUP_TYPE = "INDIVIDUAL";
    public static final String DEFAULT_TIME_ROLLUP_TYPE = "AVERAGE";
    public static final String DEFAULT_AGGREGATION_TYPE = "AVERAGE";
    public static final String DEFAULT_MULTIPLIER = "1.0";
    public static final String METRIC_SEPARATOR = "|";
    public static final String GROUPS_ENDPOINT = "/groups/";
    public static final String HOSTS_ENDPOINT = "/hosts/";
    public static final String DB_ENDPOINT = "/databases/";
    public static final String MEASUREMENTS_ENDPOINT = "/measurements?granularity=PT1M&period=PT1M";
}
