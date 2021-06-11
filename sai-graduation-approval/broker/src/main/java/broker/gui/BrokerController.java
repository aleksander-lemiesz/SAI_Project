package broker.gui;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.glassfish.jersey.client.ClientConfig;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import shared.gui.ListViewLine;
import shared.model.approval.ApprovalReply;
import shared.model.approval.ApprovalRequest;
import shared.model.broker.Aggregator;
import shared.model.broker.StudentInfo;
import shared.model.client.GraduationReply;
import shared.model.client.GraduationRequest;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;


public class BrokerController implements Initializable {

    private ClientApplicationGateway clientGateway = null;
    private ApprovalApplicationGateway approvalGateway = null;

    // Linking GraduationRequests with ApprovalRequests
    private HashMap<ApprovalRequest, GraduationRequest> requests = new HashMap<>();

    // Linking ApprovalRequests with Aggregators
    private ArrayList<Aggregator> aggregations = new ArrayList<>();


    @FXML
    private ListView<ListViewLine<GraduationRequest, GraduationReply>> lvRequestReply = new ListView<>();

    public BrokerController() {
        clientGateway = new ClientApplicationGateway() {
            @Override
            public void onGraduationRequestReceived(GraduationRequest graduationRequest) {
                StudentInfo studentInfoRequest = getStudentInfo(graduationRequest.getStudentNumber());

                ApprovalRequest approvalRequest = new ApprovalRequest(graduationRequest.getStudentNumber(), graduationRequest.getCompany(),
                        graduationRequest.getProjectTitle(), studentInfoRequest.getGraduationPhaseECs(), graduationRequest.getGroup());

                boolean isValid = checkAndSendRequest(approvalRequest);

                requests.put(approvalRequest, graduationRequest);

                showGraduationRequest(graduationRequest);

                if (!isValid) {
                    rejectRequest(graduationRequest);
                }
            }
        };
        approvalGateway = new ApprovalApplicationGateway() {
            @Override
            public void onApprovalReplyReceived(ApprovalReply approvalReply, ApprovalRequest approvalRequest) {

                var index = getAggregatorIndex(approvalRequest);

                if (aggregations.get(index).getAggregationID() != 0) {

                    aggregations.get(index).AddReply(approvalReply);

                    if (aggregations.get(index).isReadyForFinalReply()) {

                        ApprovalReply evaluatedApprovalReply = aggregations.get(index).getEvaluatedApprovalReply();
                        GraduationReply evaluatedGraduationReply =
                                new GraduationReply(evaluatedApprovalReply.isApproved(), evaluatedApprovalReply.getName());

                        GraduationRequest request = requests.get(approvalRequest);

                        clientGateway.sendGraduationReply(evaluatedGraduationReply, request);

                        showAndUpdateGraduations(evaluatedGraduationReply, request);

                    }
                }
            }
        };

    }

    private void rejectRequest(GraduationRequest graduationRequest) {
        GraduationReply evaluatedGraduationReply = new GraduationReply(false, "InvalidRequest");

        clientGateway.sendGraduationReply(evaluatedGraduationReply, graduationRequest);
        showAndUpdateGraduations(evaluatedGraduationReply, graduationRequest);
    }

    public int getAggregatorIndex(ApprovalRequest request) {
        int toReturn = 0;

        int index = 0;
        for (Aggregator a : aggregations) {
            if (a.getRequest().equals(request)) {
                toReturn = index;
            }
            index++;
        }
        return toReturn;
    }

    private StudentInfo getStudentInfo(int sn) {

        // Start Connection
        WebTarget serviceTarget = null;
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newBuilder().withConfig(config).build();
        URI baseURI = UriBuilder.fromUri("http://localhost:9091/administration/students/" + sn).build();
        serviceTarget = client.target(baseURI);

        Invocation.Builder requestBuilder = serviceTarget.request().accept(MediaType.APPLICATION_JSON);
        Response response = requestBuilder.get();

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            StudentInfo entity = response.readEntity(StudentInfo.class);
            return entity;
        } else {
            System.err.println("ERROR: Cannot get path param! " + response);
            String entity = response.readEntity(String.class);
            System.err.println(entity);
            return null;
        }

    }

    /*
     Use this method to show each GraduationRequest (upon message arrival) on the frame in a thread-safe way.
     */
    private void showGraduationRequest(GraduationRequest request) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ListViewLine<GraduationRequest, GraduationReply> listViewLine = new ListViewLine<>(request);
                lvRequestReply.getItems().add(listViewLine);
            }
        });
    }

    private void showAndUpdateGraduations(GraduationReply graduationReply, GraduationRequest graduationRequest) {
        for (ListViewLine<GraduationRequest, GraduationReply> list : lvRequestReply.getItems()) {
            // assign reply to that line
            if (graduationRequest.equals(list.getRequest())) {
                list.setReply(graduationReply);
            }
        }
        // Refreshing the list
        Platform.runLater(() -> lvRequestReply.refresh());
    }

    void stop() {
        clientGateway.stop();
        approvalGateway.stop();
    }

    /*
    This method is executed by FX after the FX frame is initialized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public boolean checkAndSendRequest(ApprovalRequest approvalRequest) {

        Aggregator aggregator = new Aggregator(generateAggregationID(), approvalRequest);
        int numberOfTimesSent = 0;

        String SOFTWARE   = "ec >= 24 && group = 1";
        String TECHNOLOGY = "ec >= 24 && group = 2";
        String EXAM_BOARD = "ec >= 24 && ec < 30  && group != 3";
        String RETURN = "ec < 24  && group != 3";


        if (verifyExpression(SOFTWARE, approvalRequest)) {
            approvalGateway.sendApprovalRequestToSOFT(approvalRequest);
            numberOfTimesSent++;
        }
        if (verifyExpression(TECHNOLOGY, approvalRequest)) {
            approvalGateway.sendApprovalRequestToTECH(approvalRequest);
            numberOfTimesSent++;
        }
        if (verifyExpression(EXAM_BOARD, approvalRequest)) {
            approvalGateway.sendApprovalRequestToEXAM(approvalRequest);
            numberOfTimesSent++;
        }
        if (verifyExpression(RETURN, approvalRequest)) {
            return false;
        }
        aggregator.setNumberOfRepliesExpected(numberOfTimesSent);
        aggregations.add(aggregator);
        return true;

    }

    public boolean verifyExpression(String condition, ApprovalRequest approvalRequest) {
        int software = 1;
        int technology = 2;

        Argument ec = new Argument(" ec = " + approvalRequest.getEcs());
        Argument groupSoft = new Argument(" group = " + software + " ");
        Argument groupTech = new Argument(" group = " + technology + " ");

        // Evaluate rule:
        Expression expression = new Expression();

        if (approvalRequest.getGroup().toString().equals("SOFTWARE")) {
            expression = new Expression(condition, ec, groupSoft);
        } else if (approvalRequest.getGroup().toString().equals("TECHNOLOGY")) {
            expression = new Expression(condition, ec, groupTech);
        }
        double result = expression.calculate();
        return result == 1.0;// 1.0 means TRUE, otherwise it is FALSE
    }

    public int generateAggregationID() {
        int maxID = 0;
        if (!aggregations.isEmpty()) {
            for (Aggregator aggregator : aggregations) {
                if (maxID < aggregator.getAggregationID()) {
                    maxID = aggregator.getAggregationID();
                }
            }
        }
        return maxID + 1;
    }

}
