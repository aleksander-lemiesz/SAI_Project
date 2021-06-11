package graduation.approval.gui;

import com.google.gson.Gson;
import shared.model.MessageReceiverGateway;
import shared.model.MessageSenderGateway;
import shared.model.approval.ApprovalReply;
import shared.model.approval.ApprovalRequest;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public abstract class BrokerApplicationGateway {

    private final MessageReceiverGateway msgReceiverGateway;
    private final MessageSenderGateway msgSenderGateway;

    /**
     * Abstract function that is implemented in the controller. It allows to have access to the request in the controller.
     * @param request is the original approval request.
     */
    public abstract void onApprovalRequestReceived(ApprovalRequest request);

    /**
     * Function that allows to send reply to the request.
     * @param request is the request to which the reply is sent.
     * @param reply is the reply to the request.
     */
    public void sendApprovalReply(ApprovalRequest request, ApprovalReply reply) {
        try {

            // Serialize
            String serialized = serializeApprovalReplyAndRequest(reply, request);

            // Create the message
            TextMessage message = (TextMessage) msgSenderGateway.createTextMessage(serialized);

            // Send the reply message
            msgSenderGateway.send(message);

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor of the BrokerApplicationGateway. It starts the connection.
     * @param queue is the name of the queue to the broker.
     */
    public BrokerApplicationGateway(String queue) {
        // Start the connection
        msgSenderGateway = new MessageSenderGateway("brokerReplyQueue");
        msgReceiverGateway = new MessageReceiverGateway(queue);

        msgReceiverGateway.setListener( new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                try {
                    // get JSON from the message text
                    TextMessage message = (TextMessage) msg;
                    String json = message.getText();

                    //deserialize json
                    ApprovalRequest approvalRequest = deserializeApprovalRequest(json);

                    //call abstr. meth. to pass the approvalRequest
                    onApprovalRequestReceived(approvalRequest);

                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * Deserializes ApprovalRequest.
     * @param body is JSON String object to be deserialized.
     * @return is deserialized object obtained from body.
     */
    public ApprovalRequest deserializeApprovalRequest(String body) {
        return new Gson().fromJson(body, ApprovalRequest.class);
    }

    /**
     * Serializes ApprovalReply into JSON String object.
     * @param reply is the reply to be serialized.
     * @return is JSON String.
     */
    public String serializeApprovalReply(ApprovalReply reply) {
        return new Gson().toJson(reply);
    }

    /**
     * Serializes ApprovalRequest into JSON String object.
     * @param request is the request to be serialized.
     * @return is JSON String.
     */
    public String serializeApprovalRequest(ApprovalRequest request) {
        return new Gson().toJson(request);
    }

    /**
     * It serializes both ApprovalRequest and ApprovalReply and adds & between two Strings.
     * @param reply is the reply to be serialized.
     * @param request is the request to be serialized.
     * @return is String containing two serialized objects separated by &.
     */
    public String serializeApprovalReplyAndRequest(ApprovalReply reply, ApprovalRequest request) {
        return serializeApprovalReply(reply) + " & " + serializeApprovalRequest(request);
    }

    /**
     * Is used to close all of the gateways connections.
     */
    public void stop() {
        msgReceiverGateway.stop();
        msgSenderGateway.stop();
    }

}
