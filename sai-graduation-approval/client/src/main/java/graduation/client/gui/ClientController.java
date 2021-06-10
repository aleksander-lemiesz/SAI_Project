package graduation.client.gui;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import shared.gui.ListViewLine;
import shared.model.client.GraduationReply;
import shared.model.client.GraduationRequest;
import shared.model.Group;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;


import java.net.URL;
import java.util.ResourceBundle;

@SuppressWarnings("WeakerAccess")
public class ClientController implements Initializable {

    private BrokerApplicationGateway gateway = null;

    @FXML
    private TextField tfStudentNumber;
    @FXML
    private TextField tfCompany;
    @FXML
    private TextField tfProjectName;
    @FXML
    private ComboBox<Group> cbGroup;
    @FXML
    private ListView<ListViewLine<GraduationRequest, GraduationReply>> lvRequestReply;

    public ClientController(){

    }

    @FXML
    private void btnSendRequestClicked(){
        // create the GraduationClientRequest
        int studentNumber = Integer.parseInt(tfStudentNumber.getText());
        String company = tfCompany.getText();
        String projectName = tfProjectName.getText();
        Group group = cbGroup.getSelectionModel().getSelectedItem();
        GraduationRequest graduationRequest = new GraduationRequest(studentNumber,company,projectName, group);

        //create the ListViewLine line with the request and add it to lvRequestReply
        ListViewLine<GraduationRequest, GraduationReply> listViewLine = new ListViewLine<>(graduationRequest);
        this.lvRequestReply.getItems().add(listViewLine);

        //send the graduationRequest
        try {
            gateway.applyForGraduation(graduationRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * This method returns the line of lvMessages which contains the given request.
     * @param request GraduationClientRequest for which the line of lvMessages should be found and returned
     * @return The ListViewLine line of lvMessages which contains the given request
     */
    private ListViewLine<GraduationRequest, GraduationReply> getRequestReply(GraduationRequest request) {
        for (ListViewLine<GraduationRequest, GraduationReply> listViewLine: lvRequestReply.getItems()) {
            if (listViewLine.getRequest() == request) {
                return listViewLine;
            }
        }
        return null;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tfStudentNumber.setText("123");
        tfCompany.setText("Philips");
        tfProjectName.setText("New website");
        // add all types of Group to the combo box
        cbGroup.getItems().addAll(Group.values());
        if (cbGroup.getItems().size() > 0){
            cbGroup.getSelectionModel().select(0);
        }

        gateway = new BrokerApplicationGateway() {
            @Override
            public void onGraduationReplyReceived(GraduationRequest request, GraduationReply reply) {
                getRequestReply(request).setReply(reply);
                Platform.runLater(() -> lvRequestReply.refresh());
            }
        };
    }

    /**
     * This method is called in ClientMain when closing this JavaFX stage, i.e., when closing the application.
     */
    public void stop() {

    }
}
