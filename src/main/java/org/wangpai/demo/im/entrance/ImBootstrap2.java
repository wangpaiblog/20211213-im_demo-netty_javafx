package org.wangpai.demo.im.entrance;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.wangpai.demo.im.client.Client;
import org.wangpai.demo.im.server.Server;
import org.wangpai.demo.im.util.multithreading.CentralDatabase;
import org.wangpai.demo.im.util.multithreading.Multithreading;
import org.wangpai.demo.im.view.MainFace;

/**
 * @since 2021-12-1
 */
public class ImBootstrap2 extends Application {
    @Override
    public void start(Stage stage) {
        var myPort = 3322;
        var server = Server.getInstance().setPort(myPort);
        Multithreading.execute(() -> server.start());
        var otherIp = "127.0.0.1";
        var otherPort = 3311;
        var client = Client.getInstance().setIp(otherIp).setPort(otherPort);

        var mainFace = MainFace.getInstance();
        mainFace.setClient(client);
        server.setMainFace(mainFace);

        Scene scene = new Scene(mainFace.getComponent(), 500, 500);
        stage.setTitle("ImApp2");
        stage.setScene(scene);
        stage.setX(800);
        stage.setY(120);
        stage.show();

        stage.setOnCloseRequest(event -> {
            client.destroy();
            server.destroy();
            CentralDatabase.multithreadingClosed();
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}