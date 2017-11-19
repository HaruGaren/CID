package serverAgents;

import DiskLogger.DiskLogger;
import SubscribedAgent.SubscribedAgent;
import SubscribedAgent.SubscribedList;
import ThreatLog.ThreatLog;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.logging.Level;
import java.util.logging.Logger;
import myagent.MyAgent;
import onmessage.MessageQueue;

/**
 * Server agent class 
 * 
 * @author Aaron Rodriguez Bueno
 */
public class SERV extends MyAgent{
    
    /***************************************************
     ***************CLASS MEMBER DATA***************
    ***************************************************/
    
    //Agent states
    private static final int IDLE = 0;
    private static final int SUBSCRIBE_AGENT = 1;
    private static final int CANCEL_AGENT = 2;
    private static final int ADD_SSH_ATTACK = 3;
    private static final int CHECK_ANSWER_SSH = 4;
    private static final int PREVENT_SSH_ATTACKS = 5;
    private int state;
    
    private SubscribedList sshConversations;
    
    private String sshLog;
    private String serverLog;
    private DiskLogger dlogger;
    
    private ACLMessage messageReceived;
    
    private final int dimQueue = 1000;
    private MessageQueue subsMessages;
    private MessageQueue cancelMessages;
    private MessageQueue sshAttacksMessages;
    private MessageQueue sshAnswerMessages;
    
    /***************************************************
     ******************CLASS METHODS*****************
    ***************************************************/
    
    /**
     * Constructor
     * @param aid ID agent
     * @param sshlog The name of the ssh attacks log
     * @throws Exception 
     */
    public SERV(AgentID aid, DiskLogger dl, String serverlog, String sshlog) throws Exception {
        super(aid);
        sshConversations = new SubscribedList();
        messageReceived = new ACLMessage();
        sshLog = sshlog;
        serverLog = serverlog;
        dlogger = dl;
        
        this.subsMessages = new MessageQueue(dimQueue);
        this.cancelMessages = new MessageQueue(dimQueue);
        this.sshAttacksMessages = new MessageQueue(dimQueue);
        this.sshAnswerMessages = new MessageQueue(dimQueue);
        
        System.out.println("Iniciated SERV agent "+this.getAid());
    }
    
    /**
     * It initializes the agent
     */
    @Override
    public void init() {
        dlogger.AddObject(logMessage("\"status\":\"Starting\""));
        state = IDLE;
    }
    
    /**
     * Execution of the agent
     */
    @Override
    public void execute(){
        dlogger.AddObject(logMessage("\"status\":\"Executing\""));
        
        while(true){
            switch(state){
                case IDLE:
            {
                try {
                    stateIdle();
                } catch (InterruptedException ex) {
                    dlogger.AddObject(logMessage("\"status\":\"Exception caught in IDLE state\""));
                }
            }
                break;
                case SUBSCRIBE_AGENT:
                    stateSubscribeAgent();
                break;
                case CANCEL_AGENT:
                    stateCancelAgent();
                break;
                case ADD_SSH_ATTACK:
                    stateAddSSHAttack();
                break;
                case PREVENT_SSH_ATTACKS:
                    statePreventSSHAttacks();
                break;
                case CHECK_ANSWER_SSH:
                    stateCheckAnswerSSH();
                break;
            }
        }
    }
    
    /**
     * Finalization of the agent
     */
    @Override
    public void finalize(){
        dlogger.AddObject(logMessage("\"status\":\"Ending\""));
        super.finalize();
    }

    /**
     * Waiting for the other agents' comunications. Then, in function of the message arrived, it goes to one state or another.
     */
    private void stateIdle() throws InterruptedException {
        System.out.println("IN IDLE");
        
        while(this.subsMessages.isEmpty() &&
                this.cancelMessages.isEmpty() &&
                this.sshAttacksMessages.isEmpty() &&
                this.sshAnswerMessages.isEmpty()){
        
            Thread.sleep(500);
        }
        
        //Priority actions
        if(!this.subsMessages.isEmpty()){
            messageReceived = this.subsMessages.Pop();
            state = this.SUBSCRIBE_AGENT;
        }
        else if(!this.sshAttacksMessages.isEmpty()){
            messageReceived = this.sshAttacksMessages.Pop();
            state = this.ADD_SSH_ATTACK;
        }
        else if(!this.cancelMessages.isEmpty()){
            messageReceived = this.cancelMessages.Pop();
            state = this.CANCEL_AGENT;
        }
        else if(!this.sshAnswerMessages.isEmpty()){
            messageReceived = this.sshAnswerMessages.Pop();
            state = this.CHECK_ANSWER_SSH;
        }
        else{
            dlogger.AddObject(logMessage("\"status\":\"Empty queues, but out of the loop while queues are empty\""));
        }
    }

    /**
     * If everything is correct, subscribes into the server the contacted agent.
     */
    private void stateSubscribeAgent() {
        System.out.println("IN SUBSCRIBE AGENT");
        JsonObject message = new JsonObject();
        
        try{
            //Checking the task
            JsonObject contentMessageReceived = Json.parse(messageReceived.getContent()).asObject();
            
            if(contentMessageReceived.names().contains("task"))
                switch(contentMessageReceived.get("task").asString()){
                    case "SSH authentications":
                        
                        //Checking IP syntax
                        if(contentMessageReceived.names().contains("IP")){
                            String ip = contentMessageReceived.get("IP").asString();
                            
                            //Bad IP
                            if(!checkIp(ip)){
                                //Creating the message
                                message = new JsonObject();

                                message.add("reason", "BAD IP");

                                //Sending the message
                                this.sendMessage(messageReceived.getSender(), 
                                                            ACLMessage.NOT_UNDERSTOOD, 
                                                            message.toString());
                            }
                            //IP correct
                            else{
                                //Now we check if there is other agent with the same task and ip subscribed
                                int index = sshConversations.indexOfAgent(ip);
                                if(index == -1)
                                    index = sshConversations.indexOfAgent(messageReceived.getSender());
                                if( index != -1 ){
                                    sshConversations.removeSubscribedAgent(index);
                                }

                                //Adding the new agent
                                sshConversations.addSubscribedAgent(messageReceived.getSender());
                                SubscribedAgent sa = sshConversations.getSubscribedAgent(sshConversations.size()-1); //The last one
                                sa.generateConversationID();
                                sa.setIp(ip);
                                
                                //Creating the message
                                message = new JsonObject();

                                message.add("subscribe", "OK");

                                //Sending the message
                                this.sendMessage(messageReceived.getSender(), 
                                                            ACLMessage.INFORM, 
                                                            message.toString(),
                                                            sa.getConversationID());

                                dlogger.AddObject(logMessage("\"status\":\"Agent "+messageReceived.getSender().name+" with IP "+ip
                                        +" is now subscribed to this server in the SSH agents list\""));
                            }
                        }
                        else{
                                //Creating the message
                                message = new JsonObject();

                                message.add("reason", "NO IP");

                                //Sending the message
                                this.sendMessage(messageReceived.getSender(), 
                                                            ACLMessage.NOT_UNDERSTOOD, 
                                                            message.toString());
                        }

                    break;
                    default:
                        //Creating the message
                        message = new JsonObject();

                        message.add("reason", "BAD TASK");

                        //Sending the message
                        this.sendMessage(messageReceived.getSender(), 
                                                        ACLMessage.NOT_UNDERSTOOD, 
                                                        message.toString());
                    break;
                }
            
            else{
                //Creating the message
                message = new JsonObject();

                message.add("reason", "NO TASK");

                //Sending the message
                this.sendMessage(messageReceived.getSender(), 
                                                ACLMessage.NOT_UNDERSTOOD, 
                                                message.toString());
            }
        
        }catch(Exception ex){
            dlogger.AddObject(logMessage("\"status\":\"Error subscribing to the agent "+messageReceived.getSender().name+"\""));
            
            //Creating the message
            message = new JsonObject();

            message.add("reason", "EXCEPTION CAUGHT");

            //Sending the message
            this.sendMessage(messageReceived.getSender(), 
                                            ACLMessage.FAILURE, 
                                            message.toString());
        }
        
        state = IDLE;
    }

    /**
     * Cancels the subscribed agent if it is registered.
     */
    private void stateCancelAgent() {
        System.out.println("IN CANCEL AGENT");
        JsonObject message = new JsonObject();
        
        try{
            //Checking the task
            JsonObject contentMessageReceived = Json.parse(messageReceived.getContent()).asObject();
            
            if(contentMessageReceived.names().contains("task"))
                switch(contentMessageReceived.get("task").asString()){
                    case "SSH authentications":
                        int index = sshConversations.indexOfAgent(messageReceived.getSender());
                        if(index!=-1){
                            //Cancelling the agent's subscribe
                            String ip = sshConversations.getSubscribedAgent(index).getIp();
                            sshConversations.removeSubscribedAgent(index);

                            //Creating the message
                            message.add("cancel", "OK");

                            //Sending the message
                            this.sendMessage(messageReceived.getSender(), 
                                                        ACLMessage.AGREE, 
                                                        message.toString());

                            dlogger.AddObject(logMessage("\"status\":\"Agent "+messageReceived.getSender().name+" with IP "+ip
                                    +" successfully cancelled the subscription to this server in the SSH agents list\""));
                        }
                        else{
                            //Creating the message
                            message.add("reason", "UNREGISTERED");

                            //Sending the message
                            this.sendMessage(messageReceived.getSender(), 
                                                        ACLMessage.NOT_UNDERSTOOD, 
                                                        message.toString());
                        }
                    break;
                    default:
                        //Creating the message
                        message = new JsonObject();

                        message.add("reason", "BAD TASK");

                        //Sending the message
                        this.sendMessage(messageReceived.getSender(), 
                                                        ACLMessage.NOT_UNDERSTOOD, 
                                                        message.toString());
                    break;
                }
            else{
                //Creating the message
                message.add("reason", "NO TASK");

                //Sending the message
                this.sendMessage(messageReceived.getSender(), 
                                            ACLMessage.NOT_UNDERSTOOD, 
                                            message.toString());
            }
        
        }catch(Exception ex){
            dlogger.AddObject(logMessage("\"status\":\"Error cancelling the subscription to the agent "+messageReceived.getSender().name+"\""));
         
            //Creating the message
            message = new JsonObject();

            message.add("reason", "EXCEPTION CAUGHT");

            //Sending the message
            this.sendMessage(messageReceived.getSender(), 
                                            ACLMessage.FAILURE, 
                                            message.toString());
        }
        
        state = IDLE;
    }

    /**
     * If everything is correct, registers the possible attack.
     */
    private void stateAddSSHAttack() {
        System.out.println("IN ADDSSHATTACK");
        
        boolean ok = false;
        JsonObject message = new JsonObject();
        JsonObject contentMessageReceived = Json.parse(messageReceived.getContent()).asObject();
        
        try{
        
            //Checking if the agent is subscribed
            AgentID aid = messageReceived.getSender();
            boolean found = false;
            SubscribedAgent agent = null;

            for(int i = 0; i < sshConversations.size() && !found; i++){
                if(sshConversations.getSubscribedAgent(i).getAgentID().name.equals(aid.name)){
                    found = true;
                    agent = sshConversations.getSubscribedAgent(i);
                    agent.setReplyID(messageReceived.getReplyWith());
                }
            }

            if(!found){
                //Creating the message
                message = new JsonObject();

                message.add("reason", "UNREGISTERED");

                //Sending the message
                this.answerMessage(messageReceived.getSender(), 
                                                ACLMessage.NOT_UNDERSTOOD, 
                                                message.toString(),
                                                messageReceived.getConversationId(), 
                                                messageReceived.getReplyWith());
            }
            else{            
                //Checking conversationID
                if ( !messageReceived.getConversationId().equals(agent.getConversationID()) ){
                    //Creating the message
                    message = new JsonObject();

                    message.add("reason", "BAD CONVERSATION");

                    //Sending the message
                    this.answerMessage(messageReceived.getSender(), 
                                                    ACLMessage.NOT_UNDERSTOOD, 
                                                    message.toString(),
                                                    agent.getConversationID(), 
                                                    agent.getReplyID());
                }
                else{
                    
                    //Checking IP
                    JsonArray vector = contentMessageReceived.get("SSH IP attackers").asArray();
                    JsonArray dates;
                    JsonObject ipDates;
                    String ip;
                    ok = true;
                    
                    for(int i = 0; i < vector.size() && ok; i++){ //For every IP
                        ipDates = vector.get(i).asObject();
                        ip = ipDates.get("ip").asString();
                        
                        if (!checkIp(ip)){
                            ok = false;
                            
                            //Creating the message
                            message = new JsonObject();

                            message.add("reason", "BAD IP");

                            //Sending the message
                            this.answerMessage(messageReceived.getSender(), 
                                                ACLMessage.NOT_UNDERSTOOD, 
                                                message.toString(),
                                                agent.getConversationID(), 
                                                agent.getReplyID());
                        }
                        else{
                            dates = ipDates.get("attack dates").asArray();
                            for (JsonValue date : dates){
                                //Recording fact
                                ThreatLog sshLog = new ThreatLog(this.sshLog);
                                sshLog.addEntry(date.asString(), agent.getIp(), ip, "SSH attack");
                                
                            }
                            //dlogger.AddObject(logMessage("\"status\":\"The attacking IP "+ip+" from the agent "
                            //        +messageReceived.getSender()+" has been registered successfully"));
                            
                        }
                    }
                    
                    if (ok){
                        dlogger.AddObject(logMessage("\"status\":\"The attacking SSH IPs from the agent "
                                    +messageReceived.getSender().name+" have been registered successfully"));
                        
                        //Changing reply
                        agent.setReplyID(messageReceived.getReplyWith());

                        //Creating the message
                        message = new JsonObject();

                        message.add("registration", "OK");

                        //Sending the message
                        this.answerMessage(messageReceived.getSender(), 
                                                        ACLMessage.INFORM, 
                                                        message.toString(),
                                                        agent.getConversationID(), 
                                                        agent.getReplyID());


                    }
                }

            }
        }catch(Exception ex){
            dlogger.AddObject(logMessage("\"status\":\"Error registering SSH IP attackers from the agent"+messageReceived.getSender().name+"\""));
            
            //Creating the message
            message = new JsonObject();

            message.add("reason", "EXCEPTION CAUGHT");

            //Sending the message
            this.answerMessage(messageReceived.getSender(), 
                                            ACLMessage.FAILURE, 
                                            message.toString(),
                                            messageReceived.getConversationId(), 
                                            messageReceived.getReplyWith());
        }
        
        if(ok)
            state = PREVENT_SSH_ATTACKS;
        else
            state = IDLE;
    }
    
    /**
     * Warns the other SSH agents about the attacker SSH IPs
     */
    private void statePreventSSHAttacks(){
        System.out.println("IN PREVENT SSH ATTACKS");
        
        try{
            SubscribedAgent sa;
            JsonObject message = new JsonObject();
            JsonObject contentReceived = Json.parse(messageReceived.getContent()).asObject();
            JsonObject elementReceived;
            String ip;
            JsonArray vectorReceived = contentReceived.get("SSH IP attackers").asArray();
            JsonArray vectorToSend = new JsonArray();

            //Creating the message
            for(int i = 0; i < vectorReceived.size(); i++){ //For every IP
                elementReceived = vectorReceived.get(i).asObject();
                ip = elementReceived.get("ip").asString();
                vectorToSend.add(ip);
            }

            message.add("block IPs", vectorToSend);

            //Sending the message to the other SSH agents
            for(int i = 0; i < this.sshConversations.size(); i++){
                sa = sshConversations.getSubscribedAgent(i);
                if(!sa.getAgentID().name.equals(messageReceived.getSender().name)){
                    this.sendMessage(sa.getAgentID(), 
                                        ACLMessage.REQUEST, 
                                        message.toString(),
                                        sa.getConversationID(), 
                                        sa.generateReplyID());
                }
            }
        }catch(Exception e){
            dlogger.AddObject(logMessage("\"status\":\"Error in state PREVENT SSH ATTACKS\""));
        }
        
        state = IDLE;
    }
    
    /**
     * Checks the answer message from an SSH agent about preventing SSH attacks
     */
    private void stateCheckAnswerSSH(){
        System.out.println("IN CHECK ANSWER SSH");
        
        try{
            String content = messageReceived.getContent();
            SubscribedAgent sa;
            int index = this.sshConversations.indexOfAgent(messageReceived.getSender());
            if(index == -1){
                dlogger.AddObject(logMessage("\"status\":\"Error in state CHECK ANSWER SSH: agent "
                        +messageReceived.getSender().name+" is not subscribed\""));
            }
            else{
                sa = this.sshConversations.getSubscribedAgent(index);
                if(!content.contains("block IPs")){
                    dlogger.AddObject(logMessage("\"status\":\"Error in state CHECK ANSWER SSH: message from agent "
                        +messageReceived.getSender().name+" has bad task\""));
                }
                else{
                    String cid = messageReceived.getConversationId();
                    if(!sa.getConversationID().equals(cid)){
                        dlogger.AddObject(logMessage("\"status\":\"Error in state CHECK ANSWER SSH: agent "
                            +messageReceived.getSender().name+" sent bad ConversationID\""));
                    }
                    else{
                        String reply = messageReceived.getInReplyTo();
                        if(!sa.getReplyID().equals(reply)){
                            dlogger.AddObject(logMessage("\"status\":\"Error in state CHECK ANSWER SSH: agent "
                                +messageReceived.getSender().name+" sent bad InReplyTo\""));
                        }
                        else{
                            //Everything is correct
                        }
                    }
                }
            }
        }
        catch(Exception e){
            dlogger.AddObject(logMessage("\"status\":\"Error in state CHECK ANSWER SSH\""));        
        }
        
        state = IDLE;
    }
    
    /**
     * Manages input message queues.
     * @param msg Input message
     */
    public void onMessage(ACLMessage msg){
        String content = msg.getContent();
        if(msg.getPerformativeInt() == ACLMessage.SUBSCRIBE &&
                content.contains("SSH authentications")){
            try {
                this.subsMessages.Push(msg);
            } catch (InterruptedException ex) {
                dlogger.AddObject(logMessage("\"status\":\"Error queueing subscribing message: queue is full\""));
            }
        }
        else if(msg.getPerformativeInt() == ACLMessage.CANCEL &&
                content.contains("SSH authentications")){
            try {
                this.cancelMessages.Push(msg);
            } catch (InterruptedException ex) {
                dlogger.AddObject(logMessage("\"status\":\"Error queueing cancelling message: queue is full\""));
            }
        }
        else if(msg.getPerformativeInt() == ACLMessage.REQUEST){
            try {
                this.sshAttacksMessages.Push(msg);
            } catch (InterruptedException ex) {
                dlogger.AddObject(logMessage("\"status\":\"Error queueing attacking message: queue is full\""));
            }
        }
        else{
            try {
                this.sshAnswerMessages.Push(msg);
            } catch (InterruptedException ex) {
                dlogger.AddObject(logMessage("\"status\":\"Error queueing answering message: queue is full\""));
            }
        }
    }
}
