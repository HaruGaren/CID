/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SubscribedAgent;

import es.upv.dsic.gti_ia.core.AgentID;
import java.util.UUID;

/**
 * Class that registers the IP, ConversationID and Reply with other agent
 * @author Aaron Rodriguez Bueno
 */
public class SubscribedAgent {
    private AgentID agent;
    private String ip;
    private String conversationID;
    private String replyID;
    
    /**
     * Constructor
     * @param aid The agent ID
     */
    public SubscribedAgent(AgentID aid){
        this.agent = aid;
        this.conversationID = "";
        this.replyID = "";
        this.ip = "";
    }
    
    /**
     * Constructor to fill everything
     * @param aid The AgentIP
     * @param cid The ConversationID with that agent
     * @param rid The InReplyTo with that agent
     * @param ip The IP of that agent
     */
    public SubscribedAgent(AgentID aid, String cid, String rid, String ip){
        this.agent = aid;
        this.conversationID = cid;
        this.replyID = rid;
        this.ip = ip;
    }
    
    /**
     * Gets if the ConversationID was filled
     * @return true if it was filled, false otherwise
     */
    public boolean hasConversationID(){
        return !conversationID.equals("");
    }
    
    /**
     * Fills the ConversationID with a random String
     * @return The ConversationID
     */
    public String generateConversationID(){
        if(conversationID.equals("")){
            this.conversationID = UUID.randomUUID().toString().substring(0, 5);
        }
        
        return getConversationID();
    }
    
    /**
     * Fills the ReplyWith with a random String
     * @return The ReplyWith
     */
    public String generateReplyID(){
        replyID = UUID.randomUUID().toString().substring(0, 5);
        return getReplyID();
    }

    /**
     * Sets the ReplyID for a new one
     * @param rid The new ReplyID
     */
    public void setReplyID(String rid){
        replyID = rid;
    }
    
    /**
     * Sets the ConversationID for a new one
     * @param cid The new ConversationID
     */
    public void setConversationID(String cid){
        if (conversationID.equals(""))
            this.conversationID = cid;
    }
    
    /**
     * Sets a new IP
     * @param ip The new IP
     */
    public void setIp(String ip){
        if (this.ip.equals(""))
            this.ip = ip;
    }
    
    /**
     * Gets the AgentID
     * @return The AgentID
     */
    public AgentID getAgentID(){
        return this.agent;
    }
    
    /**
     * Gets the ConversationID
     * @return The ConversationID
     */    
    public String getConversationID() {
       // if (conversationID.equals(""))
       //     generateConversationID();
        return this.conversationID;
    }
    
    /**
     * Gets the ReplyID
     * @return The ReplyID
     */
    public String getReplyID(){
        return this.replyID;
    }
    
    /**
     * Gets the IP
     * @return The IP
     */
    public String getIp(){
        return this.ip;
    }
}
