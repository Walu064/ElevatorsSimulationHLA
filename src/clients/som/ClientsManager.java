package clients.som;

import java.util.ArrayList;

public class ClientsManager {

    private ArrayList<Client> clientsList;

    public ClientsManager(){
        clientsList = new ArrayList<>();
    }

    public Client registerNewClient(){
        Client client = new Client();
        this.clientsList.add(client);
        return client;
    }


}
