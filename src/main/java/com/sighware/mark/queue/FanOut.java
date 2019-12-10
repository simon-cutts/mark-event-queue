package com.sighware.mark.queue;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * Responds to the the Fanout SQS queue and fanouts the messages to all client SQS destinations.
 * SQS destinations are loaded dynamically from the environment
 *
 * @author Simon Cutts
 */
public class FanOut implements RequestHandler<SQSEvent, Void> {

    private final static String FAN_OUT_SQS_QUEUE_URL = System.getenv("FAN_OUT_SQS_QUEUE_URL");
    private final static ArrayList<String> CLIENT_QUEUES = new ArrayList<>();

    private final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

    static {
         // Build the list of fanout SQS client queue destinations
        Map<String, String> environment = System.getenv();
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            if (entry.getKey().startsWith("FAN_OUT_CLIENT_SQS")) {
                CLIENT_QUEUES.add(entry.getValue());
            }
        }
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSMessage msg : event.getRecords()) {

            Map<String, SQSEvent.MessageAttribute> attributes = msg.getMessageAttributes();
            String eventId = attributes.get(MessageSender.EVENT_ID).getStringValue();

            String json = msg.getBody();
            System.out.println(json);

            fanout(eventId, json);

            sqs.deleteMessage(new DeleteMessageRequest(FAN_OUT_SQS_QUEUE_URL, msg.getReceiptHandle()));
        }
        return null;
    }

    /**
     * Fanout the message to all client SQS queues
     *
     * @param eventId
     * @param json
     */
    private void fanout(String eventId, String json) {
        for (String url : CLIENT_QUEUES) {
            MessageSender.send(sqs, UUID.fromString(eventId), json, url);
        }
    }
}