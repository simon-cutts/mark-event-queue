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

    private static final Map<String, String> environment = System.getenv();
    private static final String FAN_OUT_SQS_QUEUE_URL = System.getenv("FAN_OUT_SQS_QUEUE_URL");

    private final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
    private final ArrayList<String> clientQueueUrl = new ArrayList<>();

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

        buildSqsDestinations();

        for (String url : clientQueueUrl) {
            MessageSender.send(sqs, UUID.fromString(eventId), json, url);
        }
    }

    /**
     * Build the list of fanout SQS client queues
     */
    private void buildSqsDestinations() {
        if (clientQueueUrl.size() != 0) {
            return;
        }
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            if (entry.getKey().startsWith("FAN_CLIENT_OUT_SQS")) {
                clientQueueUrl.add(entry.getValue());
            }
        }
    }
}