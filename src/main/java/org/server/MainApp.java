/* ----------------------------------------------------------
   Server
   usage: java Server [RTSP listening port]
   ---------------------------------------------------------- */
package org.server;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainApp extends Application
{
	/**----------------------------------------------------------------
	 * Main executor.
	 * ----------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		launch(args);
	}

	/**----------------------------------------------------------------
	 * Starts application.
	 * Initializes UI components and opens primary stage.
	 * ----------------------------------------------------------------*/
	@Override
	public void start(Stage primaryStage) throws Exception
	{
		CommunicationService service = new CommunicationService(new Server());

        primaryStage.setTitle("Stream Server");
        Button closeButton = new Button();
        closeButton.setText("Close");

        // stop application when "close" button is clicked
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Stopping stream server");
                service.cancel();
                stop(primaryStage);
            }
        });

        // stop application when application window is closed
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) { System.exit(0); }
        });

        StackPane root = new StackPane();
        root.getChildren().add(closeButton);
        primaryStage.setScene(new Scene(root, 200, 150));
        primaryStage.show();

        service.start();
	}

	/**----------------------------------------------------------------
	 * Stops underlying services before closing the primary application stage.
	 * ----------------------------------------------------------------*/
	public void stop(Stage primaryStage)
	{
		primaryStage.close();
    	System.exit(0);
	}
}