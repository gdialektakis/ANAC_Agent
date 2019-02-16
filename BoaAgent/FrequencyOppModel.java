package boaAgent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import negotiator.Bid;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.BOAparameter;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OpponentModel;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Objective;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;

/**
 * BOA framework implementation of the HardHeaded Frequecy Model.
 *
 * Default: learning coef l = 0.2; learnValueAddition v = 1.0
 *
 */
public class FrequencyOppModel extends OpponentModel {

    // the learning coefficient is the weight that is added each turn to the
    // issue weights which changed.
    // It's a trade-off between concession speed and accuracy.
    private double learnCoef;
    // value which is added to a value of an issue if it is found. Determines how fast
    // the value weights converge.
    private int learnValueAddition;
    private int amountOfIssues;

    /**
     * Constructor.
     */
    public FrequencyOppModel(NegotiationSession negoSession) {
        this.init(negoSession, new HashMap<>());
    }

    /**
     * Initializes the utility space of the opponent such that all value issue
     * weights are equal.
     */
    @Override
    public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
        super.init(negotiationSession, parameters);
        this.negotiationSession = negotiationSession;
        if (parameters != null && parameters.get("l") != null) {
            learnCoef = parameters.get("l");
        } else {
            learnCoef = 0.2;
        }
        learnValueAddition = 1;
        initializeModel();
    }

    private void initializeModel() {
        opponentUtilitySpace = new AdditiveUtilitySpace(negotiationSession.getDomain());
        amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
        double commonWeight = 1D / (double) amountOfIssues;

        // initialize the weights
        for (Entry<Objective, Evaluator> e : opponentUtilitySpace.getEvaluators()) {
            // set the issue weights
            opponentUtilitySpace.unlock(e.getKey());
            e.getValue().setWeight(commonWeight);
            try {
                // set all value weights to one (they are normalized when
                // calculating the utility)
                for (ValueDiscrete vd : ((IssueDiscrete) e.getKey()).getValues())
                    ((EvaluatorDiscrete) e.getValue()).setEvaluation(vd, 1);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Determines the difference between the previous and the current bid. For each issue, it is determined
     * if the value changed. If this is the case, a 1 is stored in a hashmap for
     * that issue, else a 0.
     *
     * @param previousBid    the previous bid of the opponent
     *
     * @param currentBid     the current bid of the opponent
     *
     * @return  HashMap<Integer, Integer> diff : the difference between the two bids
     *
     */
    private HashMap<Integer, Integer> determineDifference(BidDetails previousBid, BidDetails currentBid) {

        HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
        try {
            for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
                // compare the values of each issue to see if the have changed between the two bids
                diff.put(i.getNumber(), (((ValueDiscrete) currentBid.getBid().getValue(i.getNumber()))
                        .equals((ValueDiscrete) previousBid.getBid().getValue(i.getNumber()))) ? 0 : 1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // return the hashmap
        return diff;
    }


    /**
     * Updates the opponent model given a bid.
     */
    @Override
    public void updateModel(Bid opponentBid, double time) {
        if (negotiationSession.getOpponentBidHistory().size() < 2) {
            return;
        }
        int numberOfUnchanged = 0;
        // get the current bid of the opponent
        BidDetails oppBid = negotiationSession.getOpponentBidHistory().getHistory()
                .get(negotiationSession.getOpponentBidHistory().size() - 1);
        // get the previous bid of the opponent
        BidDetails prevOppBid = negotiationSession.getOpponentBidHistory().getHistory()
                .get(negotiationSession.getOpponentBidHistory().size() - 2);
        HashMap<Integer, Integer> diffSet = determineDifference(prevOppBid, oppBid);

        // count the number of unchanged issues in value
        for (Integer i : diffSet.keySet()) {
            if (diffSet.get(i) == 0)
                numberOfUnchanged++;
        }

        // This is the value to be added to weights of unchanged issues before normalization.
        // Also the value that is taken as the minimum possible weight,
        // (therefore defining the maximum possible also).
        double goldenValue = learnCoef / (double) amountOfIssues;
        // The total sum of weights before normalization.
        double totalSum = 1D + goldenValue * (double) numberOfUnchanged;
        // The maximum possible weight
        double maximumWeight = 1D - ((double) amountOfIssues) * goldenValue / totalSum;

        // re-weighing issues while making sure that the sum remains 1
        for (Integer i : diffSet.keySet()) {
            if (diffSet.get(i) == 0 && opponentUtilitySpace.getWeight(i) < maximumWeight)
                opponentUtilitySpace.setWeight(opponentUtilitySpace.getDomain().getObjectives().get(i),
                        (opponentUtilitySpace.getWeight(i) + goldenValue) / totalSum);
            else
                opponentUtilitySpace.setWeight(opponentUtilitySpace.getDomain().getObjectives().get(i),
                        opponentUtilitySpace.getWeight(i) / totalSum);
        }

        // Then for each issue value that has been offered last time, a constant
        // value is added to its corresponding ValueDiscrete.
        try {
            for (Entry<Objective, Evaluator> e : opponentUtilitySpace.getEvaluators()) {
                // cast issue to discrete and retrieve value. Next, add constant
                // learnValueAddition to the current preference of the value to
                // make it more important
                ((EvaluatorDiscrete) e.getValue()).setEvaluation(
                        oppBid.getBid().getValue(((IssueDiscrete) e.getKey()).getNumber()),
                        (learnValueAddition + ((EvaluatorDiscrete) e.getValue()).getEvaluationNotNormalized(
                                ((ValueDiscrete) oppBid.getBid().getValue(((IssueDiscrete) e.getKey()).getNumber())))));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the utility of a given bid.
     */
    @Override
    public double getBidEvaluation(Bid bid) {
        double BidUtil = 0;
        try {
            BidUtil = opponentUtilitySpace.getUtility(bid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BidUtil;
    }

    @Override
    public String getName() {
        return "Frequency Opponent Model";
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("l", 0.2,
                "The learning coefficient determines how quickly the issue weights are learned"));
        return set;
    }
}