/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package temclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Line;

/**
 * FXML Controller class
 *
 * @author Yousef
 */

public class ClientDocumentController implements Initializable {
    @FXML
    private Label status, error, trumpLabel, winner, guessLabel, idLabel;
    
    @FXML
    private TextField userInput;
    
    @FXML
    private Button submitButton;
    
    @FXML
    private GridPane gPane, gPaneLabels;
    
    @FXML
    private ImageView centerImage;
    
    private ArrayList<String> pathList = new ArrayList();
    
    private String[] path;
    private int numDeal = 0, playerCount;
    private boolean added = false, canSelect = false;
    private String id, suit = "";
    private ArrayList<Integer> allGuesses = new ArrayList();
    String username, address = "localhost", paths = "";
    ArrayList<String> users = new ArrayList();
    int port = 2222;
    Boolean isConnected = false;
    
    Socket sock;
    BufferedReader reader;
    PrintWriter writer;
    
    public void ListenThread() {
        Thread IncomingReader = new Thread(new IncomingReader());
        IncomingReader.start();
    }
    
    public void sendDisconnect() {
        writer.println("bye" + id);
    }
    
    public class IncomingReader implements Runnable
    {
        @Override
        public void run() 
        {
            String stream;
            try {
                while ((stream = reader.readLine()) != null) {
                    String prefix = stream.substring(0, 3);
                    switch (prefix) {
                        case "new":
                            Platform.runLater(new Runnable(){
                                @Override
                                public void run(){
                                    allGuesses.clear();
                                    deleteColumn(gPaneLabels, 0);
                                    playerCount = 0;
                                }
                            });
                            break;
                        case "sut":
                            suit = stream.substring(3);
                            break;
                        case "win":
                            int num = Integer.parseInt(stream.substring(3));
                            Platform.runLater(new Runnable(){
                                @Override
                                public void run(){
                                    winner.setText("Winner is player " + (num + 1));
                                }
                            });
                            break;
                        case "trp":
                            String tempString = stream.substring(3);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    trumpLabel.setText("Current trump: " + tempString);
                                }
                            });
                            break;
                        case "gus":
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    status.setText("");
                                }
                            });
                            if (stream.substring(3).equals(id)){
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    status.setText("It is your turn to estimate the number of sets that you will make");
                                    gPaneLabels.setVisible(true);
                                    setVisible();
                                }
                            });
                            }
                            break;
                        case "qut":
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    handleQuit();
                                }
                            });
                            break;
                        case "set":
                            int tempNum = Integer.parseInt(stream.substring(3));
                            allGuesses.add(tempNum);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    updateData(allGuesses);
                                }
                            });
                            break;
                        case "add":
                            if (!added){
                                id = stream.substring(3);
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        int tempId = Integer.parseInt(id);
                                        tempId++;
                                        idLabel.setText(String.valueOf(tempId));
                                    }
                                });
                                added = true;
                            }
                            break;
                        case "upd":
                            numDeal = Integer.parseInt(stream.substring(3));
                            break;
                        case "ccd":    
                            String suffix = stream.substring(stream.length() - 1);
                            if (suffix.equals(id)){
                                paths = stream.substring(3, stream.length() - 1);
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        setCards();
                                    }
                                });
                            }      
                        case "trn":
                            if (stream.substring(3).equals(id)){
                                canSelect = true;
                                Platform.runLater(new Runnable() {
                                    public void run() {
                                        status.setText("Please select your card");
                                    }
                                });
                            }
                            break;
                        case "img":
                            String path = stream.substring(3);
                            Platform.runLater(new Runnable() {
                                    public void run() {
                                        centerImage.setImage(new Image(path));
                                    }
                                });
                            break;
                        case "clr":
                            Platform.runLater(new Runnable() {
                                public void run() {
                                        centerImage.setImage(null);
                                    }
                                });
                            break;
                    }        
                }
           }catch(Exception ex) { 
               if (isConnected){
                    Platform.runLater(new Runnable() {
                        public void run() {
                            handleQuit();
                        }
                    });
               }
           }
        }
    }                                     

    private void b_connectActionPerformed(){                                          
        while (isConnected == false){
            try {
                sock = new Socket(address, port);
                InputStreamReader streamreader = new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(streamreader);
                writer = new PrintWriter(sock.getOutputStream());
                writer.println("add");
                writer.flush();
                isConnected = true; 
            } 
            catch (Exception ex) {
                System.out.println("AWAITING SERVER");
            }
            ListenThread();   
        }
    }                                                                                                                                                               
    
    String clientCard;
   
    @FXML
    private void handleQuit(){
        writer.println("rem");
        writer.flush();
        System.exit(0);
    }
    
    @FXML
    private void checkEstimation(){
        if (isNumeric(userInput.getText())){
            if (Integer.parseInt(userInput.getText()) <= 8){
                int numGuessedSets = Integer.parseInt(userInput.getText());
                writer.println("set" + numGuessedSets);
                writer.flush();
                error.setVisible(false);
                submitButton.setVisible(false);
                userInput.setVisible(false);
            }
            else{
                error.setText("GUESS < NUM CARDS");
                error.setVisible(true);
            }
        }
        else{
            error.setText("INVALID INPUT");
            error.setVisible(true);
        }
    }
    
    private static boolean isNumeric(String str){   // Code to determine if a value can be converted to a number
        try{  
          double test = Integer.parseInt(str);  // Attempts to convert a string value to a double
        } 
        catch(NumberFormatException nfe){  
          return false;  // If there is an error, it will return false
        }  
        return true;  // Otherwise, it will return true which means that the value can be converted to a double
    }
    
    private void setCards(){
        ImageView[] currentHand = new ImageView[numDeal];
        path = paths.split(":");
        for(int i=0; i < currentHand.length; i++){
            currentHand[i] = new ImageView();
            currentHand[i].setFitHeight(Math.round(50 * ( 1.0 + Math.sqrt(5)/2.0)));
            currentHand[i].setFitWidth(75);          // Sets up everything for images
            gPane.add(currentHand[i], i, 0);
            gPane.setHgap(10);                  //horizontal gap in pixels
            gPane.setAlignment(Pos.CENTER);
            currentHand[i].setImage(new Image(path[i]));
            pathList.add(path[i]);
        }
        EventHandler z = new EventHandler<MouseEvent>(){        
            @Override
            public void handle(MouseEvent t) {
                int c = GridPane.getColumnIndex(((ImageView) t.getSource()));
                addCard(c);
            }
        };
        for (int i=0; i < currentHand.length; i++){
            currentHand[i].setOnMouseClicked(z);     // Adds an eventlistener to all of the imageviews
        } 
    }
    
    private void deleteColumn(GridPane grid, final int column) {
        Set<Node> deleteNodes = new HashSet<>();
        for (Node child : grid.getChildren()) {
            // get index from child
            Integer colIndex = GridPane.getColumnIndex(child);

            // handle null values for index=0
            int c = colIndex == null ? 0 : colIndex;
                                                                    // Code retrieved from StackOverflow
            if (c > column) {                                                  
                // decrement rows for rows after the deleted row
                GridPane.setColumnIndex(child, c-1);
            } else if (c == column) {
                // collect matching rows for deletion
                deleteNodes.add(child);
            }
        }

        // remove nodes from row
        grid.getChildren().removeAll(deleteNodes);
    }
    
    private void addCard(int c){
        if (canSelect){
            boolean hasSuit = false;
            int pos = 0;
            for (String tempSuit : pathList){
                pos = tempSuit.indexOf("/");
                String shortened = tempSuit.substring(pos + 1, pos + 2);
                if (!"".equals(suit)){
                    if (shortened.equals(suit)){
                        hasSuit = true;
                        break;
                    }
                }
            }
            if (hasSuit){
                if (!pathList.get(c).substring(pos + 1, pos + 2).equals(suit)){ 
                    error.setVisible(true);
                    error.setText("INVALID CARD");
                    return;
                }
            }
            error.setVisible(false);
            writer.println("sel" + pathList.get(c));
            writer.flush();
            canSelect = false;
            status.setText("Awaiting others");
            centerImage.setImage(new Image(pathList.get(c)));
            writer.println("img" + pathList.get(c));
            writer.flush();
            deleteColumn(gPane, c); pathList.remove(c);
        }
    }
    
    @FXML
    private void updateData(ArrayList<Integer> allGuesses){
        for (int i=0; i < allGuesses.size(); i++){
            Label temp = new Label();
            temp.setText(allGuesses.get(i).toString());     // Sets up all of the labels
            gPaneLabels.add(temp, 0, i);
        }
    }
    
    @FXML
    private void setVisible(){
        userInput.setVisible(true); guessLabel.setVisible(true);
        submitButton.setVisible(true);
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        b_connectActionPerformed();
    }
}
