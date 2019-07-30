/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package templateserver;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;                           //Imports necessary libraries
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

public class ServerDocumentController implements Initializable {
    @FXML
    private Label status, error, trumpLabel, winner, guessLabel;
    
    @FXML
    private TextField userInput;
    
    @FXML
    private Button submitButton;
    
    @FXML
    private GridPane gPane, gPaneLabels;
    
    @FXML
    private ImageView centerImage;
    
    @FXML
    private MenuItem connectMenu;
    
    private ArrayList<Integer> scores = new ArrayList();
    private boolean canSelect = false, isConnected = false;
    private String trump = "Spades";
    private int countID = 1, nextID = 1, numDeal = 7, turn = 0, offset = 0, turnsPlayed = 0;        //Creates instance fields
    private List<Card> deck = new ArrayList();
    private List<Card> fullDeck = new ArrayList();
    private List<Card> hand1D = new ArrayList();
    private List<Card> hand2D = new ArrayList();
    private List<Card> choices = new ArrayList();
    private ArrayList clientOutputStreams;
    private ArrayList<String> users;
    private ArrayList<Integer> roundWins = new ArrayList();
    private ArrayList<Integer> allGuesses = new ArrayList();
    
   public class ClientHandler implements Runnable	
   {
       BufferedReader reader;
       Socket sock;             
       PrintWriter client;

       public ClientHandler(Socket clientSocket, PrintWriter user) 
       {
            client = user;
            try {
                sock = clientSocket;
                InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(isReader);
            }
            catch (Exception ex) 
            {
                System.out.println("ERROR 1");
            }

       }

       @Override
       public void run() 
       {
            String message;
            try  {
                while ((message = reader.readLine()) != null) {
                    String prefix = message.substring(0, 3);            // This code runs when a client sends a message to the server
                    switch (prefix){                                 // Gets the prefix to better manage the code
                        case "img":
                            String path = message.substring(3);
                            tellEveryone("img" + path);             // Tells all of the clients the path to the image they need to update
                            break;
                        case "sel":
                            for (Card temp : fullDeck){
                                if (temp.getCardPath().equals(message.substring(3))){
                                    choices.add(temp);                                                    // Finds the card selected by the user
                                    tellEveryone("sut" + choices.get(0).cSuit.substring(0, 1));         // Tells everyone the suit of the original card
                                    centerImage.setImage(new Image(temp.getCardPath()));                // Sets the center image to this new card
                                    tellEveryone("img" + temp.getCardPath());                   // Tells everyone the path to this new card
                                    break;
                                } 
                            }
                            turn++;
                            turnsPlayed++;
                            if (turnsPlayed < nextID){
                                tellEveryone("trn" + turn);         // Prompts the next client to begin their turn
                            }
                            if (turnsPlayed >= nextID){
                                Platform.runLater(new Runnable() {
                                    public void run() {
                                        checkWinner();                  // Ones the number of turns exceeds the number of clients it checks for a winner
                                    }
                                });
                            }
                            else if (turn >= nextID){       // If the turn passes the last player and there are still more turns left...
                                turn = 0;           // Resets the turn to the fist player
                                canSelect = true;   // Allows teh first player to select their card
                                Platform.runLater(new Runnable() {
                                    public void run() {
                                       status.setText("Please select your card"); // Prompts the first player to select their card
                                    }
                                });
                            }
                            break;
                        case "add":
                            userAdd();      // Adds a client
                            isConnected = true;     // Sets a boolean representing the current connection status
                            break;
                        case "rem":
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    handleQuit();       // Quits the server if a client leaves the game
                                }
                            });
                            break;
                        case "set":
                            int tempNum = Integer.parseInt(message.substring(3));       // Determines the guess made by the client
                            allGuesses.add(tempNum);
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    updateData(allGuesses);     // Updates the data with the new guess
                                }
                            });
                            tellEveryone("set" + tempNum);  // Tells everyone what the new guess was
                            countID++;      // Increments a variable meant to count the number of times this code runs
                            if (countID < nextID){
                                tellEveryone("gus" + countID);  // Tells the next player to guess
                            }
                            else{
                                Platform.runLater(new Runnable() {
                                    public void run() {
                                        beginGame();        // Begins the game once everyone has guessed
                                    }
                                });
                            }     
                    }
                }
             } 
            catch (Exception ex) {
               if (isConnected){
                    Platform.runLater(new Runnable() {
                        public void run() {
                            handleQuit();  // Quits if there is an error after a client has connected
                        }
                    });
               }
                System.out.println("error");
            } 
	} 
    }                                                                               

    private void b_startActionPerformed() {                                        
        Thread starter = new Thread(new ServerStart());     // Begins a thread to monitor for messages and manage the clients
        starter.start();
    }                                       
    
    public class ServerStart implements Runnable 
    {
        @Override
        public void run() 
        {
            clientOutputStreams = new ArrayList();
            users = new ArrayList();  

            try 
            {
                ServerSocket serverSock = new ServerSocket(2222);

                while (true) 
                {
                    Socket clientSock = serverSock.accept();
                    PrintWriter writer = new PrintWriter(clientSock.getOutputStream());
                    clientOutputStreams.add(writer);

                    Thread listener = new Thread(new ClientHandler(clientSock, writer));
                    listener.start();
                }
            }
            catch (Exception ex)
            {
                System.out.println("ERROR 2");
            }
        }
    }

    public void userAdd () {
        users.add(String.valueOf(nextID));
        tellEveryone("add" + nextID);       // Adds a user and assigns them an id
        nextID++;       // Increments the id
    }
    
    public void userRemove (String data) {
        users.remove(data);     // Removes the user
    }
    
    public void tellEveryone(String message){
	Iterator it = clientOutputStreams.iterator();
        
        while (it.hasNext()){
            try{
                PrintWriter writer = (PrintWriter) it.next();
		writer.println(message);
                writer.flush();
            } 
            catch (Exception ex) {
		System.out.println("ERROR 1");
            }
        } 
    }
    
    @FXML
    private void beginGame(){
        tellEveryone("sut");  // Tells everyone to reset the suit
        turnsPlayed = 0;        // Resets a counter for the number of turns played
        if (offset == 0){  // If there is no offset, it makes the first player begin
            canSelect = true;
            status.setText("Please select your card");
        }
        else{
            tellEveryone("trn" + offset);  // Tells the next player to begin
        }
    }
    
    private void deleteColumn(GridPane grid, final int column) {
        Set<Node> deleteNodes = new HashSet<>();
        for (Node child : grid.getChildren()) {
            // get index from child
            Integer colIndex = GridPane.getColumnIndex(child);
            // handle null values for index=0
            int c = colIndex == null ? 0 : colIndex;
                                                                    // Code used from StackOverflow
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
    
    @FXML
    private void addCard(int index){
        if (canSelect){  // If the first player can select
            boolean hasSuit = false;  
            for (Card temp : hand1D){
                if (!choices.isEmpty()){
                    if (temp.cSuit.substring(0, 1).equals(choices.get(0).cSuit.subSequence(0, 1)) && !choices.isEmpty()){
                        hasSuit = true;  // If a card is found with the suit of the original card...
                        break;
                    }
                }
            }
            if (hasSuit){
                if (!(hand1D.get(index).cSuit.substring(0, 1).equals(choices.get(0).cSuit.substring(0, 1)))){
                    error.setVisible(true);  // If the card doesn't match the suit
                    error.setText("INVALID CARD");
                    return;
                }
            }
            error.setVisible(false);
            if (turn == 0){
                turnsPlayed++;  // Increments the number of turns played
                choices.add(hand1D.get(index));     // Adds the players choice
                if (offset == 0){
                    tellEveryone("sut" + hand1D.get(index).cSuit.substring(0, 1));  // If the first player is going, first, it will tell everyone the suit
                }
                canSelect = false;
                turn++;
                centerImage.setImage(new Image(hand1D.get(index).getCardPath()));
                tellEveryone("img" + hand1D.get(index).getCardPath());
                deleteColumn(gPane, index); hand1D.remove(index);
                if (turnsPlayed >= nextID){
                    checkWinner();
                } 
            }
            tellEveryone("trn" + turn);
            status.setText("Awaiting Others");
        }
    }
    
    private void checkWinner(){
        Card maxCard;
        int maxIndex = 0;
        ArrayList<Card> realCards = new ArrayList();
        ArrayList<Card> trumpCards = new ArrayList();       // Splits up the cards between an array of cards matching the suit and an array for those matching the trump
        String suit = choices.get(0).cSuit.substring(0,1);  // Gets the suit
        for (Card temp : choices){
            if (temp.getCardSuit().substring(0, 1).equals(suit)){
                realCards.add(temp);
            }
            else if(temp.getCardSuit().substring(0, 1).equals(trump.substring(0, 1))){
                trumpCards.add(temp);
            }
        }
        if (trumpCards.size() > 0){
            ArrayList<Integer> tempInts = new ArrayList();
            for (Card temp : trumpCards){
                tempInts.add(temp.cNumber);
            }
            int max = Collections.max(tempInts);            // Runs through the tumpcard array first because it is the most valuable and determines the maximum
            int index = 0;
            for (int i = 0; i < trumpCards.size(); i++){
                if (max == trumpCards.get(i).cNumber){
                    index = i;
                    break;
                }
            }
            maxCard = trumpCards.get(index);
            maxIndex = choices.indexOf(maxCard);
        }
        else if (realCards.size() > 0){
            ArrayList<Integer> tempInts = new ArrayList();
            for (Card temp : realCards){                        
                tempInts.add(temp.cNumber);
            }
            int max = Collections.max(tempInts);
            int index = 0;
            for (int i = 0; i < realCards.size(); i++){
                if (max == realCards.get(i).cNumber){
                    index = i;
                    break;
                }
            }
            maxCard = realCards.get(index);
            maxIndex = choices.indexOf(maxCard);
        }
        else{
            ArrayList<Integer> tempInts = new ArrayList();
            for (Card temp : choices){
                tempInts.add(temp.cNumber);
            }
            int max = Collections.max(tempInts);
            int index = 0;
            for (int i = 0; i < choices.size(); i++){
                if (max == choices.get(i).cNumber){
                    index = i;
                    break;
                }
            }
            maxCard = choices.get(index);
            maxIndex = choices.indexOf(maxCard);
        } 
        int playerIndex;
        if ((offset + maxIndex - nextID) >= 0){
            playerIndex = (offset + maxIndex) % maxIndex;       // Finds the index of the player without an offset
        }
        else{
            playerIndex = offset + maxIndex;
        }
        tellEveryone("win" + playerIndex);
        winner.setText("Winner is player " + (playerIndex + 1));        // Tells everyone the winner of the turn
        choices.clear();
        offset = playerIndex;
        turn = offset;
        int temp = roundWins.get(playerIndex);
        roundWins.set(playerIndex, temp + 1);       // Adds the win to the players number of rounds won
        if (hand1D.isEmpty()){
            beginNextRound();   // If that was the last card, it will begin a new round
        }
        else{
            beginNextTurn();  // Otherwise, it proceeds to the next turn
        }
    }
    
    private void beginNextRound(){
        for (int i = 0; i < nextID; i++){
            if (roundWins.get(i).equals(allGuesses.get(i))){
                String temp = "";
                temp += String.valueOf(allGuesses.get(i) + 10);         // If the number of wins equals the number of estimated wins it adds the score
                int current = scores.get(i) + Integer.parseInt(temp);
                scores.set(i, current);
            }
            else{
                String temp = "";
                temp += String.valueOf(-1*(allGuesses.get(i) + 10));        // Otherwise it subtracts the score
                int current = scores.get(i) + Integer.parseInt(temp);
                scores.set(i, current);
            }  
        }
        allGuesses.clear();
        deleteColumn(gPaneLabels, 0);
        countID = 1;
        submitButton.setVisible(true); userInput.setVisible(true);
        tellEveryone("new");
        tellEveryone("clr");
        centerImage.setImage(null);
        deck.clear();                       // Resets many of the variables
        deck.addAll(fullDeck);
        numDeal--;          // Decrements the number of cards to be delt
        gPane.getChildren().removeAll();
        for(int i = 0; i < numDeal; i++){
            int y = (int)(Math.random() * (52 - i));
            hand1D.add(deck.remove(y));
        }
        ImageView[] currentHand = new ImageView[numDeal];
        for(int i=0; i < currentHand.length; i++){
            currentHand[i] = new ImageView();
            currentHand[i].setFitHeight(Math.round(50 * ( 1.0 + Math.sqrt(5)/2.0)));
            currentHand[i].setFitWidth(75);          // Sets up everything for images
            gPane.add(currentHand[i], i, 0);
            gPane.setHgap(10);                  //horizontal gap in pixels
            gPane.setAlignment(Pos.CENTER);
            currentHand[i].setImage(new Image(hand1D.get(i).getCardPath()));
        }
        EventHandler z = new EventHandler<MouseEvent>(){        
            @Override
            public void handle(MouseEvent t) {
                int c = GridPane.getColumnIndex(((ImageView) t.getSource()));
                addCard(c);
            }       
        };
        for(int i=0; i < currentHand.length; i++){
            currentHand[i].setOnMouseClicked(z);     // Adds an eventlistener to all of the imageviews
        } 
        switch (trump){
            case "Spades":
                trump = "Diamonds";
                break;
            case "Diamonds":
                trump = "Clubs";
                break;                  // Cycles to the next trump
            case "Clubs":
                trump = "Hearts";
                break;
            case "Hearts":
                trump = "Spades";
                break;   
        }
        trumpLabel.setText("Current trump: " + trump);
        status.setText("Please select a card");
        turn = 0;
        turnsPlayed = 0;
        offset = 0;
        canSelect = true;
        tellEveryone("trp" + trump);
        tellEveryone("upd" + numDeal);
        int tempCount = 0;
        for (int i = 1; i <= (nextID - 1); i++){
            for(int j = 0;j < numDeal; j++){
                int y = (int)(Math.random()*((52 - numDeal) - i - tempCount));
                hand2D.add(deck.remove(y));
                tempCount++;                                                        // Redistributes cards
            }
            String paths = "";
            for (int j = 0; j < hand2D.size(); j++){
                paths += hand2D.get(j).getCardPath();
                paths += ":"; 
            }
            tellEveryone("ccd" + paths + i);
            hand2D.clear();
        }
    }
    
    @FXML
    private void beginNextTurn(){
        centerImage.setImage(null);
        tellEveryone("clr");        // Tells everyone to clear the centerimage and clears the current center image
        beginGame();  // Begins the next phase
    }
    
    @FXML
    private void handleStart(){
        userInput.setVisible(true); submitButton.setVisible(true); guessLabel.setVisible(true);
        for(int i = 1;i<14;i++){      
            deck.add(new Card("C" +Integer.toString(i+1))); fullDeck.add(new Card("C" +Integer.toString(i+1)));
            deck.add(new Card("S"+Integer.toString(i+1)));  fullDeck.add(new Card("S"+Integer.toString(i+1)));      // Creates an array of all the cards
            deck.add(new Card("H"+Integer.toString(i+1)));  fullDeck.add(new Card("H"+Integer.toString(i+1)));
            deck.add(new Card("D"+Integer.toString(i+1)));  fullDeck.add(new Card("D"+Integer.toString(i+1)));  
        }
        for(int i = 0; i < numDeal; i++){
            int y = (int)(Math.random() * (52 - i));        // Distributes random cards from the deck
            hand1D.add(deck.remove(y));
        }
        ImageView[] currentHand = new ImageView[numDeal];
        for(int i=0; i < currentHand.length; i++){
            currentHand[i] = new ImageView();
            currentHand[i].setFitHeight(Math.round(50 * ( 1.0 + Math.sqrt(5)/2.0)));
            currentHand[i].setFitWidth(75);          // Sets up everything for images
            gPane.add(currentHand[i], i, 0);
            gPane.setHgap(10);                  //horizontal gap in pixels
            gPane.setAlignment(Pos.CENTER);
            currentHand[i].setImage(new Image(hand1D.get(i).getCardPath()));
        }
        EventHandler z = new EventHandler<MouseEvent>(){        
            @Override
            public void handle(MouseEvent t) {
                int c = GridPane.getColumnIndex(((ImageView) t.getSource()));
                addCard(c);
            }       
        };
        for(int i=0; i < currentHand.length; i++){
            currentHand[i].setOnMouseClicked(z);     // Adds an eventlistener to all of the imageviews
        } 
        trumpLabel.setText("Current trump: " + trump);
        status.setText("It is your turn to estimate the number of sets that you will make");
        tellEveryone("trp" + trump);
        tellEveryone("upd" + numDeal);
        int tempCount = 0;
        for (int i = 0; i < nextID; i++){
            scores.add(0);
            roundWins.add(0);
        } 
        for (int i = 1; i <= (nextID - 1); i++){
            for(int j = 0;j < numDeal; j++){
                int y = (int)(Math.random()*((52 - numDeal) - i - tempCount));
                hand2D.add(deck.remove(y));
                tempCount++;
            }
            String paths = "";
            for (int j = 0; j < hand2D.size(); j++){
                paths += hand2D.get(j).getCardPath();
                paths += ":"; 
            }
            tellEveryone("ccd" + paths + i);
            hand2D.clear();
        }
    }
    
    @FXML
    private void handleQuit(){
        tellEveryone("qut");
        System.exit(0);     // Exits the game and tells all of the clients to quit
    } 
    
    @FXML
    private void checkEstimation(){
        if (isNumeric(userInput.getText())){
            if (Integer.parseInt(userInput.getText()) <= numDeal){
                int numGuessedSets = Integer.parseInt(userInput.getText());
                allGuesses.add(numGuessedSets);
                updateData(allGuesses);
                tellEveryone("set" + numGuessedSets);
                tellEveryone("gus" + countID);          // Mkes sure the estimation is valid and adds it to a gridpane
                error.setVisible(false);
                submitButton.setVisible(false);
                userInput.setVisible(false);
                status.setText("");
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
    
    private void updateData(ArrayList<Integer> allGuesses){
        for (int i=0; i < allGuesses.size(); i++){
            Label temp = new Label();
            temp.setText(allGuesses.get(i).toString());     // Sets up all of the labels
            gPaneLabels.add(temp, 0, i);
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
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        b_startActionPerformed();  // Begins attempoting to connect
    }
}
