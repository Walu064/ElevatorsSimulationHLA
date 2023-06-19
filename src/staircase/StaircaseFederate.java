package staircase;

import staircase.som.Staircase;
import staircase.som.StaircaseManager;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

public class StaircaseFederate {
    private final static double END_OF_SIM_TIME = 5000.0;
    private final static String FEDERATION_NAME = "ElevatorsSimulationFederation";
    public static final String READY_TO_RUN = "READY_TO_RUN";
    private static final String FOM_FILENAME = "elevatorSimulationFOM.fed";
    private RTIambassador rtiAmbassador;
    private StaircaseFederateAmbassador staircaseFederateAmbassador;

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
        staircaseFederateAmbassador = new StaircaseFederateAmbassador();

        // 3. Registration of federate model
        staircaseFederateAmbassador.staircaseManager = new StaircaseManager();
        rtiAmbassador.joinFederationExecution("StaircaseFederate", FEDERATION_NAME, staircaseFederateAmbassador);
        log( "Joined Federation as StaircaseFederate");

        // 4. Federation sync register:
        rtiAmbassador.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        while(!staircaseFederateAmbassador.isAnnounced) {
            rtiAmbassador.tick();
        }

        waitForUser();

        // 5. Federate sync:
        rtiAmbassador.synchronizationPointAchieved(READY_TO_RUN);
        log( "Achieved sync point: " + READY_TO_RUN + ", waiting for federation..." );
        while(!staircaseFederateAmbassador.isReadyToRun) {
            rtiAmbassador.tick();
        }

        enableTimePolicy();

        publishAndSubscribe();

        // 6. Federate main loop:
        while (staircaseFederateAmbassador.running) {
            advanceTime();
            // Simulation end condition
            if(staircaseFederateAmbassador.federateTime > END_OF_SIM_TIME) {
                break;
            }

            // Interactions published by Elevator:
            Staircase floor = staircaseFederateAmbassador.staircaseManager.registerNewFloor();
            if(floor != null){
                publishRegisterNewStaircase(floor.getStaircaseId());
            }

            rtiAmbassador.tick();
        }
    }

    private static void log(String message) {
        System.out.println("StaircaseFederate: " + message);
    }

    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = new DoubleTime(staircaseFederateAmbassador.federateTime);
        LogicalTimeInterval lookahead = new DoubleTimeInterval(staircaseFederateAmbassador.federateLookahead);

        this.rtiAmbassador.enableTimeRegulation(currentTime, lookahead);

        while(!staircaseFederateAmbassador.isRegulating) {
            rtiAmbassador.tick();
        }

        this.rtiAmbassador.enableTimeConstrained();

        while(!staircaseFederateAmbassador.isConstrained) {
            rtiAmbassador.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        int newStaircaseRegister = rtiAmbassador.getInteractionClassHandle("InteractionRoot.NewStaircaseRegister");
        staircaseFederateAmbassador.NEW_STAIRCASE_REGISTER_HANDLE = newStaircaseRegister;
        rtiAmbassador.publishInteractionClass(newStaircaseRegister);

        /* SUBSCRIBE: SendClientToDestinationFloorByStaircase */
        int sendClientToDestinationFloorByStaircase = rtiAmbassador.getInteractionClassHandle("InteractionRoot.SendClientToDestinationFloorByStaircase");
        staircaseFederateAmbassador.SEND_CLIENT_TO_DEST_FLOOR_STAIRCASE_HANDLE = sendClientToDestinationFloorByStaircase;
        rtiAmbassador.subscribeInteractionClass(sendClientToDestinationFloorByStaircase);
    }

    private void advanceTime() throws RTIexception {

        log("Time advance :: Request :: Step : " + 10.0);

        staircaseFederateAmbassador.isAdvancing = true;
        LogicalTime newTime = new DoubleTime(staircaseFederateAmbassador.federateTime + 10.0);
        rtiAmbassador.timeAdvanceRequest(newTime);

        while (staircaseFederateAmbassador.isAdvancing) {
            rtiAmbassador.tick();
        }

        log("Time advance :: Granted :: Time : " + staircaseFederateAmbassador.federateTime);
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

    private void publishRegisterNewStaircase(int staircaseId) throws RTIexception{
        int newStaircaseRegisterHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.NewStaircaseRegister");

        SuppliedParameters params = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int elevatorIdHandle = rtiAmbassador.getParameterHandle("staircaseId", newStaircaseRegisterHandle);
        byte[] elevatorIdValue = EncodingHelpers.encodeInt(staircaseId);
        params.add(elevatorIdHandle, elevatorIdValue);

        LogicalTime time = new DoubleTime(staircaseFederateAmbassador.federateTime + staircaseFederateAmbassador.federateLookahead);

        rtiAmbassador.sendInteraction(newStaircaseRegisterHandle, params, "tag".getBytes(), time);

        log("New Staircase Register | " + " Staircase ID: " + staircaseId + " | Time: " + time);
    }

    public static void main(String[] args) {
        try {
            new StaircaseFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

}
