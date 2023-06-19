package elevators.som;

import clients.som.Client;

import java.util.ArrayList;

public class ElevatorsManager {
    private static final int MAX_ELEVATORS_NUMBER = 2;
    private ArrayList<Elevator> elevatorsList;

    public ElevatorsManager(){
        elevatorsList = new ArrayList<>();
    }

    public ArrayList<Elevator> getElevatorsList(){
        return this.elevatorsList;
    }

    public Elevator registerNewElevator(){
        if(elevatorsList.size() < MAX_ELEVATORS_NUMBER){
            Elevator elevator = new Elevator();
            elevatorsList.add(elevator);
            return elevator;
        }
        return null;
    }

    public void putClientInElevator(int chosenElevatorId, Client client){
        for(Elevator elevator : this.elevatorsList){
            if(elevator.getElevatorId() == chosenElevatorId){
                elevator.putPassengerInElevator(client);
                elevator.setIsBusy(true);
                elevator.setDestinationFloorNumber(client.getDestinationFloor());
            }
        }
    }

    public void deleteClientFromElevator(int elevatorId){
        for(Elevator elevator : this.elevatorsList){
            if(elevator.getElevatorId() == elevatorId){
                elevator.deletePassengerFromElevator();
            }
        }
    }
}
