package broker.gui;

import com.google.gson.Gson;
import shared.model.MessageReceiverGateway;
import shared.model.MessageSenderGateway;
import shared.model.client.GraduationReply;
import shared.model.client.GraduationRequest;

import javax.jms.*;
import java.util.HashMap;

public abstract class ClientApplicationGateway {

    private MessageSenderGateway toClientGateway = null;
    private MessageReceiverGateway fromClientGateway = null;

    // Storing the requests
    private HashMap<GraduationRequest, Destination> requests = new HashMap<>();

    /**
     * Constructor of ClientApplicationGateway. It creates the instances of gateways.
     */
    public ClientApplicationGateway() {
        // Instantiate the gateways
        fromClientGateway = new MessageReceiverGateway("brokerRequestQueue");
        toClientGateway = new MessageSenderGateway();

        fromClientGateway.setListener( new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                try {
                    // Create text message to be able to read its text
                    TextMessage textMessage = (TextMessage) msg;

                    // Deserialize strings into objet
                    var request = deserializeApprovalRequest(textMessage.getText());
                    // Get the reply destination
                    var replyTo = msg.getJMSReplyTo();

                    // Remember the reply destination by putting into a map with GraduationRequest as a key
                    requests.put(request, replyTo);

                    // Forward the request into abstract function implemented in the controller
                    onGraduationRequestReceived(request);

                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     *  Abstract function that is implemented in the controller. It allows to have access to the request in the controller.
     * @param request is the request to be forwarded.
     */
    public abstract void onGraduationRequestReceived(GraduationRequest request);

    /**
     * Is used to close all of the gateways connections.
     */
    public void stop() {
        toClientGateway.stop();
        fromClientGateway.stop();
    }

    /**
     * Deserializes ApprovalRequest.
     * @param body is JSON String object to be deserialized.
     * @return is deserialized object obtained from body.
     */
    public GraduationRequest deserializeApprovalRequest(String body) {
        return new Gson().fromJson(body, GraduationRequest.class);
    }

    /**
     * Serializes GraduationReply into JSON String object.
     * @param reply is the reply to be serialized.
     * @return is JSON String.
     */
    public String serializeGraduationReply(GraduationReply reply) {
        return new Gson().toJson(reply);
    }

    /**
     * Serializes GraduationRequest into JSON String object.
     * @param request is the request to be serialized.
     * @return is JSON String.
     */
    public String serializeGraduationRequest(GraduationRequest request) {
        return new Gson().toJson(request);
    }

    /**
     * Serializes both GraduationReply and GraduationRequest by serializing them alone and them joining them with &.
     * @param reply reply is the reply to be serialized.
     * @param request is the request to be serialized.
     * @return is JSON String of reply and request with & between them.
     */
    public String serializeGraduationReplyAndRequest(GraduationReply reply, GraduationRequest request) {
        return serializeGraduationReply(reply) + " & " + serializeGraduationRequest(request);
    }

    /**
     * Sends the original GraduationRequest along with GraduationReply back to the client.
     * @param reply is the reply that the approval gave.
     * @param request is the original request that was sent at the beginning of the process.
     */
    public void sendGraduationReply(GraduationReply reply, GraduationRequest request) {
        try {
            Message msg = toClientGateway.createTextMessage(serializeGraduationReplyAndRequest(reply, request));
            var replyTo = requests.get(request);
            toClientGateway.send(msg, replyTo);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}
