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

    public ClientApplicationGateway() {
        fromClientGateway = new MessageReceiverGateway("brokerRequestQueue");
        toClientGateway = new MessageSenderGateway();

        fromClientGateway.setListener( new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                try {

                    TextMessage textMessage = (TextMessage) msg;
                    var request = deserializeApprovalRequest(textMessage.getText());
                    var replyTo = msg.getJMSReplyTo();
                    requests.put(request, replyTo);

                    onGraduationRequestReceived(request);

                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public abstract void onGraduationRequestReceived(GraduationRequest request);

    public void stop() {
        toClientGateway.stop();
        fromClientGateway.stop();
    }

    public GraduationRequest deserializeApprovalRequest(String body) {
        return new Gson().fromJson(body, GraduationRequest.class);
    }

    public String serializeGraduationReply(GraduationReply reply) {
        return new Gson().toJson(reply);
    }

    public String serializeGraduationRequest(GraduationRequest request) {
        return new Gson().toJson(request);
    }

    public String serializeGraduationReplyAndRequest(GraduationReply reply, GraduationRequest request) {
        return serializeGraduationReply(reply) + " & " + serializeGraduationRequest(request);
    }

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
