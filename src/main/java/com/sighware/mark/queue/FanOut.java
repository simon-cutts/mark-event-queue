package com.sighware.mark.queue;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

public class FanOut implements RequestHandler<SQSEvent, Void> {

    public static final String EVENT_ID = "eventId";
    private static final String FAN_OUT_SQS_QUEUE_URL = System.getenv("FAN_OUT_SQS_QUEUE_URL");
    private static final String FAN_CLIENT_OUT_SQS_QUEUE_URL = System.getenv("FAN_CLIENT_OUT_SQS_QUEUE_URL");
    private static final String MARK_EVENT = "mark-event";
    private final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSMessage msg : event.getRecords()) {

            Map<String, SQSEvent.MessageAttribute> attributes = msg.getMessageAttributes();
            String eventId = attributes.get(EVENT_ID).getStringValue();

            System.out.println("id " + eventId);
            String json = msg.getBody();
            System.out.println(json);

            Map<String, MessageAttributeValue> clientAttributes = new HashMap<>();
            clientAttributes.put(EVENT_ID, new MessageAttributeValue()
                    .withDataType("String")
                    .withStringValue(eventId));

            // Now write event to SQS
            SendMessageRequest clientMsg = new SendMessageRequest()
                    .withQueueUrl(FAN_CLIENT_OUT_SQS_QUEUE_URL)
                    .withMessageGroupId(MARK_EVENT)
                    .withMessageDeduplicationId(eventId)
                    .withMessageAttributes(clientAttributes)
                    .withMessageBody(json);
            sqs.sendMessage(clientMsg);

            sqs.deleteMessage(new DeleteMessageRequest(FAN_OUT_SQS_QUEUE_URL, msg.getReceiptHandle()));
        }
        return null;
    }
}