
// myALG2

package FinalProject.BL.Agents;
import org.apache.log4j.Logger;

import java.util.*;

import static FinalProject.BL.DataCollection.PowerConsumptionUtils.calculateEPeak;
import static FinalProject.BL.DataCollection.PowerConsumptionUtils.calculateTotalConsumptionWithPenalty;

public class HousePropImp extends SmartHomeAgentBehaviour {

    private final static Logger logger = Logger.getLogger(HousePropImp.class);
    private int[] ticksBag;
    private boolean inImprovementRound = false;
    private double oldPrice;

    public HousePropImp() {
        super();
    }

    //==================================================================================================================
    //==================================================================================================================
    //The main Loop
    @Override
    protected void doIteration() {
        if (agent.isZEROIteration()) {
            // actions of the very first iteration
            initMsgTemplate();
            buildScheduleFromScratch();
            agent.setZEROIteration(false);
            agent.setPriceSum(calcCsum(iterationPowerConsumption));
        } else {
            // actions during inter-houses iteration
            logger.info("");
            receiveNeighboursIterDataAndHandleIt();
            improveSchedule(); // write your code in this function

        }
        beforeIterationIsDone();
        this.currentNumberOfIter++;
    }

    //----------------------------------------YOUR CODE IN THIS FUNCTION----------------------------------------------//
    private void improveSchedule() {
        helper.resetProperties();
        buildScheduleBasic(false);
        List<double[]> allScheds = getNeighbourScheds(); // getting schedules of all neighbours
        List<PropAgent> listOfPropsAgents = new ArrayList<PropAgent>(); //list of properties helps to do inner iterations of properties
        // ==============================================

        propToSubsetsMap.keySet().forEach(prop -> {
            PropAgent agent = new PropAgent(prop, allScheds); //creating new agent representing property
            listOfPropsAgents.add(agent); // add this agent to our list
            Set<Integer> prevTicks = new HashSet<>(getTicksForProp(prop)); // get the Ticks that this property has now
            listOfPropsAgents.get(listOfPropsAgents.indexOf(agent)).setPrevTicks(prevTicks); // set this ticks to relevant agent
        });


        //create neighbours to each agent
        listOfPropsAgents.forEach( agent -> {
            listOfPropsAgents.forEach( neighbour -> {
                if(!neighbour.equals(agent)){
                    agent.neighbours.add(neighbour);
                }
            });
        });

        // 100 iteration loop of properties (here is an example of simulating distributed behaviour of agents)
        for(int i = 0; i < 100 ; i++){
            listOfPropsAgents.forEach( agent -> {
                agent.chooseTicks(); // we change this function //
            });
            listOfPropsAgents.forEach(agent -> {
                agent.changeTicks();
            });
        }

        //after each agent made its choice of assumption (which ticks it is going to work in)
        //we will build the final chosenSched:
        double[] chosenSched = helper.cloneArray(iterationPowerConsumption); //already with background load!
        listOfPropsAgents.forEach( agent -> {
            agent.updateChosenSched(chosenSched);
        });
        allScheds.add(chosenSched);
        double newGrade = calcImproveOptionGrade(chosenSched, allScheds);
        helper.totalPriceConsumption = newGrade;
        helper.ePeak = calculateEPeak(allScheds);
        // final step
        listOfPropsAgents.forEach( agent -> {
            updateTotals(
                    agent.prop,
                    new ArrayList<>(agent.currTicks),
                    propToSensorsToChargeMap.get(agent.prop)
            );
        });

    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private Set<Integer> pickRandomSubsetForProp(PropertyWithData prop) {
        List<Set<Integer>> allSubsets = propToSubsetsMap.get(prop); //all possible subsets of hours (ticks) where the property
        // can work in. These subsets made according to constrains of this particular house
        if (allSubsets == null || allSubsets.isEmpty()) {
            return new HashSet<>(0);
        }
        int index = drawRandomNum(0, allSubsets.size() - 1);
        return allSubsets.get(index);
    }

    private Set<Integer> smartChoiceForProp(PropertyWithData prop, List <Set<Integer>> explored) {
        List<Set<Integer>> allSubsets = propToSubsetsMap.get(prop);
        int index = drawRandomNum(0, allSubsets.size() - 1); //pick a random assgiment
        //int counter = 0;
        if (allSubsets == null || allSubsets.isEmpty())
            return new HashSet<>(0);

        do {
            if (!explored.contains(allSubsets.get(index)) || explored == null || explored.isEmpty()) { // assignemt not in explored set
                if (flipCoin(0.5f))
                    explored.add(allSubsets.get(index));
            }//if
            else index = drawRandomNum(0, allSubsets.size() - 1); //pick a random assgiment
        }
        while(!explored.contains(allSubsets.get(index)));
        return allSubsets.get(index);
    }


    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    //==================================================================================================================
    //==================================================================================================================
    @Override
    protected void generateScheduleForProp(PropertyWithData prop, double ticksToWork,
                                           Map<String, Integer> sensorsToCharge, boolean randomSched) {
        //iteration 0, build a schedule for prop
        if (agent.isZEROIteration()) {
            startWorkZERO(prop, sensorsToCharge, ticksToWork);
        }
        //non-zero iteration, just fill propToSubsetsMap if not already filled.
        //the schedule for all of the properties together will be built later in improveSchedule
        else {
            if (!propToSubsetsMap.containsKey(prop)) {
                getSubsetsForProp(prop, ticksToWork); //to put in map if absent
            }
        }
    }
    //==================================================================================================================
    //==================================================================================================================
    @Override
    protected void onTermination() {
        logger.info("onTermination!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        logger.info(agent.getName() + " for problem " + agent.getProblemId() + "and algo HouseProp is TERMINATING!");
    }

    //==================================================================================================================
    //==================================================================================================================
    @Override
    public HousePropImp cloneBehaviour() {
        logger.info("cloneBehaviour!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        HousePropImp newInstance = new HousePropImp();
        newInstance.finished = this.finished;
        newInstance.currentNumberOfIter = this.currentNumberOfIter;
        newInstance.FINAL_TICK = this.FINAL_TICK;
        newInstance.agentIterationData = null;
        return newInstance;
    }

    //==================================================================================================================
    //==================================================================================================================
    @Override
    protected double calcImproveOptionGrade(double[] newPowerConsumption, List<double[]> allScheds) {
        logger.info("calcImproveOptionGrade!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        double price = calcCsum(newPowerConsumption);
        return price + calculateEPeak(allScheds);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------
    // PROPERTY - AGENT CLASS
    // -----------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------
    ///inner class represents the Agent of Property
    class PropAgent {
        PropertyWithData prop;
        List<double[]> allScheds; //reference
        Set<Integer> nextTicks;
        Set<Integer> currTicks;
        double[] currSched;
        List<PropAgent> neighbours;
        boolean toChange;

        PropAgent(PropertyWithData prop, List<double[]> allScheds){
            this.prop = prop;
            this.allScheds = allScheds;
            this.neighbours = new ArrayList<PropAgent>();
            this.toChange = false;
        }

        void setPrevTicks(Set<Integer> prevTicks){ this.currTicks = prevTicks; }


        void updateChosenSched(double[] chosenSched){
            double powerCons = prop.getPowerConsumedInWork();
            this.currTicks.forEach(tick -> {
                chosenSched[tick] += powerCons;
            });
        }
        
        // insert ticks of neighbors
        void updateNeighbors () { 
        	neighbours.forEach(neighbour -> {
                neighbour.currTicks.forEach(tick -> {
                    this.currSched[tick] += neighbour.prop.getPowerConsumedInWork();
                    // every prop has a method getPowerConsumedInWork() - it returns a value in energy units, how much it
                    // consumes in one hour (in one tick)
                });
            });
        }
        
        // insert my new ticks
        void insertMyTicks(Set <Integer> ticks) {
        	ticks.forEach(tick -> { 
            this.currSched[tick] += this.prop.getPowerConsumedInWork();
        	});
        }
            
        void chooseTicks(){
        	// this.nextTicks = pickRandomSubsetForProp(this.prop); // in this case we choose the random set of ticks
            //see pickRandomSubsetForProp() in order to understand how it choose the set. You also can play with it
        	
            /////////////////////// old one ////////////////////
            this.currSched = helper.cloneArray(iterationPowerConsumption); //already with background load! The background here
            // is additional amount of energy spent by house in this tick - do not forget to create the new schedule that way
            // in order to not loose this information

            // insert ticks of neighbours
            this.updateNeighbors();
            
            // insert my curr ticks
            insertMyTicks(this.currTicks);
            
            // calc the grade
            this.allScheds.add(this.currSched);
            double oldGrade = calcImproveOptionGrade(this.currSched, this.allScheds); // in order to get proper grade to
            // assumption we use calcImproveOptionGrade() function. It's input is the current schedule of the house and
            // all schedules together of hole neighbourhood including this concrete house also. That is why it is important
            // to add the current schedule to all schedules before inserting to the function -> this.allScheds.add(this.currSched)
            // The grate holds a combination of total electricity consumption and peak of use in one number. You can
            //use it in order to compare different assignments.

            this.allScheds.remove(this.currSched); // in order to calculate another assumption in a proper way
            // we new to replace this currSched from allScheds

            
            /////////////////////// new one ////////////////////
            double newGrade = 0;
            List<Set<Integer>> explored = new ArrayList<Set<Integer>>();
            List<Set<Integer>> allSubsets = propToSubsetsMap.get(prop); // all possible subsets of hours (ticks) for prop
            if (flipCoin(0.7f)) {
	            if (allSubsets == null || allSubsets.isEmpty()) { // when no ticks exists
	            	this.nextTicks = new HashSet<>(0); // as pickRandom function
	            	this.currSched = helper.cloneArray(iterationPowerConsumption); // in order to not lose info
	            	this.updateNeighbors(); // update consumption of the agent's neighbors because of my change
	            	this.allScheds.add(this.currSched);
	            	newGrade = calcImproveOptionGrade(this.currSched, this.allScheds); // calculate improve neighborhood grade
	            	this.allScheds.remove(this.currSched);
	            }
	            else { // ticks exists
	            	Set <Integer> tempTicks = new HashSet<Integer>();
	            	double tempGrade = 0;
	            	double bestGrade = 0; // this will be the favorite assignment
	            	// for all assignments of an agent, the favorite one will be chosen
	            	for(int i= 0; i < allSubsets.size(); i++) {
	            		tempTicks = allSubsets.get(i);
	                	this.currSched = helper.cloneArray(iterationPowerConsumption); // in order to not lose info
	                	this.updateNeighbors(); // update consumption of the agent's neighbors because of my change
	                	this.insertMyTicks(tempTicks); // insert my new temp ticks 
	                	
	                	// calc the grade
	                	this.allScheds.add(this.currSched); 
	                	tempGrade = calcImproveOptionGrade(this.currSched, this.allScheds); // calculate improve neighborhood grade
	                	this.allScheds.remove(this.currSched);
	                	if (tempGrade < bestGrade || i == 0) { // temp is better than best grade we saved
	                		bestGrade = tempGrade;
	                		this.nextTicks = tempTicks;
	                	}
	            	}
	            	newGrade = bestGrade; // checking best assignment with previous assignment
	            }
            }
            else {
            	this.nextTicks = smartChoiceForProp(this.prop, explored); // in this case we choose smart set of ticks
            	this.currSched = helper.cloneArray(iterationPowerConsumption); // in order to not lose info
            	this.updateNeighbors(); // update consumption of the agent's neighbors because of my change
            	this.insertMyTicks(this.nextTicks); // insert my new ticks 
            	// calc the grade
            	this.allScheds.add(this.currSched); 
            	newGrade = calcImproveOptionGrade(this.currSched, this.allScheds); // calculate improve neighborhood grade
            	this.allScheds.remove(this.currSched);
            }
            
            // decision: we compare both assignments using their grades
            // you can use flipCoin(0.8f) as a boolean probability function
            if (newGrade < oldGrade && flipCoin(0.7f)){ // minimum
                this.toChange = true;
            }
        }

        void changeTicks(){
            if(this.toChange){
                this.currTicks = this.nextTicks;
                this.toChange = false;
            }
        }
    }
    // -----------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------

}


