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

    private ClientApplicationGateway loanGateway = null;
    private ApprovalApplicationGateway bankGateway = null;

    // Linking LoanRequests with BankRequests
    private HashMap<ApprovalRequest, GraduationRequest> requests = new HashMap<>();

    // Linking BankRequests with Aggregators
    private ArrayList<Aggregator> aggregations = new ArrayList<>();


    @FXML
    private ListView<ListViewLine<GraduationRequest, GraduationReply>> lvLoanRequestReply = new ListView<>();

    public BrokerController() {
        loanGateway = new ClientApplicationGateway() {
            @Override
            public void onGraduationRequestReceived(GraduationRequest graduationRequest) {
                StudentInfo studentInfoRequest = getStudentInfo(graduationRequest.getStudentNumber());

                ApprovalRequest approvalRequest = new ApprovalRequest(graduationRequest.getStudentNumber(), graduationRequest.getCompany(),
                        graduationRequest.getProjectTitle(), studentInfoRequest.getEc(), graduationRequest.getGroup());

                checkAndSendRequest(approvalRequest);

                requests.put(approvalRequest, graduationRequest);

                showGraduationRequest(graduationRequest);
            }
        };
        bankGateway = new ApprovalApplicationGateway() {
            @Override
            public void onApprovalReplyReceived(ApprovalReply approvalReply, ApprovalRequest approvalRequest) {

                var index = getAggregatorIndex(approvalRequest);
                System.out.println("Aggregator from get: " + aggregations.get(index));

                if (aggregations.get(index).getAggregationID() != 0) {

                    aggregations.get(index).AddReply(approvalReply);

                    if (aggregations.get(index).isReadyForFinalReply()) {

                        ApprovalReply evaluatedBankReply = aggregations.get(index).getBestBankReply();
                        GraduationReply evaluatedGraduationReply =
                                new GraduationReply(evaluatedBankReply.isApproved(), evaluatedBankReply.getName());

                        GraduationRequest request = requests.get(approvalRequest);
                        loanGateway.sendLoanReply(evaluatedGraduationReply, request);

                        showAndUpdateGraduations(evaluatedGraduationReply, request);

                    }
                }
            }
        };

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
            //System.out.println("The service response is: " + entity);
            return entity;
        } else {
            System.err.println("ERROR: Cannot get path param! " + response);
            String entity = response.readEntity(String.class);
            System.err.println(entity);
            return null;
        }

    }

    /*
     Use this method to show each bankRequest (upon message arrival) on the frame in a thread-safe way.
     */
    private void showGraduationRequest(GraduationRequest request) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ListViewLine<GraduationRequest, GraduationReply> listViewLine = new ListViewLine<>(request);
                lvLoanRequestReply.getItems().add(listViewLine);
            }
        });
    }

    private void showAndUpdateGraduations(GraduationReply loanReply, GraduationRequest request) {
        for (ListViewLine<GraduationRequest, GraduationReply> list : lvLoanRequestReply.getItems()) {
            // assign reply to that line
            if (request.equals(list.getRequest())) {
                list.setReply(loanReply);
            }
        }
        // Refreshing the list
        Platform.runLater(() -> lvLoanRequestReply.refresh());
    }

    void stop() {
        loanGateway.stop();
        bankGateway.stop();
    }

    /*
    This method is executed by FX after the FX frame is initialized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void checkAndSendRequest(ApprovalRequest bankRequest) {

        Aggregator aggregator = new Aggregator(generateAggregationID(), bankRequest);
        int numberOfTimesSent = 0;

        String SOFTWARE   = "ec >= 24 && group == 'software'";
        String TECHNOLOGY = "ec >= 24 && group == 'technology'";
        String EXAM_BOARD = "ec < 24  && group != ''";

        if (verifyExpression(SOFTWARE, bankRequest)) {
            bankGateway.sendBankRequestToING(bankRequest);
            numberOfTimesSent++;
        }
        if (verifyExpression(TECHNOLOGY, bankRequest)) {
            bankGateway.sendBankRequestToAMRO(bankRequest);
            numberOfTimesSent++;
        }
        if (verifyExpression(EXAM_BOARD, bankRequest)) {
            bankGateway.sendBankRequestToRABO(bankRequest);
            numberOfTimesSent++;
        }
        aggregator.setNumberOfRepliesExpected(numberOfTimesSent);
        System.out.println("Aggregator added to the array: " + aggregator);
        aggregations.add(aggregator);

    }

    public boolean verifyExpression(String condition, ApprovalRequest approvalRequest) {
        Argument ec = new Argument(" amount = " + approvalRequest.getEcs() + " ");
        Argument group = new Argument(" time = " + approvalRequest.getGroup() + " ");
        // Evaluate rule:
        Expression expression = new Expression(condition, ec, group);
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
