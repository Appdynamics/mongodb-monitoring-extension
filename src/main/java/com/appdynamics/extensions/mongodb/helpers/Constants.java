/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.mongodb.helpers;

import java.util.Arrays;
import java.util.List;

/**
 * Created by aditya.jagtiani on 7/12/17.
 * Updated by Mark.Walmsley on 6/29/18.
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
    public static final String METRIC_TEMPLATE_SEPARATOR = "_";
    public static final String GROUPS_ENDPOINT = "/groups/";
    public static final String HOSTS_ENDPOINT = "/hosts/";
    public static final String DB_ENDPOINT = "/databases/";
    public static final String DISKS_ENDPOINT = "/disks/";
    public static final String MEASUREMENTS_ENDPOINT = "/measurements?granularity=PT1H&period=PT1H";
    public static final String ALERTS_ENDPOINT = "/globalAlerts";
    public static final String ALERTS_PROJECT_ENDPOINT = "/alerts";
    public static final String CLUSTERS_ENDPOINT = "/clusters";
    public static final String AGENTS_MONITORING_ENDPOINT = "/agents/MONITORING";
    public static final String AGENTS_BACKUP_ENDPOINT = "/agents/BACKUP";
    public static final String AGENTS_AUTOMATION_ENDPOINT = "/agents/AUTOMATION";
}
