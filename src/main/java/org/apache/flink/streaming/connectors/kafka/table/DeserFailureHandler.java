package org.apache.flink.streaming.connectors.kafka.table;

public enum DeserFailureHandler {
    /**
     * No deserialization failure handling is applied.
     * In case of a problematic record, the application will fail.
     * This is the default setting.
     */
    NONE,
    LOG,
    KAFKA
}
