package agents;

import agents.models.ApplianceConsumption;
import agents.models.ApplianceConsumptionHistory;
import agents.models.Contract;
import agents.models.Proposal;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.core.behaviours.CyclicBehaviour;
import ui.containers.HomeStatusContainer;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.*

;/**
 * Created by fegwin on 7/09/2016.
 */
public class HomeAgent extends AbstractAgent {
    // Internal State Variables
    private Vector<String> retailers;
    private HashMap<String, ApplianceConsumptionHistory> applianceConsumptionHistory;

    private HashMap<String, ApplianceConsumption> currentApplianceConsumption;

    private Contract currentEnergyContract = null;
    private int ticksTillNextNegotiation = 0;

    private boolean inTheMiddleOfANegotiation = false;

    public HomeAgent() {
        retailers = new Vector();
        applianceConsumptionHistory = new HashMap();
        currentApplianceConsumption = new HashMap();
    }

    @Override
    public EnergyAgentType getAgentType() {
        return EnergyAgentType.HomeAgent;
    }

    protected void setup() {
        super.setup();

        // Grab the known retailers, from args
        Object[] args = getArguments();

        for(Object arg : args) {
            String retailer = (String)arg;
            retailers.add(retailer);
        }
    }

    /** Behaviours and Control Logic **/
    @Override
    protected void configureBehaviours() {
        addBehaviour(getReceiveHelloMessagesBehaviour());
    }

    @Override
    protected void appTickElapsed() {
//        // Negotiate/Predict all that jazz
//        if(ticksTillNextNegotiation <= 0) {
//            negotiateWithRetailers();
//        }
//
//        ticksTillNextNegotiation--;
        addBehaviour(getRecalculateAndUpdateBehaviour());
    }

    private void negotiateWithRetailers() {
        if(inTheMiddleOfANegotiation) return;

        addBehaviour(getRetailerNegotiationBehaviour());
    }

    private Behaviour getRetailerNegotiationBehaviour() {
        ACLMessage cfpMessage = new ACLMessage(ACLMessage.CFP);

        // Add cfp recipients
        for(String retailer : retailers) {
            cfpMessage.addReceiver(new AID(retailer, AID.ISLOCALNAME));
        }

        cfpMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        cfpMessage.setReplyByDate(new Date(System.currentTimeMillis() + APP_TICK));

        inTheMiddleOfANegotiation = true;

        return new ContractNetInitiator(this, cfpMessage) {
            protected void handlePropose(ACLMessage propose, Vector v) { }
            protected void handleRefuse(ACLMessage refuse) { }
            protected void handleFailure(ACLMessage failure) { }
            protected void handleAllResponses(Vector responses, Vector acceptances) {
                // Evaluate proposals.
                Enumeration e = responses.elements();

                // Get a handle not all proposals
                Vector<ACLMessage> proposalMessages = new Vector();
                while (e.hasMoreElements()) {
                    ACLMessage msg = (ACLMessage) e.nextElement();
                    if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        proposalMessages.add(msg);
                    }
                }

                // Extract Contract proposals from messages
                Vector<Contract> proposedContracts = new Vector();
                for(ACLMessage msg : proposalMessages)
                {
                    String compoundProposalString = msg.getContent();
                    Vector<Proposal> proposalsFromAgent = Proposal.fromCompoundString(compoundProposalString);

                    for(Proposal p : proposalsFromAgent)
                    {
                        Contract c = new Contract(msg, p);

                        proposedContracts.add(c);
                    }
                }

                // Find proposal with lowest predicted cost/hour
                Contract bestProposal = null;
                for(Contract c : proposedContracts)
                {
                    float predicted_kwh = predictKWHForNextNHours(c.duration);
                    c.predictedExpenditurePerHour = predicted_kwh / (c.duration);
                    if(bestProposal == null || c.predictedExpenditurePerHour < bestProposal.predictedExpenditurePerHour)
                    {
                        bestProposal = c;
                    }
                }

                // Accept best proposal
                ACLMessage bestMsg = bestProposal.associatedMessage;
                ACLMessage reply = bestMsg.createReply();

                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                reply.setContent(bestProposal.toString());

                acceptances.addElement(reply);

                proposedContracts.remove(bestProposal);

                // Reject remaining elements
                for(Contract contract : proposedContracts)
                {
                    ACLMessage rejectReply = contract.associatedMessage.createReply();
                    rejectReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    acceptances.addElement(rejectReply);
                }
                inTheMiddleOfANegotiation = false;
            }
            protected void handleInform(ACLMessage inform) {
                currentEnergyContract = new Contract(inform, Proposal.fromString(inform.getContent()));
                ticksTillNextNegotiation = currentEnergyContract.duration;
            }
        };
    }

    private Behaviour getReceiveHelloMessagesBehaviour() {
        return new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate template = MessageTemplate.and(
                        MessageTemplate.MatchContent("hello"),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM) );

                ACLMessage msg = receive(template);

                if(msg != null) {
                    registerAppliance(msg.getSender());
                }

                block();
            }
        };
    }

    private Behaviour getReceiveMessagesBehaviour(AID sender) {
        return new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate template = MessageTemplate.and(
                        MessageTemplate.MatchSender(sender),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM) );

                ACLMessage msg = receive(template);
                if (msg!=null) {
                    if(msg.getContent().contains("consuming")) {
                        handleApplianceAgentConsumptionInform(msg.getSender(), msg.getContent());
                    }
                }

                block();
            }
        };
    }

    private Behaviour getRecalculateAndUpdateBehaviour() {
        return new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    Thread.sleep(250);
                    fireStatusChangedEvent(new HomeStatusContainer(getCurrentNetConsumption(), hourOfDay, dayOfWeek));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /** Prediction Logic **/
    private float predictKWHForNextNHours(float n) {
    	/* TODO(Lachlan 28-9-16) Use linear regression/other predicition techniques */
    	float result = 0;

    	return result;
    }

    public int getCurrentNetConsumption() {
        Vector<ApplianceConsumption> consumers = new Vector();

        Iterator it = currentApplianceConsumption.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            ApplianceConsumption applianceConsumption = (ApplianceConsumption) pair.getValue();
            consumers.add(applianceConsumption);
        }

        return calculateApplianceConsumption(consumers);
    }

    private int calculateApplianceConsumption(Vector<ApplianceConsumption> consumers) {
        int netConsumtion = 0;

        for(ApplianceConsumption ac : consumers) {
            netConsumtion += ac.consuming;
        }

        return netConsumtion;
    }

    /** Appliance Interaction Handlers **/
    private void handleApplianceAgentConsumptionInform(AID aid, String msg) {
        float kwh = Integer.parseInt( msg.replace("consuming=", "") );

        // Update the current consumption knowledge of this appliance
        ApplianceConsumption thisAppliance = currentApplianceConsumption.get(aid.getLocalName());

        // Agent has not yet registered
        if(thisAppliance == null) {
            return; // This should not happen
        }

        thisAppliance.consuming = kwh;
    }

    private void registerAppliance(AID aid) {
        // Create currently consuming AND history
        currentApplianceConsumption.put(aid.getLocalName(), new ApplianceConsumption(0, hourOfDay, dayOfWeek));
        applianceConsumptionHistory.put(aid.getLocalName(), new ApplianceConsumptionHistory());

        addBehaviour(getReceiveMessagesBehaviour(aid));
    }

    private void updateApplianceConsumptionHistory() {
        Iterator it = currentApplianceConsumption.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            String agentIdentifier = (String) pair.getKey();
            ApplianceConsumption applianceConsumption = (ApplianceConsumption) pair.getValue();

            ApplianceConsumptionHistory ach = applianceConsumptionHistory.get(agentIdentifier);
            ach.history.add(new ApplianceConsumption(applianceConsumption.consuming, hourOfDay, dayOfWeek));
        }
    }
}

