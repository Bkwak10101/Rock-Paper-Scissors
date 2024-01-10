package jadelab2;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Hashtable;
import java.util.Random;

public class GameAgent extends Agent {
    private Hashtable catalogue;
    private GameGui myGui;
    private Random random = new Random();
    private String[] moves = {"rock", "paper", "scissors"};

    protected void setup() {
        catalogue = new Hashtable();
        myGui = new GameGui(this);
        myGui.display();

        //book selling service registration at DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("game-starts");
        sd.setName("JADE-Rock-paper-scissors");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new AnalyzeOpponentMoves());

        addBehaviour(new GameServer());
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        myGui.dispose();
        System.out.println("Player agent" + getAID().getName() + " terminated.");
    }

    //invoked from GUI, when a new book is added to the catalogue
//    public void updateCatalogue(final String title, final int price) {
//        addBehaviour(new OneShotBehaviour() {
//            public void action() {
//                catalogue.put(title, new Integer(price));
//                System.out.println(getAID().getLocalName() + ": " + title + " put into the catalogue. Price = " + price);
//            }
//        });
//    }

    private class AnalyzeOpponentMoves extends CyclicBehaviour {
        public void action() {
            //proposals only template
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.get(title);
                if (price != null) {
                    //title found in the catalogue, respond with its price as a proposal
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue()));
                } else {
                    //title not found in the catalogue
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }


    private class GameServer extends CyclicBehaviour {
        public void action() {
            //purchase order as proposal acceptance only template
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.remove(title);
                if (price != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(getAID().getLocalName() + ": " + title + " sold to " + msg.getSender().getLocalName());
                } else {
                    //title not found in the catalogue, sold to another agent in the meantime (after proposal submission)
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

}
