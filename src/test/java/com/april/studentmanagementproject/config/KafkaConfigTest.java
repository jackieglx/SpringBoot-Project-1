package com.april.studentmanagementproject.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaConfigTest {

    @Test
    void createsStudentEventsTopicWithThreePartitionsAndThreeReplicas() {
        StudentKafkaProperties properties = new StudentKafkaProperties();
        properties.setTopicName("student-events");
        properties.setPartitions(3);
        properties.setReplicationFactor((short) 3);

        var topic = new KafkaConfig().studentEventsTopic(properties);

        assertEquals("student-events", topic.name());
        assertEquals(3, topic.numPartitions());
        assertEquals(3, topic.replicationFactor());
    }
}
