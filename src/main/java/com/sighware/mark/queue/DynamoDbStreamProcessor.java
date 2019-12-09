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
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes new image events from a DynamoDB stream on the RegistrationNumberEvent table.
 * Reads from the DynamoDB stream, converts data to a json RegistrationNumberEvent and then
 * writes the json to the SQS FanOut
 */
public class DynamoDbStreamProcessor implements
        RequestHandler<DynamodbEvent, String> {

    public static final String EVENT_ID = "eventId";
    private static final String FAN_OUT_SQS_QUEUE_URL = System.getenv("FAN_OUT_SQS_QUEUE_URL");
    private static final String MARK_EVENT = "mark-event";
    private final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

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

                        // Pass the eventId as the unique identifier
                        String eventId = getEventId(json);

                        Map<String, MessageAttributeValue> attributes = new HashMap<>();
                        attributes.put(EVENT_ID, new MessageAttributeValue()
                                .withDataType("String")
                                .withStringValue(eventId));

                        // Now write event to SQS
                        SendMessageRequest msg = new SendMessageRequest()
                                .withQueueUrl(FAN_OUT_SQS_QUEUE_URL)
                                .withMessageGroupId(MARK_EVENT)
                                .withMessageDeduplicationId(eventId)
                                .withMessageAttributes(attributes)
                                .withMessageBody(json);
                        sqs.sendMessage(msg);
                    }
                }
            }
        }
        return "Ok";
    }

    /**
     * Get the eventId GGUID from the json, for example: {"eventId":"90a9a11e-95b2-4c0d-aef3-53a15bfbda8f","createTime:" ..."
     *
     * @param json
     * @return
     */
    String getEventId(String json) {
        json = json.replace(" ", "").replace("\n", "");
        int s = json.indexOf("eventId\":\"") + 10;
        String trim = json.substring(s);
        s = trim.indexOf(",") - 1;
        return trim.substring(0, s);
    }
}