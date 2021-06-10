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

    public abstract void onBankRequestReceived(ApprovalRequest request);

    public void sendBankReply(ApprovalRequest request, ApprovalReply reply) {
        try {

            // Serialize
            String serialized = serializeBankReplyAndRequest(reply, request);

            // Create the message
            TextMessage message = (TextMessage) msgSenderGateway.createTextMessage(serialized);

            // Send the reply message
            msgSenderGateway.send(message);

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public BrokerApplicationGateway(String queue) {
        //msgReceiverGateway = new MessageReceiverGateway("bankRequestQueue");
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
                    ApprovalRequest bankRequest = deserializeBankRequest(json);

                    //call abstr. meth. to pass the bankRequest
                    onBankRequestReceived(bankRequest);

                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public ApprovalRequest deserializeBankRequest(String body) {
        return new Gson().fromJson(body, ApprovalRequest.class);
    }

    public String serializeBankReply(ApprovalReply reply) {
        return new Gson().toJson(reply);
    }

    public String serializeBankRequest(ApprovalRequest request) {
        return new Gson().toJson(request);
    }

    public String serializeBankReplyAndRequest(ApprovalReply reply, ApprovalRequest request) {
        return serializeBankReply(reply) + " & " + serializeBankRequest(request);
    }

    public void stop() {
        msgReceiverGateway.stop();
        msgSenderGateway.stop();
    }

}
