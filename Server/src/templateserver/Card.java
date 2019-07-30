package templateserver;
public class Card {
    
    String cSuit;
    int cNumber;
    String pathName;
    
    // contructor that starts the formation of the card path
    // the methods of this class allow the main class to obtain certain properties of a 
    // single card.
    public Card(String cName)
    {
        pathName = "Resources/" + cName + ".jpg";
        parseCardName(cName);
    }
    // 
    public void parseCardName(String cName)
    {
        cSuit = cName.substring(0); // creates the suit of a card
        cNumber = Integer.parseInt(cName.substring(1)); // creates the value of a card       
    }
    public int getCardNumber() // sends the card value when called
    {
        return cNumber;
    }
    public String getCardSuit() // sends the card suit when called
    {
        return cSuit;
    }
    public String getCardPath() // sends card path when called/ loaction in resources
    {
        return pathName;
    }

    
}
