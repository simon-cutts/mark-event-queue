package com.sighware.mark.queue;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageSender {
    public static final String MARK_EVENT = "mark-event";
    public static final String EVENT_ID = "eventId";

    private MessageSender() {
    }

    public static void send(AmazonSQS sqs, UUID eventId, String json, String url) {
        Map<String, MessageAttributeValue> clientAttributes = new HashMap<>();
        clientAttributes.put(EVENT_ID, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(eventId.toString()));

        // Now write event to SQS
        SendMessageRequest clientMsg = new SendMessageRequest()
                .withQueueUrl(url)
                .withMessageGroupId(MARK_EVENT)
                .withMessageDeduplicationId(eventId.toString())
                .withMessageAttributes(clientAttributes)
                .withMessageBody(json);
        sqs.sendMessage(clientMsg);
    }
}
