package clients;

import clients.som.Client;
import clients.som.ClientsManager;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Random;

public class ClientsFederate {
    private final static double END_OF_SIM_TIME = 5000.0;
    private final static String FEDERATION_NAME = "ElevatorsSimulationFederation";
    public static final String READY_TO_RUN = "READY_TO_RUN";
    private static final String FOM_FILENAME = "elevatorSimulationFOM.fed";
    private RTIambassador rtiAmbassador;
    private ClientsFederateAmbassador clientsFederateAmbassador;

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
        clientsFederateAmbassador = new ClientsFederateAmbassador();

        // 3. Registration of federate model
        clientsFederateAmbassador.clientsManager = new ClientsManager();
        rtiAmbassador.joinFederationExecution("ClientsFederate", FEDERATION_NAME, clientsFederateAmbassador);
        log("Joined Federation as ClientsFederate");

        // 4. Federation sync register:
        rtiAmbassador.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        while(!clientsFederateAmbassador.isAnnounced) {
            rtiAmbassador.tick();
        }

        waitForUser();

        // 5. Federate sync:
        rtiAmbassador.synchronizationPointAchieved(READY_TO_RUN);
        log( "Achieved sync point: " + READY_TO_RUN + ", waiting for federation..." );
        while(!clientsFederateAmbassador.isReadyToRun) {
            rtiAmbassador.tick();
        }

        enableTimePolicy();

        publishAndSubscribe();

        // 6. Federate main loop:
        while (clientsFederateAmbassador.running) {
            advanceTime();
            // Simulation end condition
            if(clientsFederateAmbassador.federateTime > END_OF_SIM_TIME) {
                break;
            }

            // Interactions published by Elevator:
            Random random = new Random();
            int randomTimeFactor = random.nextInt(5)+1;
            Client client = clientsFederateAmbassador.clientsManager.registerNewClient();
            if(client != null){
                publishRegisterNewClient(client.getClientId(), randomTimeFactor);
            }

            rtiAmbassador.tick();
        }
    }

    private static void log(String message) {
        System.out.println("ClientsFederate: " + message);
    }

    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = new DoubleTime(clientsFederateAmbassador.federateTime);
        LogicalTimeInterval lookahead = new DoubleTimeInterval(clientsFederateAmbassador.federateLookahead);

        this.rtiAmbassador.enableTimeRegulation(currentTime, lookahead);

        while(!clientsFederateAmbassador.isRegulating) {
            rtiAmbassador.tick();
        }

        this.rtiAmbassador.enableTimeConstrained();

        while(!clientsFederateAmbassador.isConstrained) {
            rtiAmbassador.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        int newClientRegister = rtiAmbassador.getInteractionClassHandle("InteractionRoot.NewClientRegister");
        clientsFederateAmbassador.NEW_CLIENT_HANDLE = newClientRegister;
        rtiAmbassador.publishInteractionClass(newClientRegister);

        // Next interactions soon...
    }

    private void advanceTime() throws RTIexception {

        log("Time advance :: Request :: Step : " + 10.0);

        clientsFederateAmbassador.isAdvancing = true;
        LogicalTime newTime = new DoubleTime(clientsFederateAmbassador.federateTime + 10.0);
        rtiAmbassador.timeAdvanceRequest(newTime);

        while (clientsFederateAmbassador.isAdvancing) {
            rtiAmbassador.tick();
        }

        log("Time advance :: Granted :: Time : " + clientsFederateAmbassador.federateTime);
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

    // Methods performing interaction handle:
    private void publishRegisterNewClient(int clientId, int randomTimeFactor) throws RTIexception{
        int newClientRegisterHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.NewClientRegister");

        SuppliedParameters params = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int clientIdHandle = rtiAmbassador.getParameterHandle("clientId", newClientRegisterHandle);
        byte[] clientIdValue = EncodingHelpers.encodeInt(clientId);
        params.add(clientIdHandle, clientIdValue);

        LogicalTime time = new DoubleTime(clientsFederateAmbassador.federateTime + (clientsFederateAmbassador.federateLookahead * randomTimeFactor));

        rtiAmbassador.sendInteraction(newClientRegisterHandle, params, "tag".getBytes(), time);

        log("New Client Register | " + " Client ID: " + clientId + " | Time: " + time);
    }


    public static void main(String[] args) {
        try {
            new ClientsFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

}
