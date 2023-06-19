package building.som;

import clients.som.Client;
import elevators.som.Elevator;
import staircase.som.Staircase;

import java.util.ArrayList;

public class Building {
    private ArrayList<Elevator> elevatorsList;
    private ArrayList<Staircase> staircasesList;
    private ArrayList<Client> clientsList;

    public Building(){
        this.elevatorsList = new ArrayList<>();
        this.staircasesList = new ArrayList<>();
        this.clientsList = new ArrayList<>();
    }

    public void addElevatorRegister(Elevator elevator){
        elevatorsList.add(elevator);
    }

    public void addStaircaseRegister(Staircase staircase){
        staircasesList.add(staircase);
    }

    public void addClientRegister(Client client){
        clientsList.add(client);
    }

    public ArrayList<Elevator> getElevatorsList(){
        return this.elevatorsList;
    }

    public ArrayList<Client> getClientsList(){
        return this.clientsList;
    }
}
