/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.doris.kafka.connector.utils;

import static org.apache.doris.kafka.connector.writer.LoadConstants.PARTIAL_COLUMNS;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.doris.kafka.connector.cfg.DorisOptions;
import org.apache.doris.kafka.connector.cfg.DorisSinkConnectorConfig;
import org.apache.doris.kafka.connector.converter.ConverterMode;
import org.apache.doris.kafka.connector.converter.schema.SchemaEvolutionMode;
import org.apache.doris.kafka.connector.exception.ArgumentsException;
import org.apache.doris.kafka.connector.exception.DorisException;
import org.apache.doris.kafka.connector.model.BehaviorOnNullValues;
import org.apache.doris.kafka.connector.writer.DeliveryGuarantee;
import org.apache.doris.kafka.connector.writer.LoadConstants;
import org.apache.doris.kafka.connector.writer.load.GroupCommitMode;
import org.apache.doris.kafka.connector.writer.load.LoadModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigCheckUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigCheckUtils.class);

    // task id
    public static final String TASK_ID = "task_id";

    /**
     * Validate input configuration
     *
     * @param config configuration Map
     * @return connector name
     */
    public static String validateConfig(Map<String, String> config) {
        boolean configIsValid = true; // verify all config

        // unique name of this connector instance
        String connectorName = config.getOrDefault(DorisSinkConnectorConfig.NAME, "");
        if (connectorName.isEmpty() || !isValidDorisApplicationName(connectorName)) {
            LOG.error(
                    "{} is empty or invalid. It should match doris object identifier syntax. Please see "
                            + "the documentation.",
                    DorisSinkConnectorConfig.NAME);
            configIsValid = false;
        }

        String topics = config.getOrDefault(DorisSinkConnectorConfig.TOPICS, "");
        String topicsRegex = config.getOrDefault(DorisSinkConnectorConfig.TOPICS_REGEX, "");
        if (topics.isEmpty() && topicsRegex.isEmpty()) {
            LOG.error(
                    "{} or {} cannot be empty.",
                    DorisSinkConnectorConfig.TOPICS,
                    DorisSinkConnectorConfig.TOPICS_REGEX);
            configIsValid = false;
        }

        if (!topics.isEmpty() && !topicsRegex.isEmpty()) {
            LOG.error(
                    "{} and {} cannot be set at the same time.",
                    DorisSinkConnectorConfig.TOPICS,
                    DorisSinkConnectorConfig.TOPICS_REGEX);
            configIsValid = false;
        }

        if (config.containsKey(DorisSinkConnectorConfig.TOPICS_TABLES_MAP)
                && parseTopicToTableMap(config.get(DorisSinkConnectorConfig.TOPICS_TABLES_MAP))
                        == null) {
            LOG.error("{} is empty or invalid.", DorisSinkConnectorConfig.TOPICS_TABLES_MAP);
            configIsValid = false;
        }

        String dorisUrls = config.getOrDefault(DorisSinkConnectorConfig.DORIS_URLS, "");
        if (dorisUrls.isEmpty()) {
            LOG.error("{} cannot be empty.", DorisSinkConnectorConfig.DORIS_URLS);
            configIsValid = false;
        }

        String queryPort = config.getOrDefault(DorisSinkConnectorConfig.DORIS_QUERY_PORT, "");
        if (queryPort.isEmpty()) {
            LOG.error("{} cannot be empty.", DorisSinkConnectorConfig.DORIS_QUERY_PORT);
            configIsValid = false;
        }

        String httpPort = config.getOrDefault(DorisSinkConnectorConfig.DORIS_HTTP_PORT, "");
        if (httpPort.isEmpty()) {
            LOG.error("{} cannot be empty.", DorisSinkConnectorConfig.DORIS_HTTP_PORT);
            configIsValid = false;
        }

        String dorisUser = config.getOrDefault(DorisSinkConnectorConfig.DORIS_USER, "");
        if (dorisUser.isEmpty()) {
            LOG.error("{} cannot be empty.", DorisSinkConnectorConfig.DORIS_USER);
            configIsValid = false;
        }

        String autoDirect = config.getOrDefault(DorisSinkConnectorConfig.AUTO_REDIRECT, "");
        if (!autoDirect.isEmpty()
                && !("true".equalsIgnoreCase(autoDirect) || "false".equalsIgnoreCase(autoDirect))) {
            LOG.error("autoDirect non-boolean type, {}", autoDirect);
            configIsValid = false;
        }

        String bufferCountRecords = config.get(DorisSinkConnectorConfig.BUFFER_COUNT_RECORDS);
        if (!isNumeric(bufferCountRecords)) {
            LOG.error(
                    "{} cannot be empty or not a number.",
                    DorisSinkConnectorConfig.BUFFER_COUNT_RECORDS);
            configIsValid = false;
        }

        String bufferSizeBytes = config.get(DorisSinkConnectorConfig.BUFFER_SIZE_BYTES);
        if (!isNumeric(bufferSizeBytes)
                || isIllegalRange(
                        bufferSizeBytes, DorisSinkConnectorConfig.BUFFER_SIZE_BYTES_MIN)) {
            LOG.error(
                    "{} cannot be empty or not a number or less than 1.",
                    DorisSinkConnectorConfig.BUFFER_SIZE_BYTES);
            configIsValid = false;
        }

        String bufferFlushTime = config.get(DorisSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC);
        if (!isNumeric(bufferFlushTime)
                || isIllegalRange(
                        bufferFlushTime, DorisSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC_MIN)) {
            LOG.error(
                    "{} cannot be empty or not a number or less than 1.",
                    DorisSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC);
            configIsValid = false;
        }

        String loadModel = config.get(DorisSinkConnectorConfig.LOAD_MODEL);
        if (!validateEnumInstances(loadModel, LoadModel.instances())) {
            LOG.error(
                    "The value of {} is an illegal parameter of {}.",
                    loadModel,
                    DorisSinkConnectorConfig.LOAD_MODEL);
            configIsValid = false;
        }

        String deliveryGuarantee = config.get(DorisSinkConnectorConfig.DELIVERY_GUARANTEE);
        if (!validateEnumInstances(deliveryGuarantee, DeliveryGuarantee.instances())) {
            LOG.error(
                    "The value of {} is an illegal parameter of {}.",
                    deliveryGuarantee,
                    DorisSinkConnectorConfig.DELIVERY_GUARANTEE);
            configIsValid = false;
        }

        String enableCombineFlush = config.get(DorisSinkConnectorConfig.ENABLE_COMBINE_FLUSH);
        if (!validateEnumInstances(enableCombineFlush, new String[] {"true", "false"})) {
            LOG.error(
                    "The value of {} is an illegal parameter of {}.",
                    enableCombineFlush,
                    DorisSinkConnectorConfig.ENABLE_COMBINE_FLUSH);
            configIsValid = false;
        }

        if (configIsValid
                && Boolean.parseBoolean(enableCombineFlush)
                && DeliveryGuarantee.EXACTLY_ONCE.name().equalsIgnoreCase(deliveryGuarantee)) {
            LOG.error(
                    "The value of {} is not supported set {} when {} is set to {}.",
                    DorisSinkConnectorConfig.ENABLE_COMBINE_FLUSH,
                    enableCombineFlush,
                    DorisSinkConnectorConfig.DELIVERY_GUARANTEE,
                    DeliveryGuarantee.EXACTLY_ONCE.name());
            configIsValid = false;
        }

        String converterMode = config.get(DorisSinkConnectorConfig.CONVERTER_MODE);
        if (!validateEnumInstances(converterMode, ConverterMode.instances())) {
            LOG.error(
                    "The value of {} is an illegal parameter of {}.",
                    loadModel,
                    DorisSinkConnectorConfig.CONVERTER_MODE);
            configIsValid = false;
        }

        String schemaEvolutionMode = config.get(DorisSinkConnectorConfig.DEBEZIUM_SCHEMA_EVOLUTION);
        if (!validateEnumInstances(schemaEvolutionMode, SchemaEvolutionMode.instances())) {
            LOG.error(
                    "The value of {} is an illegal parameter of {}.",
                    loadModel,
                    DorisSinkConnectorConfig.DEBEZIUM_SCHEMA_EVOLUTION);
            configIsValid = false;
        }

        String maxRetries = config.get(DorisSinkConnectorConfig.MAX_RETRIES);
        if (!isNumeric(maxRetries) || isIllegalRange(maxRetries, 0)) {
            LOG.error(
                    "{} cannot be empty or not a number or less than 0.",
                    DorisSinkConnectorConfig.MAX_RETRIES);
            configIsValid = false;
        }

        String retryIntervalMs = config.get(DorisSinkConnectorConfig.RETRY_INTERVAL_MS);
        if (!isNumeric(retryIntervalMs) || isIllegalRange(retryIntervalMs, 0)) {
            LOG.error(
                    "{} cannot be empty or not a number or less than 0.",
                    DorisSinkConnectorConfig.RETRY_INTERVAL_MS);
            configIsValid = false;
        }

        String behaviorOnNullValues = config.get(DorisSinkConnectorConfig.BEHAVIOR_ON_NULL_VALUES);
        if (!validateEnumInstances(behaviorOnNullValues, BehaviorOnNullValues.instances())) {
            LOG.error(
                    "The value of {} is an illegal parameter of {}.",
                    behaviorOnNullValues,
                    DorisSinkConnectorConfig.BEHAVIOR_ON_NULL_VALUES);
            configIsValid = false;
        }

        if (!configIsValid) {
            throw new DorisException(
                    "input kafka connector configuration is null, missing required values, or wrong input value");
        }

        return connectorName;
    }

    /**
     * validates that given name is a valid doris application name, support '-'
     *
     * @param appName doris application name
     * @return true if given application name is valid
     */
    public static boolean isValidDorisApplicationName(String appName) {
        return appName.matches("([a-zA-Z0-9_\\-]+)");
    }

    /**
     * verify topic name, and generate valid table name
     *
     * @param topic input topic name
     * @param topic2table topic to table map
     * @return valid table name
     */
    @Deprecated
    public static String tableName(String topic, Map<String, String> topic2table) {
        return generateValidName(topic, topic2table);
    }

    /**
     * verify topic name, and generate valid table/application name
     *
     * @param topic input topic name
     * @param topic2table topic to table map
     * @return valid table/application name
     */
    @Deprecated
    public static String generateValidName(String topic, Map<String, String> topic2table) {
        if (topic == null || topic.isEmpty()) {
            throw new DorisException("Topic name is empty String or null");
        }
        if (topic2table.containsKey(topic)) {
            return topic2table.get(topic);
        }
        if (isValidTableIdentifier(topic)) {
            return topic;
        }
        // debezium topic default regex name.db.tbl
        if (topic.contains(".")) {
            String[] split = topic.split("\\.");
            return split[split.length - 1];
        }

        throw new ArgumentsException("Failed get table name from topic");
    }

    public static Map<String, String> parseTopicToTableMap(String input) {
        Map<String, String> topic2Table = new HashMap<>();
        boolean isInvalid = false;
        for (String str : input.split(",")) {
            String[] tt = str.split(":");

            if (tt.length != 2 || tt[0].trim().isEmpty() || tt[1].trim().isEmpty()) {
                LOG.error(
                        "Invalid {} config format: {}",
                        DorisSinkConnectorConfig.TOPICS_TABLES_MAP,
                        input);
                return null;
            }

            String topic = tt[0].trim();
            String table = tt[1].trim();

            if (table.isEmpty()) {
                LOG.error("tableName is empty");
                isInvalid = true;
            }

            if (topic2Table.containsKey(topic)) {
                LOG.error("topic name {} is duplicated", topic);
                isInvalid = true;
            }

            topic2Table.put(tt[0].trim(), tt[1].trim());
        }
        if (isInvalid) {
            throw new DorisException("Failed to parse topic2table map");
        }
        return topic2Table;
    }

    private static boolean isNumeric(String str) {
        if (str != null && !str.isEmpty()) {
            Pattern pattern = Pattern.compile("[0-9]*");
            return pattern.matcher(str).matches();
        }
        return false;
    }

    private static boolean isIllegalRange(String flushTime, long minValue) {
        long time = Long.parseLong(flushTime);
        return time < minValue;
    }

    /** validates that table name is a valid table identifier */
    private static boolean isValidTableIdentifier(String tblName) {
        return tblName.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
    }

    private static boolean validateEnumInstances(String value, String[] instances) {
        for (String instance : instances) {
            if (instance.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    public static boolean validateGroupCommitMode(DorisOptions dorisOptions) {
        Properties streamLoadProp = dorisOptions.getStreamLoadProp();
        boolean enable2PC = dorisOptions.enable2PC();
        boolean force2PC = dorisOptions.force2PC();
        if (!streamLoadProp.containsKey(LoadConstants.GROUP_COMMIT)) {
            return false;
        }

        Object value = streamLoadProp.get(LoadConstants.GROUP_COMMIT);
        String normalizedValue = value.toString().trim().toLowerCase();
        if (!GroupCommitMode.instances().contains(normalizedValue)) {
            throw new DorisException(
                    "The value of group commit mode is an illegal parameter, illegal value="
                            + value);
        } else if (enable2PC && force2PC) {
            throw new DorisException(
                    "When group commit is enabled, you should disable two phase commit! Please  set 'enable.2pc':'false'");
        } else if (streamLoadProp.containsKey(PARTIAL_COLUMNS)
                && streamLoadProp.get(PARTIAL_COLUMNS).equals("true")) {
            throw new DorisException(
                    "When group commit is enabled,you can not load data with partial column update.");
        } else if (enable2PC) {
            // The default enable2PC is true, in the scenario of group commit, it needs to be closed
            LOG.info(
                    "The Group Commit mode is on, the two phase commit default value should be disabled.");
            dorisOptions.setEnable2PC(false);
        }
        return true;
    }
}
