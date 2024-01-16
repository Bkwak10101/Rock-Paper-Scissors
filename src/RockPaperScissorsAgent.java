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

// Agent realizujący grę w Kamień-Papier-Nożyce
public class RockPaperScissorsAgent extends Agent {
    private Map<String, Integer> opponentMoveHistory = new HashMap<>();
    private Random random = new Random();
    private String[] moves = {"rock", "paper", "scissors"};

    private AID opponentAID; // AID przeciwnika

    // Prawdopodobieństwa dla każdego ruchu
    private double probRock;
    private double probPaper;
    private double probScissors;

    // Inicjalizacja agenta
    protected void setup() {
        // Inicjalizacja prawdopodobieństw dla ruchów
        probRock = 0.4; // 40% szansa na zagranie kamieniem
        probPaper = 0.4; // 40% szansa na zagranie papierem
        probScissors = 0.2;

        // Inicjalizacja historii ruchów przeciwnika
        opponentMoveHistory.put("rock", 0);
        opponentMoveHistory.put("paper", 0);
        opponentMoveHistory.put("scissors", 0);

        // Rejestracja agenta w usłudze Directory Facilitator (DF)
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

        // Dodanie zachowania do wyszukiwania przeciwnika w książce telefonicznej (DF)
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
                        // Sprawdzenie, czy agent nie wybiera samego siebie jako przeciwnika
                        if (!provider.equals(getAID())) {
                            opponentAID = provider;
                            break;
                        }
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                // Jeśli przeciwnik zostanie znaleziony i agent jest rozpoczynającym, wysyłamy początkowy ruch
                if (opponentAID != null) {
                    Object[] args = getArguments();
                    if (args != null && args.length > 0 && "starter".equals(args[0])) {
                        String initialMove = moves[random.nextInt(moves.length)];
                        System.out.println("Agent " + myAgent.getLocalName() + " zaczyna od: " + initialMove);
                        ACLMessage initialMessage = new ACLMessage(ACLMessage.INFORM);
                        initialMessage.addReceiver(opponentAID);
                        initialMessage.setContent(initialMove);
                        myAgent.send(initialMessage);
                    }
                } else {
                    System.out.println("Agent " + myAgent.getLocalName() + " nie mógł znaleźć przeciwnika.");
                }
            }
        });
        // Dodanie zachowania PlayGameBehaviour do obsługi przychodzących ruchów
        addBehaviour(new PlayGameBehaviour());
    }

    // Metoda wywoływana przy zakończeniu działania agenta
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Agent " + getAID().getName() + " is terminating.");
    }

    // Klasa wewnętrzna PlayGameBehaviour obsługująca rozgrywkę
    private class PlayGameBehaviour extends CyclicBehaviour {
        private int roundsPlayed = 0;
        private final int maxRounds = 10;

        // Metoda obsługująca kolejne ruchy
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

                    roundsPlayed++;
                } else {
                    block();
                }
            } else {
                System.out.println(getLocalName() + " has finished playing " + maxRounds + " rounds.");
                myAgent.doDelete(); // Optionally terminate the agent
            }
            if (roundsPlayed > 0 && roundsPlayed <= maxRounds) {
                adaptStrategyBasedOnHistory();
                System.out.println(getLocalName() + ": Updated Probabilities - Rock: " + probRock + ", Paper: " + probPaper + ", Scissors: " + probScissors);
            }
        }

        // Metoda dostosowująca strategię na podstawie historii ruchów przeciwnika
        private void adaptStrategyBasedOnHistory() {
            String mostFrequentMove = getMostFrequentMove();
            switch (mostFrequentMove) {
                case "rock":
                    probPaper += 0.05;
                    break;
                case "paper":
                    probScissors += 0.05;
                    break;
                case "scissors":
                    probRock += 0.05;
                    break;
            }

            normalizeProbabilities();
        }

        // Metoda normalizująca prawdopodobieństwa, aby suma wynosiła 1
        private void normalizeProbabilities() {
            double total = probRock + probPaper + probScissors;
            probRock /= total;
            probPaper /= total;
            probScissors = 1.0 - probRock - probPaper;
        }

        // Metoda zwracająca najczęściej wykonywany ruch przeciwnika
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
                // Jeśli jest remis, losujemy spośród równolicznych ruchów
                String[] tiedMoves = new String[]{"rock", "paper", "scissors"};
                return tiedMoves[random.nextInt(tiedMoves.length)];
            }
        }
    }

    // Metoda tworząca ruch na podstawie historii ruchów przeciwnika
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

        // Wybór ruchu zgodnie z dostosowaną strategią
        return switch (mostFrequentMove) {
            case "rock" -> "paper";
            case "paper" -> "scissors";
            case "scissors" -> "rock";
            default -> moves[random.nextInt(moves.length)];
        };
    }

    // Metoda aktualizująca historię ruchów przeciwnika
    private void updateOpponentMoveHistory(String move) {
        int count = opponentMoveHistory.getOrDefault(move, 0);
        opponentMoveHistory.put(move, count + 1);
    }
}
