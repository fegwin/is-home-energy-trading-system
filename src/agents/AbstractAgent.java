package agents;

import agents.interfaces.Destroyable;
import agents.interfaces.Observable;
import jade.core.Agent;
import ui.interfaces.Informable;

import java.util.Vector;

/**
 * Created by fegwin on 15/09/2016.
 */
public abstract class AbstractAgent extends Agent implements Observable, Destroyable {
    private Vector<Informable> statusEventListeners;

    public void addStatusEventListener(Informable listener) {
        if(statusEventListeners == null) {
            statusEventListeners = new Vector();
        }

        statusEventListeners.add(listener);
    }

    public void fireStatusChangedEvent(Object newStatus) {
        if(statusEventListeners == null || statusEventListeners.isEmpty()) return;

        for(Informable listener : statusEventListeners) {
            listener.inform(newStatus);
        }
    }

    public AbstractAgent() {
        registerO2AInterface(Observable.class, this);
        registerO2AInterface(Destroyable.class, this);
    }

    @Override
    public void destroy() {
        System.out.println(this.getLocalName() + " (" + this.getClass().getName() + ") - STOPPING");
        doDelete();
    }

    @Override
    protected void setup() {
        super.setup();

        configureBehaviours();

        System.out.println(this.getLocalName() + " (" + this.getClass().getName() + ") - STARTING");
    }

    protected abstract void configureBehaviours();
}
