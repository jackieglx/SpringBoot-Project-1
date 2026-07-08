package com.april.studentmanagementproject.dto;

import java.time.Instant;

public class ConsumedKafkaMessage {

    private final String topic;
    private final int partition;
    private final long offset;
    private final String key;
    private final String value;
    private final String consumerThread;
    private final Instant consumedAt;

    public ConsumedKafkaMessage(
            String topic,
            int partition,
            long offset,
            String key,
            String value,
            String consumerThread,
            Instant consumedAt) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.value = value;
        this.consumerThread = consumerThread;
        this.consumedAt = consumedAt;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getConsumerThread() {
        return consumerThread;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }
}
