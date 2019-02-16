package boaAgent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import negotiator.Bid;
import negotiator.Deadline;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.BOAparameter;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.NoModel;
import negotiator.boaframework.OMStrategy;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.SortedOutcomeSpace;

/**
 * This is a class used to implement a TimeDependentAgent Strategy
 * adapted from S. Shaheen Fatima Michael Wooldridge Nicholas R.
 * Jennings Optimal Negotiation Strategies for Agents with Incomplete Information
 *
 * The default strategy was extended to enable the usage of opponent models.
 *
 */
public class TimeDependent_Strategy extends OfferingStrategy {

    /**
     * k in [0, 1]. For k = 0 the agent starts with a bid of maximum utility
     */
    private double k;
    /** Maximum target utility */
    private double Umax;
    /** Minimum target utility */
    private double Umin;
    /** Concession factor */
    private double e;
    /** Outcome space */
    private SortedOutcomeSpace outcomespace;
    private Deadline deadLine;
    private double newDeadline;
    // Percentage of time in which we'll just keep offering the maximum utility bid
    private static double Offer_Max_Util_Time = 0.2D;

    /**
     * Method which initializes the agent by setting all parameters. The
     * parameter "e" is the only parameter which is required.
     */

    public TimeDependent_Strategy(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, double e, double k, double max, double min) {
        this.e = e;
        this.k = k;
        this.Umax = max;
        this.Umin = min;
        this.negotiationSession = negoSession;
        this.outcomespace = new SortedOutcomeSpace(this.negotiationSession.getUtilitySpace());
        this.negotiationSession.setOutcomeSpace(this.outcomespace);
        this.opponentModel = model;
        this.omStrategy = oms;
        this.deadLine = new Deadline();
        this.newDeadline = 0.0;
        try {
            this.init(negoSession, model, oms, new HashMap<>());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms,
                     Map<String, Double> parameters) throws Exception {
        super.init(negoSession, parameters);
        if (parameters.get("e") != null) {
            this.negotiationSession = negoSession;

            outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
            negotiationSession.setOutcomeSpace(outcomespace);

            this.e = parameters.get("e");

            if (parameters.get("k") != null)
                this.k = parameters.get("k");
            else
                this.k = 0.05;

            if (parameters.get("min") != null)
                this.Umin = parameters.get("min");
            else
                this.Umin = negoSession.getMinBidinDomain().getMyUndiscountedUtil();

            if (parameters.get("max") != null) {
                Umax = parameters.get("max");
            } else {
                BidDetails maxBid = negoSession.getMaxBidinDomain();
                Umax = maxBid.getMyUndiscountedUtil();
            }

            this.opponentModel = model;
            this.omStrategy = oms;
        } else {
            throw new Exception("Constant \"e\" for the concession speed was not set.");
        }
    }


    @Override
    public BidDetails determineOpeningBid() {
        return determineNextBid();
    }

    /**
     * Offering strategy which retrieves the target utility based on time and looks for
     * the nearest bid if no opponent model is specified. If an opponent model
     * is specified, then the agent returns a bid according to the opponent model
     * strategy.
     */
    @Override
    public BidDetails determineNextBid() {

        double time = negotiationSession.getTime();

        // for the first 20% of the time, offer the bid which gives max utility
        if(time < Offer_Max_Util_Time){
            BidDetails maxBid = null;
            try {
                maxBid = this.outcomespace.getMaxBidPossible();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception: cannot generate max utility bid");
            }
            return maxBid;

        }else {
            //compute the target utility based on time
            double utilityGoal = p(time);
            // if there is no opponent model available
            if (opponentModel instanceof NoModel) {
                // generate a Bid near the Utility we wish to have
                nextBid = negotiationSession.getOutcomeSpace().getBidNearUtility(utilityGoal);
            } else {
                // get the best Bid for the Opponent among a list of similarly preferred bids for our Agent
                // Then, make an offer with that bid
                nextBid = omStrategy.getBid(outcomespace, utilityGoal);
            }
        }
        return nextBid;
    }

    /**
     * A wide range of time dependent functions can be defined by varying the
     * way in which f(t) is computed. However, functions must ensure that 0 <=
     * f(t) <= 1, f(0) = k, and f(1) = 1.
     *
     * That is, the offer will always be between the value range, at the
     * beginning it will give the initial constant and when the deadline is
     * reached, it will offer the reservation value.
     *
     */
    public double f(double t) {
        // For e = 0 (special case), the bids will drop linearly.
        if (e == 0)
            return k;

        double ft = 0;
        if(negotiationSession.getTime() > 0.7) {
            ft = k + (1 - k) * Math.pow(Math.min(t, deadLine.getValue())
                    / deadLine.getValue(), 1.0 / e);
        }else{
            ft = k + (1 - k) * Math.pow(Math.min(t, getPseudoDeadline() )
                    / getPseudoDeadline() , 1.0 / e);
        }
        return ft;
    }

    /**
     * Makes sure the target utility is within the acceptable range according to
     * the domain between Umax and Umin!
     *
     * @param t the current time of the negotiation
     * @return target utility based on current time
     */
    public double p(double t) {

        double Util = Umin + (1 - f(t)) * (Umax - Umin) ;
        // too soon to receive such low utility
        if(negotiationSession.getTime() < 0.7 && Util < 0.75){
            Util = 0.75 ;
        }
        return Util;
    }


    public NegotiationSession getNegotiationSession() {
        return negotiationSession;
    }

    private double getPseudoDeadline() {
        double currentTime = negotiationSession.getTime();
        if(currentTime <= newDeadline){
            return newDeadline;
        }
        newDeadline = currentTime + (deadLine.getValue()/12);
        return newDeadline;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("e", 0.02, "Concession rate"));
        set.add(new BOAparameter("k", 0.05, "Offset"));
        set.add(new BOAparameter("min", 0.0, "Minimum utility"));
        set.add(new BOAparameter("max", 0.99, "Maximum utility"));

        return set;
    }

    @Override
    public String getName() {
        return "TimeDependent Offering Strategy";
    }
}