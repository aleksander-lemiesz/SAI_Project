package broker.gui;

import com.google.gson.Gson;
import shared.model.MessageReceiverGateway;
import shared.model.MessageSenderGateway;
import shared.model.approval.ApprovalReply;
import shared.model.approval.ApprovalRequest;

import javax.jms.*;

public abstract class ApprovalApplicationGateway {

    //Senders
    private MessageSenderGateway toSoftwareGateway = null;
    private MessageSenderGateway toTechnologyGateway = null;
    private MessageSenderGateway toExamBoardGateway = null;

    //Receiver
    private MessageReceiverGateway fromBankGateway = null;

    public ApprovalApplicationGateway() {
        fromBankGateway = new MessageReceiverGateway("brokerReplyQueue");

        toSoftwareGateway = new MessageSenderGateway("softwareRequests");
        toTechnologyGateway = new MessageSenderGateway("technologyRequests");
        toExamBoardGateway = new MessageSenderGateway("examBoardRequests");

        fromBankGateway.setListener(new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                try {

                    TextMessage textMessage = (TextMessage) msg;

                    String split[] = textMessage.getText().split(" & ");
                    ApprovalReply reply = deserializeBankReply(split[0]);
                    ApprovalRequest request = deserializeBankRequest(split[1]);

                    onApprovalReplyReceived(reply, request);

                } catch (JMSException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    public abstract void onApprovalReplyReceived(ApprovalReply reply, ApprovalRequest bankRequest);

    public void stop() {
        toTechnologyGateway.stop();
        toSoftwareGateway.stop();
        toExamBoardGateway.stop();
        fromBankGateway.stop();
    }

    public ApprovalReply deserializeBankReply(String body) {
        return new Gson().fromJson(body, ApprovalReply.class);
    }

    public ApprovalRequest deserializeBankRequest(String body) {
        return new Gson().fromJson(body, ApprovalRequest.class);
    }

    public String serializeBankRequest(ApprovalRequest request) {
        return new Gson().toJson(request);
    }

    public void sendBankRequestToAMRO(ApprovalRequest bankRequest) {
        try {
            Message msg = toTechnologyGateway.createTextMessage(serializeBankRequest(bankRequest));
            toTechnologyGateway.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
    public void sendBankRequestToING(ApprovalRequest bankRequest) {
        try {
            Message msg = toSoftwareGateway.createTextMessage(serializeBankRequest(bankRequest));
            toSoftwareGateway.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void sendBankRequestToRABO(ApprovalRequest bankRequest) {
        try {
            Message msg = toExamBoardGateway.createTextMessage(serializeBankRequest(bankRequest));
            toExamBoardGateway.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}
