/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SubscribedAgent;

import es.upv.dsic.gti_ia.core.AgentID;
import java.util.ArrayList;

/**
 * Class that manages a list of SubscribedAgent.
 * @author Aaron Rodriguez Bueno
 */
public class SubscribedList {
    private ArrayList<SubscribedAgent> agents;
    
    /**
     * Default constructor
     */
    public SubscribedList(){
        agents = new ArrayList<SubscribedAgent>();
    }
 
    /**
     * Gets the index of the agent with the IP given
     * @param ip The IP to find
     * @return The index of that agent
     */
    public int indexOfAgent(String ip){
        int index = -1;
        int longitud = agents.size();
        int i = 0;
        
        while(i < longitud && index == -1){
            if(agents.get(i).getIp().equals(ip))
                index = i;
            
            i++;
        }
        
        return index;
    }
    
    /**
     * Gets the index of the agent given
     * @param aid The AgentID
     * @return The index of that agent
     */
    public int indexOfAgent(AgentID aid){
        int index = -1;
        int longitud = agents.size();
        
        for(int i = 0; i < longitud && index == -1; i++){
            if(agents.get(i).getAgentID().name.equals(aid.name))
                index = i;
        }
        
        return index;
    }
    
    /**
     * Gets the AgentID and its features of a index
     * @param index The index of the agent
     * @return The agent and its features
     */
    public SubscribedAgent getSubscribedAgent(int index){
        return agents.get(index);
    }
    
    /**
     * Remove a SubscribedAgent in the index given
     * @param index The index of the agent to remove
     */
    public void removeSubscribedAgent(int index){
        if (index >= 0 && index < agents.size()){
            agents.remove(index);
        }
        
    }
    
    /**
     * Remove a SubscribedAgent with the AgentID given
     * @param aid The AgentID to remove
     */
    public void removeSubscribedAgent(AgentID aid){
        int index = indexOfAgent(aid);
        if (index != -1){
            agents.remove(index);
        }
    }
    
    /**
     * Creates a SubscribedAgent just with the AgentID
     * @param aid The AgentID
     */
    public void addSubscribedAgent(AgentID aid){
        agents.add(new SubscribedAgent(aid));
    }
    
    /**
     * Adds a SubscribedAgent given
     * @param ca The SubscribedAgent
     */
    public void addSubscribedAgent(SubscribedAgent ca){
        agents.add(ca);
    }
    
    /**
     * Gets the size of the SubscribedAgent array
     * @return The size of the array
     */
    public int size(){
        return agents.size();
    }
}
