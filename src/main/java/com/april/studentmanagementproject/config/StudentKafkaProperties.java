package com.april.studentmanagementproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public class StudentKafkaProperties {

    private String topicName = "student-events";
    private int partitions = 3;
    private short replicationFactor = 3;
    private String consumerGroupId = "student-management-consumers";
    private String listenerConcurrency = "3";
    private int consumedMessageHistoryLimit = 100;

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public short getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(short replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public String getListenerConcurrency() {
        return listenerConcurrency;
    }

    public void setListenerConcurrency(String listenerConcurrency) {
        this.listenerConcurrency = listenerConcurrency;
    }

    public int getConsumedMessageHistoryLimit() {
        return consumedMessageHistoryLimit;
    }

    public void setConsumedMessageHistoryLimit(int consumedMessageHistoryLimit) {
        this.consumedMessageHistoryLimit = consumedMessageHistoryLimit;
    }
}
