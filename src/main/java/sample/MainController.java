package sample;


import com.oceanos.ros.core.connections.UDPClient;
import com.oceanos.ros.messages.MessageClient;
import com.oceanos.ros.messages.compass.CompassMessages;
import javafx.application.Platform;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import net.java.games.input.*;
import net.java.games.input.Component;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;


public class MainController {

    private static final String host = "192.168.11.82";

    private GraphicsContext gc;

    private double w;
    private double h;

    private BooleanProperty isKeyboardControll = new SimpleBooleanProperty(false);
    private MessageClient messageClient;
    private boolean running = true;
    private DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));
    private UDPClient udpClient;
    private ObjectProperty<Image> imageObjectProperty = new SimpleObjectProperty<>();
    private ObjectProperty<Pair<Double, Double>> dataPair = new SimpleObjectProperty<>(new Pair<>(0d, 0d));

    @FXML
    AnchorPane mainWindow;

    @FXML
    ImageView videoView;

    @FXML
    Label heading;

    @FXML
    Label gamePadX;

    @FXML
    Label gamePadY;

    @FXML
    ToggleButton keyboard;

    @FXML
    private TextField kpField;

    @FXML
    private TextField kiField;

    @FXML
    private TextField kdField;

    @FXML
    private Label thrusterValue;

    @FXML
    private TextField manualX1;

    @FXML
    private TextField manualY1;

    @FXML
    private Canvas gameCanvas;

    @FXML
    private TextField headingField;

    @FXML
    private Label wSpeed;

    @FXML
    private TextField speed;

    @FXML
    private void followHeading() {
        try {
            messageClient.sendMessage("followHeading", headingField.getText(),kpField.getText(), kiField.getText(), kdField.getText(),speed.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToHeading(ActionEvent event) {
        try {
            messageClient.sendMessage("goToHeading", headingField.getText() +
                    kpField.getText() + kiField.getText() + kdField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void sendData(ActionEvent event) {
        float x1 = Float.parseFloat(manualX1.getText());
        float y1 = Float.parseFloat(manualY1.getText());
        Platform.runLater(() -> dataPair.set(new Pair<>(Double.parseDouble(df.format(x1)), Double.parseDouble(df.format(y1)))));
    }

    @FXML
    void startCompassCalibration(ActionEvent event) {
        try {
            messageClient.sendMessage(CompassMessages.START_CALIBRATION.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void startHeadingStream(ActionEvent event) {
        try {
            messageClient.sendMessage(CompassMessages.START_HEADING_STREAM.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void stopHeadingStream(ActionEvent event) {
        try {
            messageClient.sendMessage(CompassMessages.STOP_HEADING_STREAM.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void stopThruster() {
        try {
            messageClient.sendMessage("stop");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initialize() throws SocketException, UnknownHostException {
        messageClient = new MessageClient(host, 4447);

        w = gameCanvas.getWidth();
        h = gameCanvas.getHeight();

        videoView.imageProperty().bind(imageObjectProperty);

        try {
            imageObjectProperty.set(new Image(new File("C:\\Users\\Oceanos\\IdeaProjects\\roboCarClient\\src\\main\\resources\\webcam-capture-logo-small.jpg").toURI().toURL().toString()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        dataPair.addListener(p -> {
            Platform.runLater(()->{
                gamePadX.setText(String.valueOf(dataPair.get().getKey()));
                gamePadY.setText(String.valueOf(dataPair.get().getValue()));
            });
        });

        initCanvas();
        startCameraClient();
        startCompassClient();
        startGamePad();
        startThrusterClient();
        isKeyboardControll.bind(keyboard.selectedProperty());
        activateKeyboard();
    }


    private void startCameraClient() {
        try {
            udpClient = new UDPClient(host, 4446, 90000);
            udpClient.setOnRecived((bytes -> {
                //byte[] bytearray = Base64.decode(new String(bytes));
                BufferedImage imag = null;
                try {
                    imag = ImageIO.read(new ByteArrayInputStream(bytes));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                BufferedImage finalImag = imag;
                Platform.runLater(() -> {
                    if (finalImag!=null){
                        imageObjectProperty.set(SwingFXUtils.toFXImage(finalImag, null));
                    }
                });
            }));
            udpClient.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startCompassClient() {
       messageClient.subscribe("compass", this::compassAccept);
    }

    private void startGamePad() {
        new Thread(() -> {
            Controller[] controllers = ControllerEnvironment
                    .getDefaultEnvironment().getControllers();

            if (controllers.length == 0) {
                System.out.println("Found no controllers.");
                System.exit(0);
            }

            Controller gamePad = null;

            for (Controller controller : controllers) {
                // Controller  (Wireless Gamepad F710)
                if (controller.getName().equals("Controller (Gamepad F310)")) {
                    gamePad = controller;
                }
            }

            if (gamePad == null) {
                System.out.println("Found no controllers.");
                //System.exit(0);
            }
            while (running) {
                //System.out.println(controllers[i].getName());
                /* Remember to poll each one */
                gamePad.poll();
                /* Get the controllers event queue */
                EventQueue queue = gamePad.getEventQueue();

                /* Create an event object for the underlying plugin to populate */
                Event event = new Event();

                /* For each object in the queue */
                while (queue.getNextEvent(event)) {

                    //System.out.println("event");

                    Component comp = event.getComponent();
                    //System.out.println(comp.getIdentifier().Axis.X);
                    if (comp.getIdentifier() == Component.Identifier.Axis.X) {
                        //System.out.println("set X");

                        //System.out.println(val);
                        //if (val == dataPair.get().getKey()) return;
                        //Platform.runLater(() -> {
                            double val = Math.round(event.getValue() * 100.0) / 100.0;
                            //x.setValue(Double.parseDouble(df.format(event.getValue())));
                            //if (Double.parseDouble(df.format(event.getValue())) == dataPair.get().getKey()) return;
                            dataPair.set(new Pair<>(val, Double.parseDouble(gamePadY.getText())));
                            //System.out.println(event.getValue());
                        //});
                    }  /*if (comp.getIdentifier().getName().equals("ry")){
                        Platform.runLater(()->{
                            //y.setValue(event.getValue());
                            System.out.println(event.getValue());
                        });
                    } */
                    if (comp.getIdentifier() == Component.Identifier.Axis.Z) {
                        //System.out.println("set Y");

                        //if (val == dataPair.get().getValue()) return;
                        //System.out.println(val);
                        //Platform.runLater(() -> {
                            double val = Math.round(event.getValue() * 100.0) / 100.0;
                            //y.setValue(Double.parseDouble(df.format(event.getValue()*-1)));
                            /*if (Double.parseDouble(df.format(event.getValue())) == dataPair.get().getValue())
                                return;*/

                            dataPair.set(new Pair<>(Double.parseDouble(gamePadX.getText()), val));
                            //System.out.println(event.getValue());

                        //});
                        //System.out.println(event.getValue());
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }


            }
        }).start();
    }

    private void startThrusterClient(){
        dataPair.addListener(observable -> {
            Pair<Double, Double> pair = dataPair.get();
            String msg = pair.getKey() + "," + pair.getValue();
            System.out.println("thruster "+msg);
            try {
                messageClient.sendMessage("thruster", String.valueOf(pair.getKey()), String.valueOf(pair.getValue()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        messageClient.subscribe("thruster_callback", s->{
            String[] values = s.split(",");
            Platform.runLater(()->thrusterValue.setText(values[0]+", "+values[1]));
        });
    }

    private void initCanvas() {
        gc = gameCanvas.getGraphicsContext2D();

        gameCanvas.setOnMouseDragged(e -> {
            if (e.getY() >= 0 && e.getX() >= 0) {
                fillCanvas(e.getX(), e.getY());
                processMouseValues(e.getX(), e.getY());
            }
        });

        gameCanvas.setOnMouseReleased(e -> {
            fillCanvas(w / 2, h / 2);
            processMouseValues(w / 2, h / 2);
        });

        fillCanvas(w / 2, h / 2);
    }

    private void processMouseValues(double x, double y) {
        x = -1 * (w / 2 - x) / (w / 2);
        y = -1 * (h / 2 - y) / (h / 2);
        System.out.println(df.format(x) + " " + df.format(y));
        dataPair.set(new Pair<>(Double.parseDouble(df.format(x)), Double.parseDouble(df.format(y))));
    }

    private void fillCanvas(double x, double y) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.RED);
        gc.fillRect(0, 0, w, h);
        gc.strokeRect(0, 0, w, h);
        gc.setFill(Color.GREEN);
        gc.fillOval(w / 2 - 5, h / 2 - 5, 10, 10);
        gc.setFill(Color.YELLOW);
        gc.fillOval(x - 10, y - 10, 20, 20);
        gc.setStroke(Color.BLUE);
        gc.strokeLine(w / 2, h / 2, x, y);
    }

    private void activateKeyboard(){
        mainWindow.setFocusTraversable(true);
            mainWindow.setOnKeyPressed((e)->{
                if (isKeyboardControll.get()){
                    switch (e.getCode()) {
                        case W:
                            dataPair.setValue(new Pair<>(0.0, -0.7));
                            System.out.println("forward");
                        break;
                        case S: dataPair.setValue(new Pair<>(0.0, 0.7));
                        break;
                        case A: dataPair.setValue(new Pair<>(-1.0, -0.7));
                        break;
                        case D: dataPair.setValue(new Pair<>(1.0, -0.7));
                    }
                    e.consume();
                }

            });


    }

    private void exit() {
        udpClient.stop();
        //compassClient.stop();
        //thrusterClient.stop();
        try {
            messageClient.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
    }


    private void compassAccept(String s) {
        Platform.runLater(() -> {
            String[] values = s.split(",");
            heading.setText(values[0]);
            wSpeed.setText(values[1]);
        });
    }
}
