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
    private MessageReceiverGateway fromApprovalGateway = null;

    public ApprovalApplicationGateway() {
        fromApprovalGateway = new MessageReceiverGateway("brokerReplyQueue");

        toSoftwareGateway = new MessageSenderGateway("softwareRequests");
        toTechnologyGateway = new MessageSenderGateway("technologyRequests");
        toExamBoardGateway = new MessageSenderGateway("examBoardRequests");

        fromApprovalGateway.setListener(new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                try {

                    TextMessage textMessage = (TextMessage) msg;

                    String split[] = textMessage.getText().split(" & ");
                    ApprovalReply reply = deserializeApprovalReply(split[0]);
                    ApprovalRequest request = deserializeApprovalRequest(split[1]);

                    onApprovalReplyReceived(reply, request);

                } catch (JMSException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    public abstract void onApprovalReplyReceived(ApprovalReply reply, ApprovalRequest approvalRequestRequest);

    public void stop() {
        toTechnologyGateway.stop();
        toSoftwareGateway.stop();
        toExamBoardGateway.stop();
        fromApprovalGateway.stop();
    }

    public ApprovalReply deserializeApprovalReply(String body) {
        return new Gson().fromJson(body, ApprovalReply.class);
    }

    public ApprovalRequest deserializeApprovalRequest(String body) {
        return new Gson().fromJson(body, ApprovalRequest.class);
    }

    public String serializeApprovalRequest(ApprovalRequest request) {
        return new Gson().toJson(request);
    }

    public void sendApprovalRequestToTECH(ApprovalRequest approvalRequest) {
        try {
            Message msg = toTechnologyGateway.createTextMessage(serializeApprovalRequest(approvalRequest));
            toTechnologyGateway.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
    public void sendApprovalRequestToSOFT(ApprovalRequest approvalRequest) {
        try {
            Message msg = toSoftwareGateway.createTextMessage(serializeApprovalRequest(approvalRequest));
            toSoftwareGateway.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void sendApprovalRequestToEXAM(ApprovalRequest approvalRequest) {
        try {
            Message msg = toExamBoardGateway.createTextMessage(serializeApprovalRequest(approvalRequest));
            toExamBoardGateway.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}
