package staircase.som;

import clients.som.Client;

import java.util.ArrayList;

public class StaircaseManager {
    private static final int MAX_STAIRCASE_NUMBER = 1;
    private static final double[] ENTRY_TIME = {10.0, 20.0, 30.0};
    private ArrayList<Staircase> staircasesList;

    public StaircaseManager(){
        staircasesList = new ArrayList<>();
    }

    public Staircase registerNewFloor(){
        if(staircasesList.size() < MAX_STAIRCASE_NUMBER){
            Staircase staircase = new Staircase();
            staircasesList.add(staircase);
            return staircase;
        }
        return null;
    }

    public void putClientInStaircase(int staircaseId, Client client){
        for(Staircase staircase : staircasesList){
            if(staircase.getStaircaseId() == staircaseId){
                staircase.putUserInStaircase(client);
            }
        }
    }
}
