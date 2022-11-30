# Artificial Intelligence Group Project

### Description
- #### How to run
  - Ensure that you are in the correct directory that contains the project itself
  - First, build the agent: javac -cp rd881.jar *.java
  - Then run the jar file itself: java -jar rd881.jar
- #### Group Members
  - John Taylor 
  - Phonethep Nakhonekhong 
  - Sawyer Burns 
  - Austin Lane

### What has been changed
- #### Part One
  - All of these changes can be found in *munchersOne.java*
  - 1a.) Modifying the agent so that they can turn off vaccumms when necessary
    - For all directions, power level 0 was added to the acitons list. This gives us the ability to reward for no power usage in the correct situation.
    - In the *step function*, the last state is checked to see if it was an *empty state*, if it was an empty state and it was using no power - reward. If it was not an empty state and it was using no power - negative reward, we want to use power when bugs are in the vicinity.
      - In turn, the generators will choose to use power when there are bugs and not use power when there are no bugs.
  - 1b.) Modifying the agent so that it considers a wider variety of actions
    - Instead of using max power for *all* actions, we added ability to use power 0 (as stated above) and power 2 for *all* directions.
    - The agent should already have the capability to learn which to use and when. However, because there are 3 times more actions, the agent will take much longer to learn. Because of this we added a reward function in the *step function* that negativley rewards actions that are not pointing at the bug in its vicinity. This reward function cuts down the initial learning period significantly.
  - 1c.) Comparing the agent's performance
    - Our agent's capture-to-uncaptured ratio is considerably higher than the ratio of the LearnerOne agent, also our agent captures 3 to 4 times as many bugs overall than the LearnerOne agent. The LearnerOne agent learns to capture only one type of bug efficiently, while ours efficiently learns how to capture all three. Also, the LearnerOne captures more bugs in the beginning, but does not capture more bugs in the end; while ours takes more time to learn in the beginning to capture all 3 types of bugs and consequently captures less bugs in the beginning but captures way more bugs in the end.
  - 1d.) Modifying the reward function so that the agent performs well on test maps
    - In order to get the agent to perform well on the other maps, we did several things: make the agent prefer sides that the bug is on more often(not included in the final version to avoid an overfitted model).
    - We modificed the values of the reward functions. For the _simple1.dat_ map, we cut the reward values for power usage from 8.0 to 4.0 (lines 139 and 142 in our agent) in half. For the _simple4-4pack.map_, we doubled the the values for power usage from 8.0 to 16.0. 
    
- #### Part 2
  - All changes are still in *munchersOne.java*
  - 2a.) Changing the method of tie breaking for equally valued actions so that the agent is more likely to keep doing the same action if two or more have the same action value
    - Add 'lastAction' parameter to *findBestAction* function. 
    - When there are multiple actions that are tied *for* the best utility, put them into an ArrayList
    - Iterate through the arrayList and test if *any* are equal to the current action, if so, pick that action
  - 2b.) Implementing Q learning
    - In order to implement Q learning, we had to edit the basic learning that was already provided
    - First, *rewardAction* was edited to allow the access of the next map
    - Next, we implemented gamma and alpha values to be used for calculations later down the line
    - The attempts array was deleted in order for the new equation to be implemented
    - In order to find the maximum Q in the next state, we implemented a new function named maxQ
    - Finally, the new q learning algorithm was created: *utility[i] = utility[i] + alpha * (value + (gamma * nextMap.maxQ()) - utility[i])*
    
- ### Part 3
  - All changes are found in *munchersOne* and *stateVector* 
  - 3a.) Modifying the agent to perform directed reasoning
    - In order to solve this problem, we abstracted it into 'simplify current state into one that we've seen before'. To do this, if we are in a state where there is more than one bug, then we prioritize one of the bugs and it's like the others aren't there. Therefore, we don't need new, unknown states everytime there is another bug in the radius.
  - 3b.) Cell Contents
    - Edited the getContents function so that when there is more than 1 bug in a cell, simplify it to only look at a single bug.
    - If there is more than one bug in a single cell, then the priority list in order is: Scara bug, Starlight Bug, and Squirm bug
