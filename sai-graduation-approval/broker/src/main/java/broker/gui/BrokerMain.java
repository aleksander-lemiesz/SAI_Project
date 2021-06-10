package broker.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class BrokerMain extends Application {



    @Override
    public void start(final Stage primaryStage) throws IOException {
        final Logger logger = LoggerFactory.getLogger(BrokerMain.class);

        final String fxmlFileName = "broker.fxml";
        URL url = getClass().getClassLoader().getResource(fxmlFileName);
        if (url != null) {
            FXMLLoader loader = new FXMLLoader(url);
            BrokerController controller = new BrokerController();
            loader.setController(controller);
            Parent root = loader.load();

           // EXIT this application when this stage is closed
            primaryStage.setOnCloseRequest(new EventHandler<>() {
                @Override
                public void handle(WindowEvent t) {
                    logger.info("Closing broker .....");
                    controller.stop();
                    Platform.exit();
                    System.exit(0);
                }
            });
            // set the stage title, icon and size
            primaryStage.setTitle("BROKER");
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/bank.png")));
                primaryStage.setScene(new Scene(root, 500, 300));
                // show the stage
                primaryStage.show();


        } else {
            logger.error("Could not load frame from "+fxmlFileName);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
