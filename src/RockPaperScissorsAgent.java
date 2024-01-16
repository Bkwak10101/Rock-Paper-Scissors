package jadelab2;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RockPaperScissorsAgent extends Agent {
    private Map<String, Integer> opponentMoveHistory = new HashMap<>();
    private Random random = new Random();
    private String[] moves = {"rock", "paper", "scissors"};

    private AID opponentAID; // You need to set this to the opponent's AID

    // Probabilities for each move
    private double probRock;
    private double probPaper;
    private double probScissors;


    protected void setup() {
        System.out.println("Hello! Agent " + getAID().getName() + " is ready.");

        // Initialize the probabilities
        probRock = 0.4; // 40% chance to play rock
        probPaper = 0.4; // 40% chance to play paper
        probScissors = 0.2;

        // Initialize the move history
        opponentMoveHistory.put("rock", 0);
        opponentMoveHistory.put("paper", 0);
        opponentMoveHistory.put("scissors", 0);

        // Register with the DF to be searchable by other agents
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("rock-paper-scissors-player");
        sd.setName(getLocalName() + "-RPS-game");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Add a behavior to search for the opponent agent in the yellow pages
        addBehaviour(new OneShotBehaviour(this) {
            public void action() {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("rock-paper-scissors-player");
                template.addServices(sd);

                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    for (DFAgentDescription dfd : result) {
                        AID provider = dfd.getName();
                        // Ensure not to select itself as the opponent
                        if (!provider.equals(getAID())) {
                            opponentAID = provider;
                            break;
                        }
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                // If an opponent is found and this agent is the starter, send the initial move
                if (opponentAID != null) {
                    Object[] args = getArguments();
                    if (args != null && args.length > 0 && "starter".equals(args[0])) {
                        String initialMove = moves[random.nextInt(moves.length)];
                        System.out.println("Agent " + myAgent.getLocalName() + " starts with: " + initialMove
                        );
                        ACLMessage initialMessage = new ACLMessage(ACLMessage.INFORM);
                        initialMessage.addReceiver(opponentAID);
                        initialMessage.setContent(initialMove);
                        myAgent.send(initialMessage);
                    }
                } else {
                    System.out.println("Agent " + myAgent.getLocalName() + " could not find an opponent.");
                }
            }
        });
        // Add the PlayGameBehaviour to handle incoming moves
        addBehaviour(new PlayGameBehaviour());
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Agent " + getAID().getName() + " is terminating.");
    }

    private class PlayGameBehaviour extends CyclicBehaviour {
        private int roundsPlayed = 0;
        private final int maxRounds = 10;

        public void action() {
            if (roundsPlayed < maxRounds) {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    String opponentMove = msg.getContent();
                    System.out.println(getLocalName() + ": Round " + (roundsPlayed + 1) + " - Opponent played: " + opponentMove);
                    updateOpponentMoveHistory(opponentMove);
                    String myMove = makeMoveBasedOnHistory();
                    System.out.println(getLocalName() + ": Round " + (roundsPlayed + 1) + " - I played: " + myMove);
                    ACLMessage reply = msg.createReply();
                    reply.setContent(myMove);
                    send(reply);

                    roundsPlayed++; // Increment the round count
                } else {
                    block();
                }
            } else {
                System.out.println(getLocalName() + " has finished playing " + maxRounds + " rounds.");
                myAgent.doDelete(); // Optionally terminate the agent
            }
            if (roundsPlayed > 0 && roundsPlayed <= maxRounds) {
                adaptStrategyBasedOnHistory();
            }
        }

        private void adaptStrategyBasedOnHistory() {
            // Adapt strategy by analyzing the most frequent move of the opponent
            // and adjusting probabilities accordingly
            String mostFrequentMove = getMostFrequentMove();
            // Example: Increase the probability of playing
            switch (mostFrequentMove) {
                case "rock":
                    // Increase the probability to play paper
                    probPaper += 0.05;
                    break;
                case "paper":
                    // Increase the probability to play scissors
                    probScissors += 0.05;
                    break;
                case "scissors":
                    // Increase the probability to play rock
                    probRock += 0.05;
                    break;
            }

            // Ensure the probabilities sum up to 1 after adjustment
            normalizeProbabilities();
        }

        private void normalizeProbabilities() {
            double total = probRock + probPaper + probScissors;
            probRock /= total;
            probPaper /= total;
            probScissors = 1.0 - probRock - probPaper; // Scissors probability is the remainder
        }

        private String makeMoveBasedOnProbability() {
            double decision = random.nextDouble();
            if (decision < probRock) {
                return "rock";
            } else if (decision < probRock + probPaper) {
                return "paper";
            } else {
                return "scissors";
            }
        }

        private String getMostFrequentMove() {
            int rockCount = opponentMoveHistory.getOrDefault("rock", 0);
            int paperCount = opponentMoveHistory.getOrDefault("paper", 0);
            int scissorsCount = opponentMoveHistory.getOrDefault("scissors", 0);

            if (rockCount > paperCount && rockCount > scissorsCount) {
                return "rock";
            } else if (paperCount > rockCount && paperCount > scissorsCount) {
                return "paper";
            } else if (scissorsCount > rockCount && scissorsCount > paperCount) {
                return "scissors";
            } else {
// If there is a tie, pick randomly among the tied moves
                String[] tiedMoves = new String[]{"rock", "paper", "scissors"};
                return tiedMoves[random.nextInt(tiedMoves.length)];
            }
        }
    }


    private String makeMoveBasedOnHistory() {
        String mostFrequentMove = moves[0];
        int maxCount = opponentMoveHistory.get(moves[0]);

        for (String move : moves) {
            int count = opponentMoveHistory.get(move);
            if (count > maxCount) {
                maxCount = count;
                mostFrequentMove = move;
            }
        }

        switch (mostFrequentMove) {
            case "rock":
                return "paper";
            case "paper":
                return "scissors";
            case "scissors":
                return "rock";
            default:
                return moves[random.nextInt(moves.length)];
        }
    }

    private void updateOpponentMoveHistory(String move) {
        int count = opponentMoveHistory.getOrDefault(move, 0);
        opponentMoveHistory.put(move, count + 1);
    }
}
