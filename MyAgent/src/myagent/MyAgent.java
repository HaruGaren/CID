/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myagent;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.UUID;

/**
 * Class that makes some agent tasks easier
 * @author Aaron Rodriguez Bueno
 */
public class MyAgent extends SingleAgent{
    
    /**
     * Constructor
     * @param aid The AgentID of the own Agent
     * @throws Exception 
     */
    public MyAgent(AgentID aid) throws Exception {
        super(aid);
    }
    
    /**
     * Creates the message to send
     * @param receiver The receiver of the message
     * @param performative The performative of the message
     * @param content The content of the message
     * @return The created message
     */
    private ACLMessage pack (AgentID receiver, int performative, String content){
        ACLMessage outbox = new ACLMessage();

        outbox.setSender(this.getAid());
        outbox.setReceiver(receiver);
        outbox.setPerformative(performative);
        outbox.setContent(content);
        
        return outbox;
    }
    
    /**
     * Sends the message to the receiver agent
     * @param receiver The receiver of the message
     * @param performative The performative of the message
     * @param content The content of the message
     */
    public void sendMessage(AgentID receiver, int performative, String content){
        ACLMessage outbox = pack(receiver, performative, content);
        
        send(outbox);
    }

    /**
     * Sends the message to the receiver agent (with ConversationID)
     * @param receiver The receiver of the message
     * @param performative The performative of the message
     * @param content The content of the message
     * @param cid The ConversationID
     */    
    public void sendMessage(AgentID receiver, int performative, String content, String cid){
        ACLMessage outbox = pack(receiver, performative, content);
        outbox.setConversationId(cid);

        send(outbox);
    }
  
    /**
     * Sends the message to the receiver agent (with ConversationID, and ReplyWith)
     * @param receiver The receiver of the message
     * @param performative The performative of the message
     * @param content The content of the message
     * @param cid The ConversationID
     * @param reply The ReplyWith
     */    
    public void sendMessage(AgentID receiver, int performative, String content,  String cid, String reply){
        ACLMessage outbox = pack(receiver, performative, content);
        outbox.setConversationId(cid);
        outbox.setReplyWith(reply);
        
        send(outbox);
    }
    
    /**
     * Answers a message from the receiver agent (with ConversationID, and InReplyTo)
     * @param receiver The receiver of the message
     * @param performative The performative of the message
     * @param content The content of the message
     * @param cid The ConversationID
     * @param reply The InReplyTo
     */    
    public void answerMessage(AgentID receiver, int performative, String content,  String cid, String reply){
        ACLMessage outbox = pack(receiver, performative, content);
        outbox.setConversationId(cid);
        outbox.setInReplyTo(reply);
        
        send(outbox);
    }
    
    /**
     * Generates a random ReplyWith
     * @return The ReplyWith
     */
    public String generateReplyId(){
        return UUID.randomUUID().toString().substring(0, 5);
    }
    
    /**
     * Parse the message in a Json 
     * @author Luis Castillo Vidal
     * @param content The message to parse
     * @return 
     */
         protected String logMessage(String content)  {
           String res = "{\"agent\":\""+this.getName()+"\", \"content\":{"+content+"}}";
           System.out.println(res);
           return res;
    }
         

    /**
     * Check if a String is an IP or not
     * @param ip The IP to check
     * @return true if it is an IP, false otherwise
     */
    protected boolean checkIp(String ip) {
        String [] parts = ip.split("\\.");
        boolean correct = true;
        
        if(parts.length == 4)
            for(int i = 0; i < parts.length && correct; i++){
                if(Integer.parseInt(parts[i]) < 0 || Integer.parseInt(parts[i]) > 255){
                    correct = false;
                }
            }
        else
            correct = false;
        
        return correct;
    }
    
         
}
