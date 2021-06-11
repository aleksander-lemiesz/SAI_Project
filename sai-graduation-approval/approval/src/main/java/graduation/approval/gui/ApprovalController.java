package graduation.approval.gui;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import shared.gui.ListViewLine;
import shared.model.approval.ApprovalReply;
import shared.model.approval.ApprovalRequest;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;

import java.net.URL;
import java.util.ResourceBundle;

class ApprovalController implements Initializable {

    private BrokerApplicationGateway gateway = null;

    private final String queueName;
    private final String approvalName;

    @FXML
    private ListView<ListViewLine<ApprovalRequest, ApprovalReply>> lvApprovalRequestReply;

    @FXML
    private CheckBox cbApproved;


    /**
     * Constructor of ApprovalController. It sets its name and queue name.
     * @param approvalName is the name of this new ApprovalController.
     * @param queueName is the name of queue this new ApprovalController.
     */
    public ApprovalController(String approvalName, String queueName){
        this.approvalName = approvalName;
        this.queueName = queueName;
        gateway = new BrokerApplicationGateway(queueName) {
            @Override
            public void onApprovalRequestReceived(ApprovalRequest request) {
                showApprovalRequest(request);
            }
        };
   }

    @FXML
    private void btnSendApprovalReplyClicked(){
        boolean isAccepted = cbApproved.isSelected();
        ApprovalReply approvalReply = new ApprovalReply(isAccepted, approvalName);

        ListViewLine<ApprovalRequest, ApprovalReply> listViewLine = lvApprovalRequestReply.getSelectionModel().getSelectedItem();
        if (listViewLine != null) {
            ApprovalRequest approvalRequest = listViewLine.getRequest();
            listViewLine.setReply(approvalReply);
            Platform.runLater(() -> this.lvApprovalRequestReply.refresh());

            gateway.sendApprovalReply(approvalRequest, approvalReply);

        } else {
            System.err.println("Please select one request in the list!");
        }
    }

    /**
     * Adds a new ListViewLine to lvApprovalRequestReply with graduationApprovalRequest
     */
    private void addApprovalRequest(ApprovalRequest approvalRequest){
        ListViewLine<ApprovalRequest, ApprovalReply> listViewLine = new ListViewLine<>(approvalRequest);
        this.lvApprovalRequestReply.getItems().add(listViewLine);
    }

    /**
     * This method returns the line of lvMessages which contains the given request.
     * @param request GraduationClientRequest for which the line of lvMessages should be found and returned
     * @return The ListViewLine line of lvMessages which contains the given request
     */
    private ListViewLine<ApprovalRequest, ApprovalReply> getRequestReply(ApprovalRequest request) {
        for (ListViewLine<ApprovalRequest, ApprovalReply> listViewLine: lvApprovalRequestReply.getItems()) {
            if (listViewLine.getRequest() == request) {
                return listViewLine;
            }
        }
        return null;
    }

    /**
     * This method is called in ApprovalMain when closing this JavaFX stage, i.e., when closing the application.
     */
    public void stop() {
        gateway.stop();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    /**
     * Updates the GUI with the new request.
     * @param approvalRequest is the new request to be displayed in the list.
     */
    private void showApprovalRequest(ApprovalRequest approvalRequest){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ListViewLine<ApprovalRequest, ApprovalReply> listViewLine = new ListViewLine<>(approvalRequest);
                lvApprovalRequestReply.getItems().add(listViewLine);
            }
        });
    }
}
