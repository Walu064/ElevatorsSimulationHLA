package elevators;

import clients.som.Client;
import elevators.som.Elevator;
import elevators.som.ElevatorsManager;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Random;

public class ElevatorsFederate {
    private final static double END_OF_SIM_TIME = 5000.0;
    private final static String FEDERATION_NAME = "ElevatorsSimulationFederation";
    public static final String READY_TO_RUN = "READY_TO_RUN";
    private static final String FOM_FILENAME = "elevatorSimulationFOM.fed";
    private RTIambassador rtiAmbassador;
    private ElevatorsFederateAmbassador elevatorsFederateAmbassador;

    public void runFederate() throws RTIexception {
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
        elevatorsFederateAmbassador = new ElevatorsFederateAmbassador();

        // 3. Registration of federate model
        elevatorsFederateAmbassador.elevatorsManager = new ElevatorsManager();
        rtiAmbassador.joinFederationExecution("ElevatorsFederate", FEDERATION_NAME, elevatorsFederateAmbassador);
        log( "Joined Federation as ElevatorsFederate");

        // 4. Federation sync register:
        rtiAmbassador.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        while(!elevatorsFederateAmbassador.isAnnounced) {
            rtiAmbassador.tick();
        }

        waitForUser();

        // 5. Federate sync:
        rtiAmbassador.synchronizationPointAchieved(READY_TO_RUN);
        log( "Achieved sync point: " + READY_TO_RUN + ", waiting for federation..." );
        while(!elevatorsFederateAmbassador.isReadyToRun) {
            rtiAmbassador.tick();
        }

        enableTimePolicy();

        publishAndSubscribe();

        // 6. Federate main loop:
        while (elevatorsFederateAmbassador.running) {
            advanceTime();
            // Simulation end condition
            if(elevatorsFederateAmbassador.federateTime > END_OF_SIM_TIME) {
                break;
            }

            // INTERACTION: NewElevatorRegister
            Elevator newElevator = elevatorsFederateAmbassador.elevatorsManager.registerNewElevator();
            if(newElevator != null){
                publishRegisterNewElevator(newElevator.getElevatorId());
            }

            // INTERACTION: EntryDestinationFloorFromElevator
            ArrayList<Elevator> elevatorsList = elevatorsFederateAmbassador.elevatorsManager.getElevatorsList();
            if(!elevatorsList.isEmpty()){
                for(Elevator elevator : elevatorsList){
                    if(elevator.isBusy()){
                        if(!elevator.getElevatorPassengers().isEmpty()){
                            Client client = elevator.getElevatorPassengers().get(0);
                            elevator.setIsBusy(false);
                            publishEntryDestinationFloorFromElevator(client, elevator);
                        }
                    }
                }
            }

            rtiAmbassador.tick();
        }
    }

    private static void log(String message) {
        System.out.println("ElevatorsFederate: " + message);
    }

    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = new DoubleTime(elevatorsFederateAmbassador.federateTime);
        LogicalTimeInterval lookahead = new DoubleTimeInterval(elevatorsFederateAmbassador.federateLookahead);

        this.rtiAmbassador.enableTimeRegulation(currentTime, lookahead);

        while(!elevatorsFederateAmbassador.isRegulating) {
            rtiAmbassador.tick();
        }

        this.rtiAmbassador.enableTimeConstrained();

        while(!elevatorsFederateAmbassador.isConstrained) {
            rtiAmbassador.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        /* PUBLISH: NewElevatorRegister */
        int newElevatorRegister = rtiAmbassador.getInteractionClassHandle("InteractionRoot.NewElevatorRegister");
        elevatorsFederateAmbassador.NEW_ELEVATOR_REGISTER_HANDLE = newElevatorRegister;
        rtiAmbassador.publishInteractionClass(newElevatorRegister);

        /* SUBSCRIBE: SendClientToDestinationFloorByElevator */
        int sendClientToDestinationFloorByElevator = rtiAmbassador.getInteractionClassHandle("InteractionRoot.SendClientToDestinationFloorByElevator");
        elevatorsFederateAmbassador.SEND_CLIENT_TO_DEST_FLOOR_ELEVATOR_HANDLE = sendClientToDestinationFloorByElevator;
        rtiAmbassador.subscribeInteractionClass(sendClientToDestinationFloorByElevator);

        /* PUBLISH: EntryDestinationFloorFromElevator */
        int entryDestinationFloorFromElevator = rtiAmbassador.getInteractionClassHandle("InteractionRoot.EntryDestinationFloorFromElevator");
        elevatorsFederateAmbassador.ENTRY_DEST_FLOOR_ELEVATOR_HANDLE = entryDestinationFloorFromElevator;
        rtiAmbassador.publishInteractionClass(entryDestinationFloorFromElevator);
    }

    private void advanceTime() throws RTIexception {

        log("Time advance :: Request :: Step : " + 10.0);

        elevatorsFederateAmbassador.isAdvancing = true;
        LogicalTime newTime = new DoubleTime(elevatorsFederateAmbassador.federateTime + 10.0);
        rtiAmbassador.timeAdvanceRequest(newTime);

        while (elevatorsFederateAmbassador.isAdvancing) {
            rtiAmbassador.tick();
        }

        log("Time advance :: Granted :: Time : " + elevatorsFederateAmbassador.federateTime);
    }

    private void waitForUser() {
        log( " Waiting for user :: Press ENTER to continue..." );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try {
            reader.readLine();
        }
        catch( Exception e ) {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private void publishRegisterNewElevator(int elevatorId) throws RTIexception{
        int newElevatorRegisterHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.NewElevatorRegister");

        SuppliedParameters params = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int elevatorIdHandle = rtiAmbassador.getParameterHandle("elevatorId", newElevatorRegisterHandle);
        byte[] elevatorIdValue = EncodingHelpers.encodeInt(elevatorId);
        params.add(elevatorIdHandle, elevatorIdValue);

        LogicalTime time = new DoubleTime(elevatorsFederateAmbassador.federateTime + elevatorsFederateAmbassador.federateLookahead);

        rtiAmbassador.sendInteraction(newElevatorRegisterHandle, params, "tag".getBytes(), time);

        log("New Elevator Register | " + " Elevator ID: " + elevatorId + " | Time: " + time);
    }

    private void publishEntryDestinationFloorFromElevator(Client client, Elevator elevator) throws RTIexception{
        Random random = new Random();

        int entryDestinationFloorFromElevatorHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.EntryDestinationFloorFromElevator");

        SuppliedParameters params = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int clientIdHandle = rtiAmbassador.getParameterHandle("clientId", entryDestinationFloorFromElevatorHandle);
        byte[] clientIdValue = EncodingHelpers.encodeInt(client.getClientId());
        params.add(clientIdHandle, clientIdValue);

        int destinationFloorHandle = rtiAmbassador.getParameterHandle("destinationFloor", entryDestinationFloorFromElevatorHandle);
        byte[] destinationFloorValue = EncodingHelpers.encodeInt(client.getDestinationFloor());
        params.add(destinationFloorHandle, destinationFloorValue);

        int elevatorIdHandle = rtiAmbassador.getParameterHandle("elevatorId", entryDestinationFloorFromElevatorHandle);
        byte[] elevatorIdValue = EncodingHelpers.encodeInt(elevator.getElevatorId());
        params.add(elevatorIdHandle, elevatorIdValue);

        int randomTimeFactor = random.nextInt(2) + 2;
        LogicalTime time = new DoubleTime(elevatorsFederateAmbassador.federateTime + (elevatorsFederateAmbassador.federateLookahead * randomTimeFactor));

        rtiAmbassador.sendInteraction(entryDestinationFloorFromElevatorHandle, params, "tag".getBytes(), time);

        log("Client left elevator" + elevator.getElevatorId() +" at floor  "+client.getDestinationFloor()+" | Client ID: "+client.getClientId()+" | time: " + time);
    }


    public static void main(String[] args) {
        try {
            new ElevatorsFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

}
