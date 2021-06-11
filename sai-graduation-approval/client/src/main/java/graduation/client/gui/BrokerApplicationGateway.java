package graduation.client.gui;

import com.google.gson.Gson;
import shared.gui.ListViewLine;
import shared.model.MessageReceiverGateway;
import shared.model.MessageSenderGateway;
import shared.model.client.GraduationReply;
import shared.model.client.GraduationRequest;

import javax.jms.*;

public abstract class BrokerApplicationGateway {

    private MessageSenderGateway msgSenderGateway = null;
    private MessageReceiverGateway msgReceiverGateway = null;

    // Storing the requests
    private final String replyQueue = "ClientReplyQueue";

    /**
     * Constructor of BrokerApplicationGateway. It starts the connection with ActiveMQ.
     */
    public BrokerApplicationGateway() {
        // start connection by creating new gateways objects
        msgSenderGateway = new MessageSenderGateway("brokerRequestQueue");

        msgReceiverGateway = new MessageReceiverGateway(replyQueue);
        msgReceiverGateway.setListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    // Create new text message to get access to the text of the message
                    TextMessage textMessage = (TextMessage) message;

                    // Deserialize
                    String split[] = textMessage.getText().split(" & ");
                    GraduationReply reply = deserializeGraduationReply(split[0]);
                    GraduationRequest request = deserializeGraduationRequest(split[1]);

                    // Assign the reply to the request
                    onGraduationReplyReceived(request, reply);

                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * Used to apply for graduation. It sends the serialized request to the broker.
     * @param request is the request to graduate.
     * @throws Exception is thrown if there is problem with the reply destination.
     */
    public void applyForGraduation(GraduationRequest request) throws Exception {

        // Create JSON
        var json = serializeGraduationRequest(request);

        // Create the message from JSON
        Message message = msgSenderGateway.createTextMessage(json);

        // Set the receiver destination
        Destination replyDest = msgReceiverGateway.getDestination();
        message.setJMSReplyTo(replyDest);

        // send message
        msgSenderGateway.send(message);

    }

    /**
     * Abstract function that is implemented in the controller. It allows to have access to the reply and request in the controller.
     * @param request is the original graduation request.
     * @param reply is the official reply to the request.
     */
    public abstract void onGraduationReplyReceived(GraduationRequest request, GraduationReply reply);

    /**
     * Serializes GraduationRequest into JSON String object.
     * @param request is the request to be serialized.
     * @return is JSON String.
     */
    public String serializeGraduationRequest(GraduationRequest request) {
        return new Gson().toJson(request);
    }

    /**
     * Deserializes GraduationReply.
     * @param body is JSON String object to be deserialized.
     * @return is deserialized object obtained from body.
     */
    public GraduationReply deserializeGraduationReply(String body) {
        return new Gson().fromJson(body, GraduationReply.class);
    }

    /**
     * Deserializes GraduationRequest.
     * @param body is JSON String object to be deserialized.
     * @return is deserialized object obtained from body.
     */
    public GraduationRequest deserializeGraduationRequest(String body) {
        return new Gson().fromJson(body, GraduationRequest.class);
    }

    /**
     * Is used to close all of the gateways connections.
     */
    public void stop() {
        msgSenderGateway.stop();
        msgReceiverGateway.stop();
    }

}
