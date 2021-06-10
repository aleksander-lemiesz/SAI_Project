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

    public BrokerApplicationGateway() {
        // start connection
        msgSenderGateway = new MessageSenderGateway("brokerRequestQueue");

        msgReceiverGateway = new MessageReceiverGateway(replyQueue);
        msgReceiverGateway.setListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {

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

    public abstract void onGraduationReplyReceived(GraduationRequest request, GraduationReply reply);

    public String serializeGraduationRequest(GraduationRequest request) {
        return new Gson().toJson(request);
    }

    public GraduationReply deserializeGraduationReply(String body) {
        return new Gson().fromJson(body, GraduationReply.class);
    }

    public GraduationRequest deserializeGraduationRequest(String body) {
        return new Gson().fromJson(body, GraduationRequest.class);
    }

    public ListViewLine<GraduationRequest, GraduationReply> deserializeLoanReplyAndRequest(String body) {
        String split[] = body.split(" & ");
        GraduationReply reply = deserializeGraduationReply(split[0]);
        GraduationRequest request = deserializeGraduationRequest(split[1]);
        ListViewLine<GraduationRequest, GraduationReply> listViewLine = new ListViewLine<>(request);
        listViewLine.setReply(reply);
        return listViewLine;
    }

    public void stop() {
        msgSenderGateway.stop();
    }

}
