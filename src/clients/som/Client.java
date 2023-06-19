package clients.som;

import java.util.Random;

public class Client {
    private static int CLIENTS_COUNTER = 0;
    private final int clientId;
    private int currentFloor;
    private int destinationFloor;

    public Client(){
        this.clientId = CLIENTS_COUNTER++;
        this.currentFloor = 0;
        this.destinationFloor = -1;
    }

    // Constructor for BuildingFederateAmbassador:
    public Client(int clientId){
        this.clientId = clientId;
    }

    // Constructor for ElevatorsFederateAmbassador and StaircaseFederateAmbassador:
    public Client(int clientId, int destinationFloor){
        this.clientId = clientId;
        this.destinationFloor = destinationFloor;
    }

    public void setDestinationFloor(){
        Random random = new Random();
        this.destinationFloor = random.nextInt(2) + 1;
    }

    public int getClientId() {
        return clientId;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public static int getClientsCounter(){
        return CLIENTS_COUNTER;
    }
}