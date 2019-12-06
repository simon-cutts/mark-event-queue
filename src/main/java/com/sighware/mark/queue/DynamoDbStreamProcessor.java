package com.sighware.mark.queue;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sighware.mark.server.event.RegistrationNumberEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processes new image events from a DynamoDB stream on the RegistrationNumberEvent table.
 * Reads from the DynamoDB stream, converts data to a json RegistrationNumberEvent and then
 * writes the json to the SQS FanOut
 */
public class DynamoDbStreamProcessor implements
        RequestHandler<DynamodbEvent, String> {

    private static final String FAN_OUT_SQS_QUEUE_URL = System.getenv("FAN_OUT_SQS_QUEUE_URL");
    private final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String handleRequest(DynamodbEvent ddbEvent, Context context) {

        for (DynamodbStreamRecord record : ddbEvent.getRecords()) {

            // Only process a new image
            if (record.getDynamodb() != null) {
                Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
                if (newImage != null) {

                    // Load the stream data to be converted into json
                    List<Map<String, AttributeValue>> listOfMaps = new ArrayList<>();
                    listOfMaps.add(newImage);
                    List<Item> itemList = ItemUtils.toItemList(listOfMaps);

                    for (Item item : itemList) {
                        String json = item.toJSON();

                        System.out.println(json);

                        // Now write event to SQS
                        try {
                            RegistrationNumberEvent event = objectMapper.readValue(json, RegistrationNumberEvent.class);

                            SendMessageRequest msg = new SendMessageRequest()
                                    .withQueueUrl(FAN_OUT_SQS_QUEUE_URL)
                                    .withMessageGroupId("mark-event")
                                    .withMessageDeduplicationId(event.getEventId())
                                    .withMessageBody(json);
                            sqs.sendMessage(msg);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return "Ok";
    }
}