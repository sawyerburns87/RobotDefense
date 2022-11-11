# RobotDefense

### Description
- 

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
- #### Part 2
  - Changing the method of tie breaking for equally valued actions so that the agent is more likely to keep doing the same action if two or more have the same action value
    - All changes are still in *munchersOne.java*
    - When there are multiple actions that are tied *for* the best utility, put them into an ArrayList
    
