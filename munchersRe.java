


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


public class munchersRe extends BaseLearningAgent {

	HashMap<StateVector,QMap> actions = new HashMap<StateVector,QMap>();
	HashMap<AirCurrentGenerator, Integer> captureCount;
	HashMap<AirCurrentGenerator, AgentAction> lastAction;
	private static final AgentAction [] potentials;

	double runningMS;
	double targetMS = 800;

	static {
		Direction [] dirs = Direction.values();
		potentials = new AgentAction[dirs.length * 3];

		int i = 0;
		for(Direction d: dirs) {
			potentials[i] = new AgentAction(0, d);
			i++;
			potentials[i] = new AgentAction(2, d);
			i++;
			potentials[i] = new AgentAction(4, d);
			i++;
		}

	}
	
	public munchersRe() {
		captureCount = new HashMap<AirCurrentGenerator,Integer>();
		lastAction = new HashMap<AirCurrentGenerator,AgentAction>();		
	}
	
	public void step(long deltaMS) {
		StateVector state;
		QMap qmap;

		updatePerformanceLog();

		runningMS += deltaMS;
			
		for (AirCurrentGenerator acg : sensors.generators.keySet()) {
			if (!stateChanged(acg) && runningMS < targetMS) continue;

			runningMS = 0;

			state = thisState.get(acg);
			if (actions.get(state) == null) {
				actions.put(state, new QMap(potentials));
			}
			if (captureCount.get(acg) == null) captureCount.put(acg, 0);


			boolean justCaptured;
			justCaptured = (captureCount.get(acg) < sensors.generators.get(acg));

			boolean verbose = (RobotDefense.getGame().getSelectedObject() == acg);
			boolean isEmptySpace = lastState.get(acg).hashCode() == lastState.get(acg).emptyHash();
			int lastStateInsectsSucked = lastState.get(acg).insectsSucked(acg, getSensorySensors());
			
			if (lastAction.get(acg) != null ) {
				qmap = actions.get(lastState.get(acg));
				
				if(isEmptySpace && lastAction.get(acg).getPower() == 0){
					qmap.rewardAction(lastAction.get(acg), 8, actions.get(state), 0.9, 0);
				}
				else if(isEmptySpace && lastAction.get(acg).getPower() != 0){
					qmap.rewardAction(lastAction.get(acg), -8, actions.get(state), 0.9, 0);
				}
				
				//If there is an insect on the map in its radius and its missing it
				if(lastStateInsectsSucked < 0)
					qmap.rewardAction(lastAction.get(acg), lastStateInsectsSucked * 1, actions.get(state), 0.6, 0);

				if (justCaptured) {
					qmap.rewardAction(lastAction.get(acg), 30.0, actions.get(state), 0.7, 0.3);
					captureCount.put(acg,sensors.generators.get(acg));
				}

				if (verbose) {
					System.out.println("Last State for " + acg.toString() );
					System.out.println(lastState.get(acg).representation());
					System.out.println("Updated Last Action: " + qmap.getQRepresentation());
				}
			} 

			qmap = actions.get(state);

			if (verbose) {
				System.out.println("This State for Tower " + acg.toString() );
				System.out.println(thisState.get(acg).representation());
			}
			// find the 'right' thing to do, and do it.
			AgentAction bestAction = qmap.findBestAction(verbose, lastAction.get(acg));
			bestAction.doAction(acg);

			// finally, store our action so we can reward it later.
			lastAction.put(acg, bestAction);

		}
	}


	/**
	 * This inner class simply helps to associate actions with utility values
	 */
	static class QMap {
		static Random RN = new Random();

		private double[] utility; 		// current utility estimate
		private int[] attempts;			// number of times action has been tried
		private AgentAction[] actions;  // potential actions to consider

		public QMap(AgentAction[] potential_actions) {

			actions = potential_actions.clone();
			int len = actions.length;

			utility = new double[len];
			attempts = new int[len];
			for(int i = 0; i < len; i++) {
				utility[i] = 0.0;
				attempts[i] = 0;
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
			
			double percSame = (posMoves.size() * 1.0 / (actions.length - 1));
			if (percSame == 1 || utility[maxi] < 0){
				int which = RN.nextInt(actions.length);
				if (verbose)
					System.out.println( " -- Doing Random (" + which + ")!!");

				return actions[which];
			}
			else {
				//try to match last action
				/*
				for(int a = 0; a < posMoves.size(); a++){
					if(actions[posMoves.get(a)] == lastAct){
						if(verbose) System.out.println( " -- Matched last move! #" + posMoves.get(a));
						return lastAct;
					}
				}*/

				//get random of the max actions
				int whichMax = RN.nextInt(maxcount);
				if (verbose)
					System.out.println( " -- Doing Best! #" + whichMax);

				for (i = 0; i < utility.length; i++) {
					if (utility[i] == utility[maxi]) {
						if (whichMax == 0) return actions[i];
						whichMax--;
					}
				}
				return actions[maxi];
			}
		}

		public void rewardAction(AgentAction a, double value, QMap nextMap, double learnRate, double discount) {
			int i;
			for (i = 0; i < actions.length; i++) {
				if (a == actions[i]) break;
			}
			if (i >= actions.length) {
				System.err.println("ERROR: Tried to reward an action that doesn't exist in the QMap. (Ignoring reward)");
				return;
			}

			//new qlearning algorithm
			utility[i] = utility[i] + learnRate * (value + (discount * nextMap.maxQ()) - utility[i]);
			//utility[i] = utility[i] + learnRate * value;
		}


		public String getQRepresentation() {
			StringBuffer sb = new StringBuffer(80);

			for (int i = 0; i < utility.length; i++) {
				sb.append(String.format("%.2f  ", utility[i]));
			}
			return sb.toString();

		}

	}
}