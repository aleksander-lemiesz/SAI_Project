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

    /**
     * Constructor of ApprovalApplicationGateway. It initializes all of the gateways.
     * @param soft is the queue name to software approval.
     * @param tech is the queue name to technical approval.
     * @param exam is the queue name to exam board approval.
     */
    public ApprovalApplicationGateway(String soft, String tech, String exam) {
        // Instantiate the gateways
        fromApprovalGateway = new MessageReceiverGateway("brokerReplyQueue");

        toSoftwareGateway = new MessageSenderGateway(soft);
        toTechnologyGateway = new MessageSenderGateway(tech);
        toExamBoardGateway = new MessageSenderGateway(exam);

        fromApprovalGateway.setListener(new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                try {

                    // Create text message to be able to read its text
                    TextMessage textMessage = (TextMessage) msg;

                    // Read and split the text message into reply and request part
                    String split[] = textMessage.getText().split(" & ");
                    // Deserialize strings into objets
                    ApprovalReply reply = deserializeApprovalReply(split[0]);
                    ApprovalRequest request = deserializeApprovalRequest(split[1]);

                    // Forward the objects into abstract function implemented in the controller
                    onApprovalReplyReceived(reply, request);

                } catch (JMSException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    /**
     * Abstract function that is implemented in the controller. It allows to have access to the reply and request in the controller.
     * @param reply is the reply received from the approval.
     * @param approvalRequestRequest is the original request to which the reply is correlated.
     */
    public abstract void onApprovalReplyReceived(ApprovalReply reply, ApprovalRequest approvalRequestRequest);

    /**
     * Is used to close all of the gateways connections.
     */
    public void stop() {
        toTechnologyGateway.stop();
        toSoftwareGateway.stop();
        toExamBoardGateway.stop();
        fromApprovalGateway.stop();
    }

    /**
     * Deserializes ApprovalReply.
     * @param body is JSON String object to be deserialized.
     * @return is deserialized object obtained from body.
     */
    public ApprovalReply deserializeApprovalReply(String body) {
        return new Gson().fromJson(body, ApprovalReply.class);
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
     * Serializes ApprovalRequest into JSON String object.
     * @param request is the request to be serialized.
     * @return is JSON String.
     */
    public String serializeApprovalRequest(ApprovalRequest request) {
        return new Gson().toJson(request);
    }

    /**
     * Sends Approval message to Technology department.
     * @param approvalRequest is the request to be sent.
     */
    public void sendApprovalRequestToTECH(ApprovalRequest approvalRequest) {
        try {
            Message msg = toTechnologyGateway.createTextMessage(serializeApprovalRequest(approvalRequest));
            toTechnologyGateway.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends Approval message to Software department.
     * @param approvalRequest is the request to be sent.
     */
    public void sendApprovalRequestToSOFT(ApprovalRequest approvalRequest) {
        try {
            Message msg = toSoftwareGateway.createTextMessage(serializeApprovalRequest(approvalRequest));
            toSoftwareGateway.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends Approval message to Exam Board .
     * @param approvalRequest is the request to be sent.
     */
    public void sendApprovalRequestToEXAM(ApprovalRequest approvalRequest) {
        try {
            Message msg = toExamBoardGateway.createTextMessage(serializeApprovalRequest(approvalRequest));
            toExamBoardGateway.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}
