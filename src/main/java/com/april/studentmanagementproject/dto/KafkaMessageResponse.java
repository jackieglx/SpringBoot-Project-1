package com.april.studentmanagementproject.dto;

public class KafkaMessageResponse {

    private final String topic;
    private final Integer partition;
    private final Long offset;
    private final String key;
    private final String value;

    public KafkaMessageResponse(String topic, Integer partition, Long offset, String key, String value) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.value = value;
    }

    public String getTopic() {
        return topic;
    }

    public Integer getPartition() {
        return partition;
    }

    public Long getOffset() {
        return offset;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
