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
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

    // Stores conditions where to forward requests
    ArrayList<String> conditions = null;


    @FXML
    private ListView<ListViewLine<GraduationRequest, GraduationReply>> lvRequestReply = new ListView<>();

    /**
     * Constructor of BrokerController. It represents the algorithm of broker operations due to the abstract functions form gateways.
     */
    public BrokerController() {

        // Loading the file with conditions and queues needed to send the messages
        ArrayList<String> fileLines = readFileLines("groups.ini");
        // Filling in the conditions list for all the approvals
        conditions = getConditionsFromLines(fileLines);

        // Instantiating the clientGateway
        clientGateway = new ClientApplicationGateway() {
            @Override
            public void onGraduationRequestReceived(GraduationRequest graduationRequest) {
                // Issuing the GET operations to receive the student details
                StudentInfo studentInfoRequest = getStudentInfo(graduationRequest.getStudentNumber());

                // Combining the information from GraduationRequest and StudentInfo into approval request (enriching)
                ApprovalRequest approvalRequest = new ApprovalRequest(graduationRequest.getStudentNumber(), graduationRequest.getCompany(),
                        graduationRequest.getProjectTitle(), studentInfoRequest.getGraduationPhaseECs(), graduationRequest.getGroup());

                // Saving if the student has enough points to forward his request
                boolean isValid = checkAndSendRequest(approvalRequest);

                // Remembering graduationRequest with the key of newly created approvalRequest
                requests.put(approvalRequest, graduationRequest);

                // Refreshing the GUI
                showGraduationRequest(graduationRequest);

                // Checking if the request should be rejected
                if (!isValid) {
                    // rejecting the request
                    rejectRequest(graduationRequest);
                }
            }
        };

        // Extracting the queues form the loaded file
        ArrayList<String> queues = getQueuesFromLines(fileLines);

        // Instantiating approval gateway using the previously extracted queue names
        approvalGateway = new ApprovalApplicationGateway(queues.get(0), queues.get(1), queues.get(2)) {
            @Override
            public void onApprovalReplyReceived(ApprovalReply approvalReply, ApprovalRequest approvalRequest) {

                // Identifying the position in the array of the returned request
                var index = getAggregatorIndex(approvalRequest);

                // Checking if the request has properly assigned aggregation ID
                if (aggregations.get(index).getAggregationID() != 0) {

                    // Adding reply to the request in the array
                    aggregations.get(index).AddReply(approvalReply);

                    // Checking if the request has received replies from all sources that got it
                    if (aggregations.get(index).isReadyForFinalReply()) {

                        // Evaluating if the reply is positive or negative
                        ApprovalReply evaluatedApprovalReply = aggregations.get(index).getEvaluatedApprovalReply();
                        GraduationReply evaluatedGraduationReply =
                                new GraduationReply(evaluatedApprovalReply.isApproved(), evaluatedApprovalReply.getName());

                        // Remembering the original GraduationRequest form the hashmap using approvalRequest as the key
                        GraduationRequest request = requests.get(approvalRequest);

                        // Sending the reply and request back to the client
                        clientGateway.sendGraduationReply(evaluatedGraduationReply, request);

                        // Updating the GUI
                        showAndUpdateGraduations(evaluatedGraduationReply, request);

                    }
                }
            }
        };

    }

    /**
     * Opens a text file and saves each of the lines into the String inside ArrayList.
     * @param file is a text file to be loaded.
     * @return is the ArrayList of Strings where each String contains one line of the file.
     */
    private ArrayList<String> readFileLines(String file) {
        ArrayList<String> fileLines = new ArrayList<>();

        try {
            FileReader reader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(reader);

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                fileLines.add(line);
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileLines;
    }

    /**
     * Returns queue names extracted from the ArrayList of Strings extracted by readFileLines.
     * @param lines are lines of the file extracted by readFileLines.
     * @return queue names in the ArrayList of Strings.
     */
    private ArrayList<String> getQueuesFromLines(ArrayList<String> lines) {
        ArrayList<String> list = new ArrayList<>();

        for (String line : lines) {
            list.add(getQueueFromLine(line));
        }
        return list;
    }

    /**
     * Gets one queue name form one line.
     * @param line is the line of the file extracted by readFileLines
     * @return is the name of queue
     */
    private String getQueueFromLine(String line) {
        // In the file # in the separator of the condition and queue
        String[] parts = line.split("#");
        return parts[1];
    }

    /**
     * Returns conditions extracted from the ArrayList of Strings extracted by readFileLines.
     * @param lines are lines of the file extracted by readFileLines.
     * @return conditions to be met by request in the ArrayList of Strings.
     */
    private ArrayList<String> getConditionsFromLines(ArrayList<String> lines) {
        ArrayList<String> list = new ArrayList<>();

        for (String line : lines) {
            list.add(getConditionFromLine(line));
        }
        return list;
    }

    /**
     * Gets one condition name form one line.
     * @param line is the line of the file extracted by readFileLines.
     * @return is the condition.
     */
    private String getConditionFromLine(String line) {
        // In the file # in the separator of the condition and queue
        String[] parts = line.split("#");
        return parts[0];
    }

    /**
     * Rejects the request by making predefined reply and sending it to the client.
     * @param graduationRequest is the rejected request.
     */
    private void rejectRequest(GraduationRequest graduationRequest) {
        GraduationReply evaluatedGraduationReply = new GraduationReply(false, "InvalidRequest");

        clientGateway.sendGraduationReply(evaluatedGraduationReply, graduationRequest);
        showAndUpdateGraduations(evaluatedGraduationReply, graduationRequest);
    }

    /**
     * Gets the Aggregator index of the ApprovalRequest.
     * @param request is the request to be checked.
     * @return is the index of request.
     */
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

    /**
     * Gets the student details form the student administration system.
     * @param sn is the student number of the student that the detail are to be checked.
     * @return is the information gathered about the student form the system.
     */
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

    /**
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

    /**
     * Another method to show each GraduationRequest (upon message arrival) on the frame in a thread-safe way.
     */
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

    /**
     * Is used to close all of the gateways connections.
     */
    void stop() {
        clientGateway.stop();
        approvalGateway.stop();
    }

    /**
    This method is executed by FX after the FX frame is initialized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    /**
     * Checks if and where the message is sent. It uses the previously extracted conditions form the file.
     * @param approvalRequest is the request to be checked.
     * @return is true if the request is forwarded and false if the message is to be rejected
     */
    public boolean checkAndSendRequest(ApprovalRequest approvalRequest) {

        // Create aggregator to store aggregation ID and request assigned to it
        Aggregator aggregator = new Aggregator(generateAggregationID(), approvalRequest);
        // The message has not been sent yet
        int numberOfTimesSent = 0;

        // Loading extracted conditions
        String SOFTWARE = conditions.get(0);
        String TECHNOLOGY = conditions.get(1);
        String EXAM_BOARD = conditions.get(2);
        String RETURN = conditions.get(3);

        // The condition for rejection is met so stop the function and return false
        if (verifyExpression(RETURN, approvalRequest)) {
            return false;
        }
        // Verify the conditions against the request and increase the numberOfTimesSent each time the request is forwarded
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
        // Set the number of replies expected in the aggregator to the number of times the request is forwarded
        aggregator.setNumberOfRepliesExpected(numberOfTimesSent);
        // Add the aggregator to the list
        aggregations.add(aggregator);
        // Return true because the request has been forwarded at least once
        return true;

    }

    /**
     * Verifies the condition against request.
     * @param condition is the condition to be tested.
     * @param approvalRequest is the request on which the check is performed.
     * @return true if the condition is met or false if the condition is not met.
     */
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

    /**
     * Generates new aggregation ID.
     * @return is the new aggregation ID which is current highest ID plus one.
     */
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
