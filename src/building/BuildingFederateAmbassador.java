package building;

import building.som.Building;
import clients.som.Client;
import elevators.som.Elevator;
import staircase.som.Staircase;
import hla.rti.*;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;
import org.portico.impl.hla13.types.DoubleTime;

public class BuildingFederateAmbassador extends NullFederateAmbassador {
    private final static String COMPONENT_NAME = "BuildingFederateAmbassador";
    protected int NEW_ELEVATOR_REGISTER_HANDLE = 0;
    protected int NEW_STAIRCASE_REGISTER_HANDLE = 0;
    protected int NEW_CLIENT_REGISTER_HANDLE = 0;
    protected int SEND_CLIENT_TO_DEST_FLOOR_ELEVATOR_HANDLE = 0;
    protected int SEND_CLIENT_TO_DEST_FLOOR_STAIRCASE_HANDLE = 0;
    protected int ENTRY_DEST_FLOOR_ELEVATOR_HANDLE = 0;
    protected int ENTRY_DEST_FLOOR_STAIRCASE_HANDLE = 0;
    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;
    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;
    protected boolean running = true;
    protected Building building = null;
    protected double federateTime = 0.0;
    protected double federateLookahead   = 10.0;
    protected int totalClientsInBuilding = 0;

    public void receiveInteraction( int interactionClass, ReceivedInteraction theInteraction, byte[] tag ) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction( int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle ) {

        StringBuilder builder = new StringBuilder("Interaction Received -> ");
        if (interactionClass == NEW_ELEVATOR_REGISTER_HANDLE) {
            try {
                int newElevatorId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                builder.append("New Elevator Register | Elevator ID: ").append(newElevatorId);
                building.addElevatorRegister(new Elevator(newElevatorId));
            } catch (ArrayIndexOutOfBounds exception) {
                throw new RuntimeException(exception);
            }
        }

        if (interactionClass == NEW_STAIRCASE_REGISTER_HANDLE) {
            try {
                int newStaircaseId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                builder.append("New Staircase Register | Staircase ID: ").append(newStaircaseId);
                building.addStaircaseRegister(new Staircase(newStaircaseId));
            } catch (ArrayIndexOutOfBounds exception) {
                throw new RuntimeException(exception);
            }
        }

        if (interactionClass == NEW_CLIENT_REGISTER_HANDLE) {
            try {
                int newClientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                builder.append("New Client Register | Client ID: ").append(newClientId);
                building.addClientRegister(new Client(newClientId));
                totalClientsInBuilding++;
            } catch (ArrayIndexOutOfBounds exception) {
                throw new RuntimeException(exception);
            }
        }

        if (interactionClass == ENTRY_DEST_FLOOR_ELEVATOR_HANDLE) {
            try {
                int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                int destinationFloor = EncodingHelpers.decodeInt(theInteraction.getValue(1));
                int elevatorId = EncodingHelpers.decodeInt(theInteraction.getValue(2));
                builder.append("Client has reached dest. floor | Client ID: ").append(clientId).append(" | Floor: ").append(destinationFloor).append("\nElevator ").append(elevatorId).append(" is free now.");
                for(Elevator elevator : building.getElevatorsList()){
                    if(elevator.getElevatorId() == elevatorId){
                        elevator.setIsBusy(false);
                    }
                }
                building.getClientsList().removeIf(client -> client.getClientId() == clientId);
            } catch (ArrayIndexOutOfBounds exception) {
                throw new RuntimeException(exception);
            }
        }

        log(builder.toString());
    }

    private static void log(String message) {
        System.out.println(COMPONENT_NAME + " : " + message);
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