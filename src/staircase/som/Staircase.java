package staircase.som;

import clients.som.Client;

import java.awt.*;
import java.util.ArrayList;

public class Staircase {
    private static int STAIRCASE_COUNTER = 0;
    private final int staircaseId;
    private ArrayList<Client> staircaseUsers;
    private static int STAIRCASE_USERS_COUNTER;

    public Staircase(){
        this.staircaseId = STAIRCASE_COUNTER++;
        this.staircaseUsers = new ArrayList<>();
    }

    public Staircase(int staircaseId){
        this.staircaseId = staircaseId;
    }

    public int getStaircaseId() {
        return staircaseId;
    }


    public void putUserInStaircase(Client client){
        staircaseUsers.add(client);
        STAIRCASE_USERS_COUNTER = STAIRCASE_USERS_COUNTER + 1;
    }

    public static int getStaircaseCounter(){
        return STAIRCASE_USERS_COUNTER;
    }
}