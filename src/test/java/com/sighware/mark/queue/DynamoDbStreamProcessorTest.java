package com.sighware.mark.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamoDbStreamProcessorTest {

    @Test
    public void testGetEventID() {
        String eventSnippet = "{\n" +
                "  \"eventId\": \"90a9a11e-95b2-4c0d-aef3-53a15bfbda8f\",\n" +
                "  \"createTime\": \"2019-12-06T15:20:40.152Z\",\n" +
                "  \"eventName\": \"EntitlementCreatedEvent\",";

        DynamoDbStreamProcessor processor = new DynamoDbStreamProcessor();
        assertEquals("90a9a11e-95b2-4c0d-aef3-53a15bfbda8f", processor.getEventId(eventSnippet));
    }

    @Test
    public void testGetEventIDNoSpace() {
        String eventSnippet = "{\"eventId\":\"90a9a11e-95b2-4c0d-aef3-53a15bfbda8f\"," +
                "  \"createTime\": \"2019-12-06T15:20:40.152Z\"," +
                "  \"eventName\": \"EntitlementCreatedEvent\",";

        DynamoDbStreamProcessor processor = new DynamoDbStreamProcessor();
        assertEquals("90a9a11e-95b2-4c0d-aef3-53a15bfbda8f", processor.getEventId(eventSnippet));
    }
}