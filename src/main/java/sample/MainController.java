package sample;

import com.oceanos.ros.core.connections.UDPClient;
import javafx.application.Platform;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import javafx.util.StringConverter;
import net.java.games.input.*;
import net.java.games.input.Component;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import javax.accessibility.AccessibleContext;
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
    @FXML
    ImageView videoView;

    @FXML
    Label heading;

    @FXML
    Label gamePadX;

    @FXML
    Label gamePadY;

    @FXML
    private Label thrusterValue;

    @FXML
    private TextField manualX1;

    @FXML
    private TextField manualY1;

    @FXML
    private Canvas gameCanvas;

    GraphicsContext gc;

    double w;
    double h;

    boolean paint = false;

    @FXML
    void sendData(ActionEvent event) {
        float x1 = Float.parseFloat(manualX1.getText());
        float y1 = Float.parseFloat(manualY1.getText());
        Platform.runLater(()->{
            /*x.setValue(Double.parseDouble(df.format(x1)));
            y.setValue(Double.parseDouble(df.format(y1)));*/
            dataPair.set(new Pair<>(Double.parseDouble(df.format(x1)), Double.parseDouble(df.format(y1))));
        });
    }

    DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));

    private UDPClient udpClient;

    ObjectProperty<Image> imageObjectProperty = new SimpleObjectProperty<>();
    /*DoubleProperty x = new SimpleDoubleProperty();
    DoubleProperty y = new SimpleDoubleProperty();*/

    ObjectProperty<Pair<Double, Double>> dataPair = new SimpleObjectProperty<>(new Pair<>(0d,0d));

    public void initialize() throws SocketException, UnknownHostException {

        w = gameCanvas.getWidth();
        h = gameCanvas.getHeight();


        videoView.imageProperty().bind(imageObjectProperty);

        try {
            imageObjectProperty.set(new Image(new File("C:\\Users\\Oceanos\\IdeaProjects\\roboCarClient\\src\\main\\resources\\webcam-capture-logo-small.jpg").toURI().toURL().toString()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        /*gamePadX.textProperty().bindBidirectional(dataPair, new Str);

        gamePadY.textProperty().bindBidirectional(y, new SimpleStringConverter());*/

        dataPair.addListener(p->{
            gamePadX.setText(String.valueOf(dataPair.get().getKey()));
            gamePadY.setText(String.valueOf(dataPair.get().getValue()));
        });

        initCanvas();

        startCameraClient();
        startCompassClient();

        startGamePad();
        startThrusterClient();
    }



    void startCameraClient(){
        try {
            udpClient = new UDPClient("192.168.10.82", 4446, 7000);
            udpClient.setOnRecived((bytes -> {
                //System.out.println("recived "+bytes.length);

                //byte[] bytearray = Base64.decode(new String(bytes));
                BufferedImage imag = null;
                try {
                    imag = ImageIO.read(new ByteArrayInputStream(bytes));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                BufferedImage finalImag = imag;
                Platform.runLater(()->{
                    imageObjectProperty.set(SwingFXUtils.toFXImage(finalImag, null));
                });
            }));
            udpClient.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void startCompassClient(){
        try {
            UDPClient compassClient = new UDPClient("192.168.10.82", 4447, 7000);

            compassClient.setOnRecived((b)->{
                Platform.runLater(()->{
                    heading.setText(new String(b));
                });
            });

            compassClient.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    void startGamePad(){
        new Thread(new Runnable() {
            public void run() {
                Controller[] controllers = ControllerEnvironment
                        .getDefaultEnvironment().getControllers();

                if (controllers.length == 0) {
                    System.out.println("Found no controllers.");
                    System.exit(0);
                }

                Controller gamePad = null;

                for (int i = 0; i < controllers.length; i++) {
                    if (controllers[i].getName().equals("Controller (Gamepad F310)")) {
                        gamePad = controllers[i];
                    }
                }

                if (gamePad == null){
                    System.out.println("Found no controllers.");
                    System.exit(0);
                }
                while (true) {
                    //System.out.println(controllers[i].getName());
                    /* Remember to poll each one */
                    gamePad.poll();
                    /* Get the controllers event queue */
                    EventQueue queue = gamePad.getEventQueue();

                    /* Create an event object for the underlying plugin to populate */
                    Event event = new Event();

                    /* For each object in the queue */
                    while (queue.getNextEvent(event)) {

                        System.out.println("event");

                        Component comp = event.getComponent();
                        //System.out.println(comp.getIdentifier().Axis.X);
                        if (comp.getIdentifier() == Component.Identifier.Axis.X){
                            System.out.println("set X");

                            Platform.runLater(()->{
                                //x.setValue(Double.parseDouble(df.format(event.getValue())));
                                if (Double.parseDouble(df.format(event.getValue())) == dataPair.get().getKey()) return;
                                dataPair.set(new Pair<>(Double.parseDouble(df.format(event.getValue())), Double.parseDouble(gamePadY.getText())));
                                //System.out.println(event.getValue());
                            });
                        }  /*if (comp.getIdentifier().getName().equals("ry")){
                            Platform.runLater(()->{
                                //y.setValue(event.getValue());
                                System.out.println(event.getValue());
                            });
                        } */if (comp.getIdentifier() == Component.Identifier.Axis.Z){
                            System.out.println("set Y");
                            Platform.runLater(()->{

                                //y.setValue(Double.parseDouble(df.format(event.getValue()*-1)));
                                if (Double.parseDouble(df.format(event.getValue())) == dataPair.get().getValue()) return;
                                dataPair.set(new Pair<>(Double.parseDouble(gamePadX.getText()), Double.parseDouble(df.format(event.getValue()))));
                                //System.out.println(event.getValue());

                            });
                            //System.out.println(event.getValue());
                        }

                    }

                    /*try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }*/
                }
            }
        }).start();
    }

    void startThrusterClient() throws SocketException, UnknownHostException {
        UDPClient thrusterClient = new UDPClient("192.168.10.82", 4448, 256);
        thrusterClient.setOnRecived(d-> System.out.println("Thruster client get data: "+new String(d)));
        thrusterClient.start();
        /*x.addListener(observable -> thrusterClient.sendData((x.get() + "," + y.get()).getBytes()));
        y.addListener(observable -> thrusterClient.sendData((x.get() + "," + y.get()).getBytes()));*/
        dataPair.addListener(observable -> {
            Pair<Double, Double> pair = dataPair.get();
            thrusterClient.sendData((pair.getKey() + "," + pair.getValue()).getBytes());
            System.out.println("send "+pair.getKey()+" "+pair.getValue());
        });
    }

    private void initCanvas() {
        gc = gameCanvas.getGraphicsContext2D();

        gameCanvas.setOnMouseDragged(e->{
            if (e.getY()>=0 && e.getX()>=0){
                fillCanvas(e.getX(), e.getY());
                double x = (w/2-e.getX())/(w/2);
                double y = (h/2-e.getY())/(h/2);
                System.out.println(df.format(x)+" "+df.format(y));
            }
        });


        fillCanvas(w/2,h/2);
    }

    private void fillCanvas(double x, double y){
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.RED);
        gc.fillRect(0,0,w,h);
        gc.strokeRect(0,0, w, h);
        gc.setFill(Color.GREEN);
        gc.fillOval(w/2-5,h/2-5, 10,10);
        gc.setFill(Color.YELLOW);
        gc.fillOval(x-10,y-10, 20,20);
        gc.setStroke(Color.BLUE);
        gc.strokeLine(w/2,h/2, x,y);
    }

    static class SimpleStringConverter extends StringConverter<Number>{

        @Override
        public String toString(Number object) {
            return String.valueOf(object);
        }

        @Override
        public Number fromString(String string) {
            return null;
        }
    }


}