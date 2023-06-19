package staircase;

import building.BuildingFederate;
import clients.som.Client;
import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.jlc.EncodingHelpers;
import staircase.som.StaircaseManager;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.NullFederateAmbassador;
import org.portico.impl.hla13.types.DoubleTime;

public class StaircaseFederateAmbassador extends NullFederateAmbassador {
    protected int NEW_STAIRCASE_REGISTER_HANDLE = 0;
    protected int SEND_CLIENT_TO_DEST_FLOOR_STAIRCASE_HANDLE = 0;
    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;
    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;
    protected boolean running = true;
    protected double federateTime = 0.0;
    protected double federateLookahead   = 10.0;
    protected StaircaseManager staircaseManager = null;

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag ) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle ) {
        StringBuilder builder = new StringBuilder( "Interaction Received -> " );

        if (interactionClass == SEND_CLIENT_TO_DEST_FLOOR_STAIRCASE_HANDLE) {
            try {
                int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                int destinationFloor = EncodingHelpers.decodeInt(theInteraction.getValue(1));

                builder.append("Client in Staircase | Client ID: ").append(clientId).append(" | Dest. floor: ").append(destinationFloor);
                Client client = new Client(clientId, destinationFloor);
                staircaseManager.putClientInStaircase(0, client);

            } catch (ArrayIndexOutOfBounds exception) {
                throw new RuntimeException(exception);
            }
        }

        log(builder.toString());
    }

    private static void log(String message) {
        System.out.println("StaircaseFederateAmbassador" + " : " + message);
    }

    public void synchronizationPointRegistrationFailed( String label ) {
        log("Failed to register sync point: " + label);
    }

    public void synchronizationPointRegistrationSucceeded(String label) {
        log("Successfully registered sync point: " + label);
    }

    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("Sync point announced: " + label);
        if( label.equals(BuildingFederate.READY_TO_RUN) ) {
            this.isAnnounced = true;
        }
    }

    public void federationSynchronized(String label) {
        log("Federation Synchronized: " + label);
        if(label.equals(BuildingFederate.READY_TO_RUN)) {
            this.isReadyToRun = true;
        }
    }

    public void timeRegulationEnabled(LogicalTime theFederateTime) {
        this.federateTime = ((DoubleTime)theFederateTime).getTime();
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled(LogicalTime theFederateTime) {
        this.federateTime = ((DoubleTime)theFederateTime).getTime();
        this.isConstrained = true;
    }

    public void timeAdvanceGrant(LogicalTime theTime) {
        this.federateTime = ((DoubleTime)theTime).getTime();
        this.isAdvancing = false;
    }
}
