package us.rin.helpers;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class FxFixer extends Application {

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        primaryStage.initStyle(javafx.stage.StageStyle.UTILITY);
        primaryStage.setOpacity(0);
        primaryStage.setIconified(true);
        primaryStage.setResizable(false);
        primaryStage.setWidth(1);
        primaryStage.setHeight(1);
        primaryStage.setAlwaysOnTop(false);
        primaryStage.hide();
    }

    public static void runFxFixer() {
        Thread thread = new Thread(() -> {
            try {
                launch();
            } catch (Throwable e) {
                System.out.println("JavaFX not supported on this platform: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
