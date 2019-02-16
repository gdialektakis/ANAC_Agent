package boaAgent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import negotiator.boaframework.AcceptanceStrategy;
import negotiator.boaframework.Actions;
import negotiator.boaframework.BOAparameter;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;

/**
 * This Acceptance Condition will accept an opponent bid if the utility is
 * higher than a percentage of the bid the agent is ready to present
 *
 */
public class Accept_Strategy extends AcceptanceStrategy {

    private double a;
    private double b;


    public Accept_Strategy(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel om, double alpha, double beta) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        this.a = alpha;
        this.b = beta;
        this.opponentModel = om;
        this.init(negoSession, strat, om, new HashMap<>());
    }

    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel oppModel,
                     Map<String, Double> parameters) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        this.opponentModel = oppModel;

        if (parameters.get("a") != null || parameters.get("b") != null) {
            a = parameters.get("a");
            b = parameters.get("b");
        } else {
            a = 3;
            b = 4;
        }
    }

  /*  @Override
    public String printParameters() {
        return "[a: " + a + " b: " + b + "]";
    }
*/

    @Override
    public Actions determineAcceptability() {
        // get the utility of my next bid
        double nextBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
        // get the utility of opponent's last bid
        double lastOppBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails()
                .getMyUndiscountedUtil();
        // accept the bid if it exceeds a percentage of utility of the bid our Agent is going to offer
        if (lastOppBidUtil  >= nextBidUtil * (a/b)) {
            return Actions.Accept;
        }
        return Actions.Reject;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {

        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("a", 3.0,
                "Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
        set.add(new BOAparameter("b", 4.0,
                "Accept when the opponent's utility * a + b is greater than the utility of our current bid"));

        return set;
    }

    @Override
    public String getName() {
        return "Accept_Strategy ";
    }
}