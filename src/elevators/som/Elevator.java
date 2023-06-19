package elevators.som;

import clients.som.Client;

import java.util.ArrayList;

public class Elevator {
    private static final int ELEVATOR_CAPACITY = 1;
    private static int ELEVATORS_COUNTER = 1;
    private final int elevatorId;
    private int currentFloorNumber;
    private boolean isBusy;
    private int destinationFloorNumber;
    private ArrayList<Client> elevatorPassengers;

    public Elevator(){
        this.elevatorId = ELEVATORS_COUNTER++;
        this.currentFloorNumber = 0;
        this.isBusy = false;
        this.destinationFloorNumber = -1;
        this.elevatorPassengers = new ArrayList<>(ELEVATOR_CAPACITY);
    }

    public Elevator(int elevatorId){
        this.elevatorId = elevatorId;
    }

    public int getElevatorId() {
        return elevatorId;
    }

    public int getCurrentFloorNumber() {
        return currentFloorNumber;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public int getDestinationFloorNumber() {
        return destinationFloorNumber;
    }

    public ArrayList<Client> getElevatorPassengers() {
        return elevatorPassengers;
    }

    public void putPassengerInElevator(Client client){
        this.elevatorPassengers.add(client);
    }

    public void setIsBusy(boolean isBusy){
        this.isBusy = isBusy;
    }

    public void setDestinationFloorNumber(int destinationFloorNumber){
        this.destinationFloorNumber = destinationFloorNumber;
    }

    public void deletePassengerFromElevator(){
        this.elevatorPassengers.set(0, null);
    }
}