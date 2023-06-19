package building;

import building.som.Building;
import clients.som.Client;
import elevators.som.Elevator;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class BuildingFederate {
    private static final double END_OF_SIM_TIME = 5000.0;
    private static final String FEDERATION_NAME = "ElevatorsSimulationFederation";
    public static final String READY_TO_RUN = "READY_TO_RUN";
    private static final String FOM_FILENAME = "elevatorSimulationFOM.fed";
    private static final String STATS_FILENAME = "stats.csv";
    protected int totalStaircaseUsers = 0;
    private RTIambassador rtiAmbassador;
    private BuildingFederateAmbassador buildingFederateAmbassador;

    public void runFederate() throws RTIexception, IOException {
        // 1. Try to create federation (if exist - exception)
        rtiAmbassador = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();

        try {
            File fom = new File(FOM_FILENAME);
            rtiAmbassador.createFederationExecution( FEDERATION_NAME,
                    fom.toURI().toURL() );
            log( "Federation created." );
        }
        catch(FederationExecutionAlreadyExists exists) {
            log( "Cannot create federation :: already exists" );
        }
        catch(MalformedURLException urlEx) {
            log("Exception processing fom: " + urlEx.getMessage());
            urlEx.printStackTrace();
            return;
        }

        // 2. Join the federation as current Federate
        buildingFederateAmbassador = new BuildingFederateAmbassador();

        // 3. Registration of federate model
        buildingFederateAmbassador.building = new Building();
        rtiAmbassador.joinFederationExecution("BuildingFederate", FEDERATION_NAME, buildingFederateAmbassador);
        log( "Joined Federation as BuildingFederate");

        // 4. Federation sync register:
        rtiAmbassador.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        while(!buildingFederateAmbassador.isAnnounced) {
            rtiAmbassador.tick();
        }

        waitForUser();

        // 5. Federate sync:
        rtiAmbassador.synchronizationPointAchieved(READY_TO_RUN);
        log( "Achieved sync point: " + READY_TO_RUN + ", waiting for federation..." );
        while(!buildingFederateAmbassador.isReadyToRun) {
            rtiAmbassador.tick();
        }

        enableTimePolicy();

        publishAndSubscribe();

        // 6. Federate main loop:
        while (buildingFederateAmbassador.running) {
            double TIME_STEP = 10.0;
            advanceTime(TIME_STEP);
            // Simulation end condition
            if(buildingFederateAmbassador.federateTime > END_OF_SIM_TIME) {
                break;
            }

            /* INTERACTIONS PUBLISHED BY BUILDING FEDERATE */
            // SendClientToDestinationFloor:
            ArrayList<Client> clientsList = buildingFederateAmbassador.building.getClientsList();
            if(!clientsList.isEmpty()){
                Client client = clientsList.remove(0);
                sendClientToDestinationFloor(client);
            }

            rtiAmbassador.tick();
        }

        // 7 Open the stats.csv file and create csvPrinter object:
        FileReader fileReader = new FileReader(STATS_FILENAME);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        if(bufferedReader.readLine() == null){
            FileWriter fileWriter = new FileWriter(STATS_FILENAME, true);
            CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);

            double currentSimulationDay = 1.0;
            csvPrinter.printRecord(currentSimulationDay, buildingFederateAmbassador.totalClientsInBuilding, totalStaircaseUsers);

            fileWriter.close();
            csvPrinter.close();

            System.out.println("\n");
            System.out.println("--------------------------------------------");
            System.out.println("------------------ STATS -------------------");
            System.out.println("--------------------------------------------");
            System.out.println("DAY: "+currentSimulationDay);
            System.out.println("TOTAL CLIENTS TODAY:\t\t\t\t\t" + buildingFederateAmbassador.totalClientsInBuilding);
            System.out.println("TOTAL CLIENTS USING STAIRCASE TODAY:\t" + totalStaircaseUsers);

            fileReader.close();
            bufferedReader.close();

        }else{

            FileReader fileReader1 = new FileReader(STATS_FILENAME);
            CSVParser csvParser1 = new CSVParser(fileReader1, CSVFormat.DEFAULT);

            double lastSimulationDay = 0.0;
            for(CSVRecord csvRecord : csvParser1){
                lastSimulationDay = Double.parseDouble(csvRecord.get(0));
            }

            csvParser1.close();
            fileReader1.close();

            double currentSimulationDay = lastSimulationDay + 1.0;

            FileWriter fileWriter = new FileWriter(STATS_FILENAME, true);
            CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);

            csvPrinter.printRecord(String.valueOf(currentSimulationDay), String.valueOf(buildingFederateAmbassador.totalClientsInBuilding),
                    String.valueOf(totalStaircaseUsers));

            fileWriter.close();
            csvPrinter.close();

            double totalEverStaircaseUsers = 0.0;
            double totalEverClientsNumber = 0.0;

            FileReader fileReader2 = new FileReader(STATS_FILENAME);
            CSVParser csvParser2 = new CSVParser(fileReader2, CSVFormat.DEFAULT);

            for(CSVRecord csvRecord : csvParser2){
                totalEverStaircaseUsers += Double.parseDouble(csvRecord.get(1));
                totalEverClientsNumber += Double.parseDouble(csvRecord.get(2));
            }

            csvParser2.close();
            fileReader2.close();

            double dailyMeanOfClientsNumber = 0.0;
            double dailyMeanOfStaircaseUsers = 0.0;
            dailyMeanOfClientsNumber = totalEverClientsNumber / currentSimulationDay;
            dailyMeanOfStaircaseUsers = totalEverStaircaseUsers / currentSimulationDay;

            System.out.println("\n");
            System.out.println("--------------------------------------------");
            System.out.println("------------------ STATS -------------------");
            System.out.println("--------------------------------------------");
            System.out.println("DAY: "+currentSimulationDay);
            System.out.println("TOTAL CLIENTS TODAY:\t\t\t\t\t" + buildingFederateAmbassador.totalClientsInBuilding);
            System.out.println("TOTAL CLIENTS USING STAIRCASE TODAY:\t" + totalStaircaseUsers);
            System.out.println("DAILY MEAN OF USERS:\t\t\t\t\t" + dailyMeanOfClientsNumber);
            System.out.println("DAILY MEAN OF STAIRCASE USERS:\t\t\t" + dailyMeanOfStaircaseUsers);
            System.out.println("\n");
            System.out.println("Detailed statistics from all simulations are in the stats.csv file.");
        }
    }

    private static void log(String message) {
        System.out.println("BuildingFederate: " + message);
    }

    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = new DoubleTime(buildingFederateAmbassador.federateTime);
        LogicalTimeInterval lookahead = new DoubleTimeInterval(buildingFederateAmbassador.federateLookahead);

        this.rtiAmbassador.enableTimeRegulation(currentTime, lookahead);

        while(!buildingFederateAmbassador.isRegulating) {
            rtiAmbassador.tick();
        }

        this.rtiAmbassador.enableTimeConstrained();

        while(!buildingFederateAmbassador.isConstrained) {
            rtiAmbassador.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        /* SUBSCRIBE: NewElevatorRegister, NewStaircaseRegister,
                      NewClientRegister */
        int newElevatorRegister = rtiAmbassador.getInteractionClassHandle("InteractionRoot.NewElevatorRegister");
        buildingFederateAmbassador.NEW_ELEVATOR_REGISTER_HANDLE = newElevatorRegister;
        rtiAmbassador.subscribeInteractionClass(newElevatorRegister);

        int newStaircaseRegister = rtiAmbassador.getInteractionClassHandle("InteractionRoot.NewStaircaseRegister");
        buildingFederateAmbassador.NEW_STAIRCASE_REGISTER_HANDLE = newStaircaseRegister;
        rtiAmbassador.subscribeInteractionClass(newStaircaseRegister);

        int newClientRegister = rtiAmbassador.getInteractionClassHandle("InteractionRoot.NewClientRegister");
        buildingFederateAmbassador.NEW_CLIENT_REGISTER_HANDLE = newClientRegister;
        rtiAmbassador.subscribeInteractionClass(newClientRegister);

        /* PUBLISH: SendClientToDestinationFloorByElevator,
                    SendClientToDestinationFloorByStaircase */
        int sendClientToDestinationFloorByElevator = rtiAmbassador.getInteractionClassHandle("InteractionRoot.SendClientToDestinationFloorByElevator");
        buildingFederateAmbassador.SEND_CLIENT_TO_DEST_FLOOR_ELEVATOR_HANDLE = sendClientToDestinationFloorByElevator;
        rtiAmbassador.publishInteractionClass(sendClientToDestinationFloorByElevator);

        int sendClientToDestinationFloorByStaircase = rtiAmbassador.getInteractionClassHandle("InteractionRoot.SendClientToDestinationFloorByStaircase");
        buildingFederateAmbassador.SEND_CLIENT_TO_DEST_FLOOR_STAIRCASE_HANDLE = sendClientToDestinationFloorByStaircase;
        rtiAmbassador.publishInteractionClass(sendClientToDestinationFloorByStaircase);

        /* SUBSCRIBE: EntryRightFloorFromElevator,
                      EntryRightFloorFromStaircase */
        int entryRightFloorFromElevator = rtiAmbassador.getInteractionClassHandle("InteractionRoot.EntryDestinationFloorFromElevator");
        buildingFederateAmbassador.ENTRY_DEST_FLOOR_ELEVATOR_HANDLE = entryRightFloorFromElevator;
        rtiAmbassador.subscribeInteractionClass(entryRightFloorFromElevator);

        int entryRightFloorFromStaircase = rtiAmbassador.getInteractionClassHandle("InteractionRoot.EntryDestinationFloorFromStaircase");
        buildingFederateAmbassador.ENTRY_DEST_FLOOR_STAIRCASE_HANDLE = entryRightFloorFromStaircase;
        rtiAmbassador.subscribeInteractionClass(entryRightFloorFromStaircase);
    }

    private void advanceTime(double step) throws RTIexception {

        log("Time advance :: Request :: Step : " + step);

        buildingFederateAmbassador.isAdvancing = true;
        LogicalTime newTime = new DoubleTime(buildingFederateAmbassador.federateTime + step);
        rtiAmbassador.timeAdvanceRequest(newTime);

        while (buildingFederateAmbassador.isAdvancing) {
            rtiAmbassador.tick();
        }

        log("Time advance :: Granted :: Time : " + buildingFederateAmbassador.federateTime);
    }

    private void waitForUser() {
        log("Waiting for user interaction: Press ENTER to continue..." );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try {
            reader.readLine();
        }
        catch( Exception e ) {
            log("Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private void sendClientToDestinationFloor(Client client) throws RTIexception{
        // Client is choosing destination floor
        client.setDestinationFloor();

        // Check or elevator is busy:
        ArrayList<Elevator> elevatorsList = buildingFederateAmbassador.building.getElevatorsList();
        for(Elevator elevator : elevatorsList){
            if(!elevator.isBusy()){
                // Sending client to destination floor by elevator
                int sendClientToDestinationFloorHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.SendClientToDestinationFloorByElevator");
                SuppliedParameters params = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

                int clientIdHandle = rtiAmbassador.getParameterHandle("clientId", sendClientToDestinationFloorHandle);
                byte[] clientIdValue = EncodingHelpers.encodeInt(client.getClientId());
                params.add(clientIdHandle, clientIdValue);

                int clientDestinationFloorHandle = rtiAmbassador.getParameterHandle("destinationFloor", sendClientToDestinationFloorHandle);
                byte[] clientDestinationFloorValue = EncodingHelpers.encodeInt(client.getDestinationFloor());
                params.add(clientDestinationFloorHandle, clientDestinationFloorValue);

                int clientChosenElevatorHandle = rtiAmbassador.getParameterHandle("chosenElevatorId", sendClientToDestinationFloorHandle);
                byte[] clientChosenElevatorValue = EncodingHelpers.encodeInt(elevator.getElevatorId());
                params.add(clientChosenElevatorHandle, clientChosenElevatorValue);

                LogicalTime time = new DoubleTime(buildingFederateAmbassador.federateTime + buildingFederateAmbassador.federateLookahead);
                rtiAmbassador.sendInteraction(sendClientToDestinationFloorHandle, params, "tag".getBytes(), time);
                log("Client sent to floor "+client.getDestinationFloor()+" by elevator "+elevator.getElevatorId()+" | " + "Client ID: " + client.getClientId() + " | Time: " + time);
                elevator.setIsBusy(true);

                return;
            }
        }

        //Sending client to destination floor by staircase
        int sendClientToDestinationFloorHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.SendClientToDestinationFloorByStaircase");
        SuppliedParameters params = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int clientIdHandle = rtiAmbassador.getParameterHandle("clientId", sendClientToDestinationFloorHandle);
        byte[] clientIdValue = EncodingHelpers.encodeInt(client.getClientId());
        params.add(clientIdHandle, clientIdValue);

        int clientDestinationFloorHandle = rtiAmbassador.getParameterHandle("destinationFloor", sendClientToDestinationFloorHandle);
        byte[] clientDestinationFloorValue = EncodingHelpers.encodeInt(client.getDestinationFloor());
        params.add(clientDestinationFloorHandle, clientDestinationFloorValue);

        LogicalTime time = new DoubleTime(buildingFederateAmbassador.federateTime + buildingFederateAmbassador.federateLookahead);
        rtiAmbassador.sendInteraction(sendClientToDestinationFloorHandle, params, "tag".getBytes(), time);
        log("Client sent to floor "+client.getDestinationFloor()+" by staircase | " + " Client ID: " + client.getClientId() + " | Time: " + time);
        totalStaircaseUsers++;
    }

    public static void main(String[] args) {
        try {
            new BuildingFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}