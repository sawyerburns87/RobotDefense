


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList; 

import jig.misc.rd.AirCurrentGenerator;
import jig.misc.rd.Direction;
import jig.misc.rd.RobotDefense;


/**
 *  A simple agent that uses reinforcement learning to direct the vacuum
 *  The agent has the following critical limitations:
 *  
 *  	- it only considers a small set of possible actions
 *  	- it does not consider turning the vacuum off
 *  	- it only reconsiders an action when the 'local' state changes  
 *         in some cases this may take a (really) long time
 *      - it uses a very simplisitic action selection mechanism
 *      - actions are based only on the cells immediately adjacent to a tower
 *      - action values are not dependent (at all) on the resulting state 
 */
public class munchersOne extends BaseLearningAgent {

	/**
	 * A Map of states to actions
	 * 
	 *  States are encoded in the StateVector objects
	 *  Actions are associated with a utility value and stored in the QMap
	 */
	HashMap<StateVector,QMap> actions = new HashMap<StateVector,QMap>();

	/**
	 * The agent's sensor system tracks /how many/ insects a particular generator
	 * captures, but here I want to know /when/ an air current generator just
	 * captured an insect so I can reward the last action. We use this captureCount
	 * to see when a new capture happens.
	 */
	HashMap<AirCurrentGenerator, Integer> captureCount;

	//reward for sucking a bug but if stuck at same action for > 3 states, start negative reward
	HashMap<AirCurrentGenerator, Integer> concurrentCount;
	double[] concurrentRewardScl = {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,-1.5,-2,-2,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5,-5};

	HashMap<AirCurrentGenerator, Integer> crystalCount;
	HashMap<AirCurrentGenerator, AgentAction> lastAction;

	private static final AgentAction [] potentials;

	double runningMS;
	double targetMS = 200;

	static {
		Direction [] dirs = Direction.values();
		potentials = new AgentAction[dirs.length * 5];

		int i = 0;
		for(Direction d: dirs) {
			// creates a new directional action with the power set to full
			// power can range from 1 ... AirCurrentGenerator.POWER_SETTINGS
			// Allow power settings 0, 2, and 4 only
			//potentials[i] = new AgentAction(0, d);
			//i++;
			potentials[i] = new AgentAction(0, d);
			i++;
			potentials[i] = new AgentAction(1, d);
			i++;
			potentials[i] = new AgentAction(2, d);
			i++;
			potentials[i] = new AgentAction(3, d);
			i++;
			potentials[i] = new AgentAction(4, d);
			i++;
		}

	}
	
	public munchersOne() {
		captureCount = new HashMap<AirCurrentGenerator,Integer>();
		crystalCount = new HashMap<AirCurrentGenerator,Integer>();
		concurrentCount = new HashMap<AirCurrentGenerator,Integer>();
		lastAction = new HashMap<AirCurrentGenerator,AgentAction>();		
	}
	
	public void step(long deltaMS) {
		StateVector state;
		QMap qmap;

		// This must be called each step so that the performance log is 
		// updated.
		updatePerformanceLog();

		runningMS += deltaMS;
		if(runningMS < targetMS)
			return;
		else
			runningMS = 0;

		for (AirCurrentGenerator acg : sensors.generators.keySet()) {
			stateChanged(acg);

			// Check the current state, and make sure member variables are
			// initialized for this particular state...
			state = thisState.get(acg);
			if (actions.get(state) == null) {
				actions.put(state, new QMap(potentials));
			}

			if (concurrentCount.get(acg) == null) concurrentCount.put(acg, 0);
			if (captureCount.get(acg) == null) captureCount.put(acg, 0);
			if (crystalCount.get(acg) == null) crystalCount.put(acg, acg.getConsumption());


			// Check to see if an insect was just captured by comparing our
			// cached value of the insects captured by each ACG with the
			// most up-to-date value from the sensors
			boolean justCaptured = (captureCount.get(acg) < sensors.generators.get(acg));

			int crystalsUsed = acg.getConsumption() - crystalCount.get(acg);
			crystalCount.put(acg, acg.getConsumption());

			int lastStateInsectsSucked = lastState.get(acg).insectsSucked(acg, getSensorySensors());

			// if this ACG has been selected by the user, we'll do some verbose printing
			boolean verbose = (RobotDefense.getGame().getSelectedObject() == acg);

			boolean isEmptySpace = lastState.get(acg).hashCode() == lastState.get(acg).emptyHash();

			// If we did something on the last 'turn', we need to reward it
			if (lastAction.get(acg) != null ) {
				// get the action map associated with the previous state
				qmap = actions.get(lastState.get(acg));
				double rewardStep = 0;

				//Negative reward for power usage
				//rewardStep += -(crystalsUsed-2)/24.0;
				if(isEmptySpace && lastAction.get(acg).getPower() == 0){
					rewardStep += 5;
				}
				else if(isEmptySpace && lastAction.get(acg).getPower() != 0){
					rewardStep -= 5;
				}
				
				if (justCaptured) {
					// capturing insects is good
					rewardStep += 50.0;
					captureCount.put(acg,sensors.generators.get(acg));
				}

				
				if(lastStateInsectsSucked > 0)
					rewardStep += lastStateInsectsSucked * 2.0;
				else
					rewardStep += lastStateInsectsSucked;
				//rewardStep = concurrentRewardScl[concurrentCount.get(acg)] * Math.abs(rewardStep);

				qmap.rewardAction(lastAction.get(acg), rewardStep, actions.get(lastState.get(acg)));

				if (verbose) {
					System.out.println("");
					System.out.println("Is Empty Space: " + isEmptySpace);
					System.out.println("Concurrent Actions: " + concurrentCount.get(acg));
					System.out.println("Crystal Consumed: " + crystalsUsed);
					System.out.println("Insects being sucked: " + lastStateInsectsSucked);
					System.out.println("Last State for " + acg.toString() );
					System.out.println(lastState.get(acg).representation());
					System.out.println("Updated Last Action: " + qmap.getQRepresentation());
				}
			}

			// get the action map associated with the current state
			qmap = actions.get(state);

			if (verbose) {
				System.out.println("This State for Tower " + acg.toString() );
				System.out.println(thisState.get(acg).representation());
			}

			AgentAction bestAction = qmap.findBestAction(verbose, lastAction.get(acg));
			bestAction.doAction(acg);

			if(lastAction.get(acg) != null && bestAction.getDirection() == lastAction.get(acg).getDirection() && bestAction.getPower() == lastAction.get(acg).getPower()){
				concurrentCount.put(acg, concurrentCount.get(acg)+1);
			}
			else{
				concurrentCount.put(acg, 0);
			}

			// finally, store our action so we can reward it later.
			lastAction.put(acg, bestAction);
		}
	}


	/**
	 * This inner class simply helps to associate actions with utility values
	 */
	static class QMap {
		static Random RN = new Random();

		double gamma = 0.9;
		double alpha = 0.1;

		private double[] utility; 		// current utility estimate
		private AgentAction[] actions;  // potential actions to consider

		public QMap(AgentAction[] potential_actions) {

			actions = potential_actions.clone();
			int len = actions.length;

			utility = new double[len];
			for(int i = 0; i < len; i++) {
				utility[i] = 0.0;
			}
		}

		double maxQ(){
			int maxi = 0;
			for(int i = 1; i < utility.length; i++){
				if(utility[i] > utility[maxi]){
					maxi = i;
				}
			}

			return utility[maxi];
		}

		AgentAction differentPower(AgentAction startAction){
			ArrayList<Integer> potentialActions = new ArrayList<Integer>();
			for(int i = 0; i < actions.length; i++){
				if(actions[i].getDirection() == startAction.getDirection() && startAction != actions[i]){
					potentialActions.add(i);
				}
			}
			return actions[potentialActions.get(Math.abs(RN.nextInt()) % potentialActions.size())];
		}

		/**
		 * Finds the 'best' action for the agent to take.
		 * 
		 * @param verbose
		 * @return
		 */
		public AgentAction findBestAction(boolean verbose, AgentAction lastAct) {
			int i,maxi,maxcount;
			maxi=0;
			maxcount = 1;
			
			if (verbose)
				System.out.print("Picking Best Actions: " + getQRepresentation());

			ArrayList<Integer> posMoves = new ArrayList<Integer>();
			for (i = 1; i < utility.length; i++) {
				if (utility[i] > utility[maxi]) {
					maxi = i;
					maxcount = 1;
					posMoves.clear();
					posMoves.add(i);
				}
				else if (utility[i] == utility[maxi]) {
					posMoves.add(i);
					maxcount++;
				}
			}
			
			double percSame = (posMoves.size() * 1.0 / actions.length);
			//change power if random, change any if percSame is also high
			if(utility[maxi] <= 0){
				int which = RN.nextInt(actions.length);
				if (verbose)
					System.out.println( " -- Doing Random (" + which + ")!!" + " - Util: " + utility[which]);

				return actions[which];
			}
			else {
				if(posMoves.size() == 1){
					int singleIndex = posMoves.get(0);
					if(verbose) System.out.println("Single best action: #" + singleIndex + " - Util: " + utility[singleIndex]);
					if(utility[singleIndex] < 5 && utility[singleIndex] > 0.05 && RN.nextDouble() < 0.5){
						return differentPower(actions[singleIndex]);
					}
					return actions[singleIndex];
				}
				else if(posMoves.size() > 1 && lastAct != null){
					for(int a = 0; a < posMoves.size(); a++){
						if(actions[a].getDirection() == lastAct.getDirection() && actions[a].getPower() == lastAct.getPower()){
							if(verbose) System.out.println("Matched last: " + a + " out of " + actions.length + " - Util: " + utility[a]);
							if(utility[a] < 5 && utility[a] > 0.05){
								return differentPower(actions[a]);
							}
							return actions[a];
						}
					}
				}

				if(verbose) System.out.println("No consistent action: " + maxi + " out of " + actions.length + " - Util: " + utility[maxi]);
				return actions[maxi];
			}
		}

		/**
		 * Modifies an action value by associating a particular reward with it.
		 * 
		 * @param a the action performed 
		 * @param value the reward received
		 */
		public void rewardAction(AgentAction a, double value, QMap nextMap) {
			int i;
			for (i = 0; i < actions.length; i++) {
				if (a == actions[i]) break;
			}
			if (i >= actions.length) {
				System.err.println("ERROR: Tried to reward an action that doesn't exist in the QMap. (Ignoring reward)");
				return;
			}

			//new qlearning algorithm
			utility[i] = utility[i] + alpha * (value + (gamma * nextMap.maxQ()) - utility[i]);
		}
		/**
		 * Gets a string representation (for debugging).
		 * 
		 * @return a simple string representation of the action values
		 */
		public String getQRepresentation() {
			StringBuffer sb = new StringBuffer(80);

			for (int i = 0; i < utility.length; i++) {
				sb.append(String.format("%.2f  ", utility[i]));
			}
			return sb.toString();

		}

	}
}
