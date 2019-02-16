package boaAgent;

import negotiator.boaframework.*;

public class Agent007 extends BOAagent {

    /**
     * Implements the abstract method of superclass to put the pieces together
     * and convert the BOAgent to a simple Agent
     */
    @Override
    public void agentSetup () {
        //construct an opponent model
        OpponentModel om = new FrequencyOppModel(this.negotiationSession);
        //construct opponent's strategy
        OMStrategy oms = new BestBid(this.negotiationSession, om);
        //construct our agent's offering strategy
        OfferingStrategy offer_strat = new TimeDependent_Strategy(this.negotiationSession , om , oms , 0.02D , 0.05D, 0.99, 0.0D);
        //construct our agent's acceptance strategy
        AcceptanceStrategy accept_strat = new Accept_Strategy(this.negotiationSession , offer_strat , om, 3.0D, 4.0D);

        setDecoupledComponents(accept_strat , offer_strat , om , oms );
    }

    @Override
    public String getName () {
        return " Agent007 ";
    }
}