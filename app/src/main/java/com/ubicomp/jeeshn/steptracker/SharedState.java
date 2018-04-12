package com.ubicomp.jeeshn.steptracker;

// Singleton class used to share state between debug and user view.
public class SharedState {
    private static SharedState instance;

    private int steps;
    private int goal;

    // Restrict the constructor from being instantiated
    private SharedState(){}

    // sets the steps detected
    public void setSteps(int steps){
        this.steps=steps;
    }

    // gets the steps saved in the state
    public int getSteps(){
        return this.steps;
    }

    // gets the remaining steps
    public int getRemainingGoal(){
        return this.goal - this.steps;
    }

    // sets the step goal
    public void setGoal(int goal){
        this.goal = goal;
    }

    // Singleton instance of the class.
    public static synchronized SharedState getInstance(){
        if(instance==null){
            instance=new SharedState();
        }
        return instance;
    }
}
