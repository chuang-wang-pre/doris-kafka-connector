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

package org.apache.doris.kafka.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RespContent {

    @JsonProperty(value = "TxnId")
    private long txnId;

    @JsonProperty(value = "Label")
    private String label;

    @JsonProperty(value = "Status")
    private String status;

    @JsonProperty(value = "TwoPhaseCommit")
    private String twoPhaseCommit;

    @JsonProperty(value = "ExistingJobStatus")
    private String existingJobStatus;

    @JsonProperty(value = "Message")
    private String message;

    @JsonProperty(value = "NumberTotalRows")
    private long numberTotalRows;

    @JsonProperty(value = "NumberLoadedRows")
    private long numberLoadedRows;

    @JsonProperty(value = "NumberFilteredRows")
    private int numberFilteredRows;

    @JsonProperty(value = "NumberUnselectedRows")
    private int numberUnselectedRows;

    @JsonProperty(value = "LoadBytes")
    private long loadBytes;

    @JsonProperty(value = "LoadTimeMs")
    private int loadTimeMs;

    @JsonProperty(value = "BeginTxnTimeMs")
    private int beginTxnTimeMs;

    @JsonProperty(value = "StreamLoadPutTimeMs")
    private int streamLoadPutTimeMs;

    @JsonProperty(value = "ReadDataTimeMs")
    private int readDataTimeMs;

    @JsonProperty(value = "WriteDataTimeMs")
    private int writeDataTimeMs;

    @JsonProperty(value = "CommitAndPublishTimeMs")
    private int commitAndPublishTimeMs;

    @JsonProperty(value = "ErrorURL")
    private String errorURL;

    public long getTxnId() {
        return txnId;
    }

    public String getStatus() {
        return status;
    }

    public String getTwoPhaseCommit() {
        return twoPhaseCommit;
    }

    public String getMessage() {
        return message;
    }

    public String getLabel() {
        return label;
    }

    public String getExistingJobStatus() {
        return existingJobStatus;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public String getErrorURL() {
        return errorURL;
    }
}
