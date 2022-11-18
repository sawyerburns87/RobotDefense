# RobotDefense

### Description
- #### How to run
  - Ensure that you are in the correct directory that contains the project itself
  - First, build the agent: javac -cp rd881.jar *.java
  - Then run the jar file itself: java -jar rd881.jar

### What has been changed
- #### Part One
  - All of these changes can be found in *munchersOne.java*
  - Modifying the agent so that they can turn off vaccumms when necessary
    - Actions were added to the agent so that it allows power 0 to be used in *any* direction
    - In the *step function*, the last state's crystal usage is cached and negatively rewards the action *if* it is using too much power
      - In turn, the generators will incrementally want to use less power or no power
  - Modifying the agent so that it considers a wider variety of actions
    - Instead of using max power for *all* actions, we use power 2 and power 4 for *all* directions
    - The agent should already have the capability to learn which to use and when
  - Modifying the reward function so that the agent performs well on test maps
    - In order to get the agent to perform well on the other maps, we did two things: penalizing the agent if it didn't capture the bug by a negative value, and editing the reward value for the other maps
    
- #### Part 2
  - Changing the method of tie breaking for equally valued actions so that the agent is more likely to keep doing the same action if two or more have the same action value
    - All changes are still in *munchersOne.java*
    - When there are multiple actions that are tied *for* the best utility, put them into an ArrayList
    - Iterate through the arrayList and test if *any* are equal to the current action, if so, pick that action
  - Implementing Q learning
    - In order to implement Q learning, we had to edit the basic learning that was already provided
    - First, *rewardAction* was edited to allow the access of the next map
    - Next, we implemented gamma and alpha values to be used for calculations later down the line
    - The attempts array was deleted in order for the new equation to be implemented
    - In order to find the maximum Q in the next state, we implemented a new function named maxQ
    - Finally, the new q learning algorithm was created: *utility[i] = utility[i] + alpha * (value + (gamma * nextMap.maxQ()) - utility[i])*
    
- ### Part 3
  - Modifying the agent to perform directed reasoning
    - 
  - Cell Contents
    - Edited the getContents function so that the state is simplified to only look at a single bug
    - If there is more than one bug in a single cell, then the priority list in order is: Scara bug, Starlight Bug, and Squirm bug
