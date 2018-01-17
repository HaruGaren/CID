package deviceAgents;

import DiskLogger.DiskLogger;
import IPLogger.IpLogger;
import Occurrences.OccurrencesCounter;
import Occurrences.OccurrencesList;
import OccurrencesSearch.OccurrencesSearch;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import myagent.MyAgent;
import onmessage.MessageQueue;
import org.apache.commons.io.input.ReversedLinesFileReader;

/**
 * Device agent class to protect against SSH attacks 
 * 
 * @author Aaron Rodriguez Bueno
 */
public class SSH extends MyAgent{

    /***************************************************
     ***************CLASS MEMBER DATA***************
    ***************************************************/
    
    //Agent states
    private static final int SUBS_TO_SERVER = 0;
    private static final int LOOP = 1;
    private static final int FINALIZE = 2;
    private static final int CANCEL_SUBS = 3;
    private static final int CHECK_AUTH_LOG = 4;
    private static final int RE_ALLOW_IPS = 5;
    private static final int BAN_IPS = 6;
    private static final int SEND_IPS = 7;
    private static final int WAIT = 8;
    private static final int PREVENT_ATTACKS = 9;
    private int state;
    
    private final AgentID server;
    private final String authlogfilename;
    private String conversWithServer, replyWithServer;
    private boolean finish;
    
    private int seconds;
    private int attempts;
    private int lines;
    
    private DiskLogger dlogger; 
    private final int dimQueue = 100;
    private MessageQueue messagesQueue;
    
    private final String ips_to_send_filename = "tmp/send_ips.tmp";
    private final String ips_to_ban_filename = "tmp/ban_ips.tmp";
    private final String ips_to_reallow_filename = "tmp/reallow_ips.tmp";
    private final String tail_auth_log = "tmp/tail_auth_log.tmp";  
    private final String wait_to_reallow_filename = "tmp/wait_to_reallow_ips.tmp";
    
    private Calendar limit_wait_time;
    
    /***************************************************
     ******************CLASS METHODS*****************
    ***************************************************/
    
    /**
     * Constructor
     * @param agentID The name
     * @param serverID The server name
     * @param dl The DiskLogger
     * @param sshfilename The SSH file name
     * @param seconds The seconds to check the authentication log again
     * @param attempts The number of attemps to considerate an IP as an attacker
     * @param lines The number of lines to check in the authentication log
     * @throws Exception 
     */
    SSH(AgentID agentID, AgentID serverID, DiskLogger dl, String sshfilename, int seconds, int attempts, int lines) throws Exception {
        super(agentID);
        server = serverID;
        this.seconds = seconds;
        this.attempts = attempts;
        this.lines = lines;
        this.authlogfilename = sshfilename;
        this.dlogger = dl;
        this.messagesQueue = new MessageQueue(dimQueue);
        System.out.println("Iniciated SSH agent "+this.getAid());
    }

    /**
     * It initializes the agent
     */
    @Override
    public void init() {
        dlogger.AddObject(logMessage("\"status\":\"Starting\""));
        state = SUBS_TO_SERVER;
        finish = false;
        conversWithServer = null;
        
        Date current_time = Calendar.getInstance().getTime();             
        Date next_time = new Date(current_time.getTime()+seconds*1000);          
        limit_wait_time = Calendar.getInstance();
        limit_wait_time.setTime(next_time);
    }
    
    /**
     * Execution of the agent
     */
    @Override
    public void execute(){
        dlogger.AddObject(logMessage("\"status\":\"Executing\""));
        while(!finish){
            switch(state){
                case SUBS_TO_SERVER:
                    try {
                        stateSubsToServer();
                    } catch (Exception ex) {
                        state = CANCEL_SUBS;
                        dlogger.AddRecord(logMessage("\"status\":\"An error occurred in the SUBS_TO_SERVER state\""));
                    } 
                break;
                case CHECK_AUTH_LOG:
                    try {
                        stateCheckAuthLog();
                    } catch (IOException ex) {
                        state = CANCEL_SUBS;
                        dlogger.AddRecord(logMessage("\"status\":\"An error occurred in the CHECK_AUTH_LOG state\""));
                    }
                break;
                case RE_ALLOW_IPS:
                    try {
                        stateReAllowIPs();
                    } catch (IOException ex) {
                        state = CANCEL_SUBS;
                        dlogger.AddRecord(logMessage("\"status\":\"An error occurred in the RE_ALLOW_IPS state\""));
                    }
                break;
                case BAN_IPS:
                    try {
                        stateBanIPs();
                    } catch (IOException ex) {
                        state = CANCEL_SUBS;
                        dlogger.AddRecord(logMessage("\"status\":\"An error occurred in the BAN_IPS state\""));
                    }
                break;
                case SEND_IPS:
                    try {
                        stateSendIPs();
                    } catch (Exception ex) {
                        state = CANCEL_SUBS;
                        dlogger.AddRecord(logMessage("\"status\":\"An error occurred in the SEND_IPS state\""));
                    }
                break;
                case WAIT:
                    try {
                        stateWait();
                    } catch (InterruptedException ex) {
                        state = CANCEL_SUBS;
                        dlogger.AddRecord(logMessage("\"status\":\"An error occurred in the WAIT state\""));
                    }
                break;
                case PREVENT_ATTACKS:
                    try {
                        statePreventAttacks();
                    } catch (Exception ex) {
                        state = CANCEL_SUBS;
                        dlogger.AddRecord(logMessage("\"status\":\"An error occurred in the PREVENT ATTACKS state\""));
                    }
                break;
                case CANCEL_SUBS:
            {
                try {
                    stateCancelSubs();
                } catch (InterruptedException ex) {
                    state = FINALIZE;
                    dlogger.AddRecord(logMessage("\"status\":\"An error occurred in the CANCEL SUBS state\""));
                }
            }
                break;
                case FINALIZE:
                    stateFinalize();
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
     * The subscription to the server agent
     * @throws InterruptedException
     * @throws MalformedURLException
     * @throws UnknownHostException 
     */
    private void stateSubsToServer() throws InterruptedException, MalformedURLException, UnknownHostException {
        System.out.println("IN SUBS TO SERVER");
        //It subscribes to the Server Agent
        
        //Creating the message
        JsonObject message = new JsonObject();

        message.add("task", "SSH authentications");
        
        //Obtaining this IP
        String currentIp = new String();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            boolean found = false;
            while (interfaces.hasMoreElements() && !found) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                
                while(addresses.hasMoreElements() && !found) {
                    InetAddress addr = addresses.nextElement();
                    currentIp = addr.getHostAddress();
                    
                    if(checkIp(currentIp)){
                        found = true;
                    }
                }
            }
        } catch (SocketException e) {
            dlogger.AddObject(logMessage("\"status\":\"Error obtaining this device IP\""));
            state = CANCEL_SUBS;
            return;
        }
        //InetAddress iAddress = InetAddress.getLocalHost();
        //String currentIp = iAddress.getHostAddress();
        
        message.add("IP",currentIp);
        
        //Sending the message
        this.sendMessage(server, 
                    ACLMessage.SUBSCRIBE, 
                    message.toString());
        
        state = FINALIZE;
        
        //Receiving the answer
        while(this.messagesQueue.isEmpty())
            Thread.sleep(500);
        
        ACLMessage messageReceived = messagesQueue.Pop();
        
        //In function of the message, we will go to a state or to another
        if(messageReceived.getPerformativeInt() == ACLMessage.INFORM &&
                messageReceived.getContent().contains("subscribe")){
            conversWithServer = messageReceived.getConversationId();
            dlogger.AddObject(logMessage("\"status\":\"Subscribing successful with the server "+this.server.name+"\""));
            
            state = CHECK_AUTH_LOG;
        }
        else{
            dlogger.AddObject(logMessage("\"status\":\"Subscribing to the server "+this.server+" failed\""));
            state = FINALIZE;
        }
        
    }

    
    /**
     * The cancel to the subscription to the server agent
     */
    private void stateCancelSubs() throws InterruptedException {
        System.out.println("IN CANCEL SUBS");
        //Sending the cancel message
        //Creating the message
        JsonObject message = new JsonObject();

        message.add("task", "SSH authentications");

        //Sending the message
        this.sendMessage(server, 
                    ACLMessage.CANCEL, 
                    message.toString());
        
        //Waiting for an answer
        while(this.messagesQueue.isEmpty())
            Thread.sleep(500);
        
        ACLMessage messageReceived = messagesQueue.Pop();
        
        while(!(messageReceived.getPerformativeInt() == ACLMessage.AGREE && 
                messageReceived.getContent().contains("cancel")) &&
                !messageReceived.getContent().contains("not cancelled reason")){   //It is not a cancel message. We ignore it
            while(this.messagesQueue.isEmpty())
                Thread.sleep(500);
        
            messageReceived = messagesQueue.Pop();
        }
        
        try{
            if(messageReceived.getPerformativeInt() == ACLMessage.AGREE){
                dlogger.AddObject(logMessage("\"status\":\"The subscribe with the server "+server.name+" has been successfully cancelled\""));
            }
            else{                
                dlogger.AddObject(logMessage("\"status\":\"Cancelling the subscribe with the server "+server.name+" failed\""));
            }
        }
        catch(Exception ex){
            dlogger.AddObject(logMessage("\"status\":\"Error receiving the cancelling answer from the server "+this.server.name+"\""));
        }
        
        //Going to the final state
        state = FINALIZE;
    }

    /**
     * It finalizes the agent
     */
    private void stateFinalize() {
        finish = true;
    }

    /**
     * It checks the authentication log
     * @throws IOException 
     */     
    private void stateCheckAuthLog() throws IOException {
        System.out.println("IN CHECK AUTH LOG");
        
        boolean ok = true;
        
        /*
        //PREVIOUS CODE, BUT SOMETIMES DOESN'T WORK WELL
        
        File authlog = new File(this.authlogfilename);
        ReversedLinesFileReader reverse = new ReversedLinesFileReader(authlog);
        boolean beginning = false;
        String line;
        try(FileWriter fw = new FileWriter(tail_auth_log, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            PrintWriter writer = new PrintWriter(this.tail_auth_log);
            writer.print("");   //File now empty
            
            for(int i = 0; i < this.lines && !beginning; i++){
                line = reverse.readLine();
                if(line != null){
                    out.println(line);
                }
                else{
                    beginning = true;
                }
            }
            writer.close();
            out.close();
            bw.close();
            fw.close();
        } catch (Exception e) {
            dlogger.AddObject(logMessage("\"status\":\"Error creating/filling the file "+this.tail_auth_log+"\""));
            //state = CANCEL_SUBS;
            //return;   
        }*/
        
        //First extract the last lines of the file
        File file = new File(this.authlogfilename);
        String content = tail(file, this.lines);
        
        //Then create/empty the new file
        file = new File(this.tail_auth_log);
        if(!file.exists()){
            try{
                file.createNewFile();
            }
            catch(Exception e){
                dlogger.AddObject(logMessage("\"status\":\"Error creating the file "+this.tail_auth_log+"\""));
                state = CANCEL_SUBS;
                return;
            }
        }
        
        //At least, fill the new file with the extracted lines
        if(ok){
            try{
                PrintWriter writer = new PrintWriter(this.tail_auth_log);
                writer.print(content);
                writer.close();
            }
            catch(Exception e){
                dlogger.AddObject(logMessage("\"status\":\"Error filling the file "+this.tail_auth_log+"\""));
                state = CANCEL_SUBS;
                return;
            }
        }
        
        //Extracting the IP which they exceed the attempt number
        OccurrencesSearch occurrences = new OccurrencesSearch(tail_auth_log);
        OccurrencesList attackerIPs = occurrences.searchOccurrences(attempts,seconds);
        
        OccurrencesList sshIPs = occurrences.searchOccurrences(1,seconds);  //For re-allowing IPs or not
        
        //Deleting ban content
        IpLogger banFile = new IpLogger(this.ips_to_ban_filename);
        ok = banFile.eraseContent();
        
        //If an IP to re-allow tried to authenticate (but failed) again
        IpLogger reallowFile = new IpLogger(this.ips_to_reallow_filename);
        IpLogger toSendFile = new IpLogger(this.ips_to_send_filename);
        IpLogger waitToReallowFile = new IpLogger(this.wait_to_reallow_filename);
        OccurrencesCounter oc;
       
        for(int i = 0; i < sshIPs.size() && ok; i++){
            oc = sshIPs.get(i);
           
            //Don't re-allow IPs that they tried to authenticate (but failed) again
            if(reallowFile.isIP(oc.getIp())){
                ok = reallowFile.deleteIP(oc.getIp());
                
                if(ok)
                    ok = waitToReallowFile.addRegistry(oc.getIp(), oc.getDate());
                
                /*if(ok)
                    ok = toSendFile.addRegistry(oc.getIp(), oc.getDate());
                
                if(ok)
                    ok = banFile.addRegistry(oc.getIp(), oc.getDate());*/
            }
        }
        
        //Including IP attackers in banned and to send IPs files   
        boolean found;
        ArrayList<ArrayList<String>> contentWait = waitToReallowFile.getContent();
        if(ok)
            for(int i = 0; i < attackerIPs.size() && ok; i++){
                oc = attackerIPs.get(i);
                found = false;
                
                //We just ban that IP if it didn't attack (to not re-ban)
                for(int j = 0; j < contentWait.size() && !found; j++){
                    if(contentWait.get(j).get(0).equals(oc.getIp()))
                        found = true;
                }
                if(!found)
                    ok = banFile.addRegistry(oc.getIp(), oc.getDate());

                if(ok)
                    ok = toSendFile.addRegistry(oc.getIp(), oc.getDate());
            }
        
        if(ok)
            state = RE_ALLOW_IPS;
        else{            
            dlogger.AddRecord(logMessage("\"status\":\"Error checking the authentication log file "+this.authlogfilename+"\""));
            state = CANCEL_SUBS;
        }
    }

    /**
     * Re-allows the IPs banned last run/loop that they didn't try to attempt again
     * @throws IOException 
     */
    private void stateReAllowIPs() throws IOException {
        System.out.println("IN REALLOWIPS");
        IpLogger re_allow_ips = new IpLogger(this.ips_to_reallow_filename);
        IpLogger wait_to_re_allow = new IpLogger(this.wait_to_reallow_filename);
        ArrayList<ArrayList<String>> content_re_allow = re_allow_ips.getContent();
        ArrayList<ArrayList<String>> content_wait = wait_to_re_allow.getContent();
        boolean ok = true;
        String reason = "";
        
        //Re-allowing IPs
        while(content_re_allow.size() > 0 && ok){            
            String cmd = "/sbin/iptables -D INPUT -s "+content_re_allow.get(0).get(0)+" -j DROP";
            Process pb = Runtime.getRuntime().exec(cmd);
            
            ok = re_allow_ips.deleteIndex(0);
            
            if(ok)
                content_re_allow = re_allow_ips.getContent();
            else
                reason = "failed deleting an IP from the reallow IPs file content";
        }
        
        //Extracting the IPs from wait to re-allow file to the re-allow file
        while(content_wait.size() > 0 && ok){
            ok = re_allow_ips.addIP(content_wait.get(0).get(0));
            if(ok){
                ok = wait_to_re_allow.deleteIndex(0);            
            
                if(ok)
                    content_wait = wait_to_re_allow.getContent();
                else
                    reason = "failed deleting an IP from the wait-to-reallow IPs file content";              
            }
            else
                reason = "failed adding an IP to the reallow IPs file";
                
        }
        
        if(ok)
            state = BAN_IPS;
        else{
            dlogger.AddRecord(logMessage("\"status\":\"Error re-allowing banned IPs: "+reason+"\""));
            state = CANCEL_SUBS;
        }
    }

    /**
     * Ban IPs thanks to iptables that they attempts x times in a shorter time given
     * @throws IOException 
     */
    private void stateBanIPs() throws IOException {
        System.out.println("IN BANIPS");
        IpLogger banned_ips = new IpLogger(this.ips_to_ban_filename);
        IpLogger re_allow_ips = new IpLogger(this.ips_to_reallow_filename); //Initially, it should be empty
        ArrayList<ArrayList<String>> content = banned_ips.getContent();
        boolean ok = true;
        String reason = "";
        
        while(content.size() > 0 && ok){
            String cmd = "/sbin/iptables -I INPUT -s "+content.get(0).get(0)+" -j DROP";
            Process pb = Runtime.getRuntime().exec(cmd);
            
            //Now we add the IP to the re-allow file 
            ok = re_allow_ips.addIP(content.get(0).get(0));
            
            if (ok){
                ok = banned_ips.deleteIndex(0);   
                
                if(ok)
                    content = banned_ips.getContent();
                else
                    reason = "failed deleting an IP from the banned IPs file content";
            }
            else{
                reason = "failed adding IPs to reallow file";
            }
        }
        
        if(ok)
            state = SEND_IPS;
        else{
            dlogger.AddRecord(logMessage("\"status\":\"Error banning IPs: "+reason+"\""));
            state = CANCEL_SUBS;
        }
    }

    /**
     * Send IPs that they tried to authenticate by SSH with the conditions given
     * in this run (and in the last execution, if there were IPs that they wont be sent)
     * @throws IOException 
     */
    private void stateSendIPs() throws IOException, InterruptedException {
        System.out.println("IN SENDIPS");
        IpLogger ips_to_send = new IpLogger(this.ips_to_send_filename);
        ArrayList<ArrayList<String>> content = ips_to_send.getContent();
        boolean ok = true;
        JsonObject message = new JsonObject();
        JsonObject ipDates;
        JsonArray vector = new JsonArray();
        JsonArray dates;
        
        //We just send a message to the server agent if we have IPs to send
        if(content.size()>0){
            
            //First we made the message
            for(int i = 0; i < content.size(); i++){
                ipDates = new JsonObject();
                dates = new JsonArray();


                for(int j = 1; j < content.get(i).size(); j++){
                    dates.add(content.get(i).get(j));
                }

                ipDates.add("ip", content.get(i).get(0));
                ipDates.add("attack dates", dates);

                vector.add(ipDates);
            }

            message.add("SSH IP attackers", vector);
            this.replyWithServer = this.generateReplyId();

            //Sending the message
            this.sendMessage(server, 
                        ACLMessage.REQUEST, 
                        message.toString(),
                        this.conversWithServer,
                        this.replyWithServer);

            
            //Receiving the answer
            while(this.messagesQueue.isEmpty())
                Thread.sleep(500);

            ACLMessage messageReceived = messagesQueue.Pop();
            String contentReceived = messageReceived.getContent();
            
            while(!contentReceived.contains("not registered reason") &&
                    !contentReceived.contains("registration")){ //We omit other messages that they are not the answer 
                messagesQueue.Push(messageReceived);
                messageReceived = messagesQueue.Pop();
                contentReceived = messageReceived.getContent();
            }

            if(messageReceived.getPerformativeInt()!=ACLMessage.INFORM ||
                    !messageReceived.getConversationId().equals(this.conversWithServer) ||
                    !messageReceived.getInReplyTo().equals(this.replyWithServer)){
                ok = false;
                dlogger.AddObject(logMessage("\"status\":\"Unexpected message from the server "+server.name+"\""));
            }
            else{
                ok = ips_to_send.eraseContent();
                dlogger.AddObject(logMessage("\"status\":\"Successful sending IPs to the server agent "+server.name+"\""));
            }
        }    
        
        if(ok)
            state = WAIT;
        else{
            dlogger.AddRecord(logMessage("\"status\":\"Error sending IPs to the server "+this.server.name+"\""));
            state = CANCEL_SUBS;
        }
    }

    /**
     * Waits a time to check the authentication log again
     * @throws InterruptedException 
     */
    private void stateWait() throws InterruptedException {
        System.out.println("IN WAIT");
        
        Date current_time = Calendar.getInstance().getTime();
        
        //We wait until there is a message or until the time ends
        while(messagesQueue.isEmpty() && current_time.before(limit_wait_time.getTime())){
            Thread.sleep(1000);
            
            current_time = Calendar.getInstance().getTime();
        }
        
        if(!current_time.before(limit_wait_time.getTime())){    //The waiting finished
            //We change the time for the next run           
            Date next_time = new Date(current_time.getTime()+seconds*1000);          
            limit_wait_time = Calendar.getInstance();
            limit_wait_time.setTime(next_time);

            state = CHECK_AUTH_LOG;
        }
        else{   //There is a message (or more than one)
            state = PREVENT_ATTACKS;
        }
    }
    
    /**
     * Bans the attacker IPs sent, and answers the prevention message
     */
    private void statePreventAttacks() throws InterruptedException, IOException{
        System.out.println("IN PREVENT ATTACKS");
        
        boolean ok = true;
        
        ACLMessage messageReceived = this.messagesQueue.Pop();
        JsonObject contentMessageReceived = Json.parse(messageReceived.getContent()).asObject();
        JsonArray ipsReceived;
        String ip;
        JsonObject message = new JsonObject();
        ArrayList <String> ips = new ArrayList();
        
        //First we analyze the message and extract the IPs
        if(!contentMessageReceived.names().contains("block IPs")){
            message.add("not prevented reason","BAD TASK");

            //Sending the message
            this.answerMessage(server, 
                        ACLMessage.NOT_UNDERSTOOD, 
                        message.toString(),
                        this.conversWithServer,
                        messageReceived.getReplyWith());
            
            dlogger.AddRecord(logMessage("\"status\":\"Error receiving preventing attack message from server agent "
                    +this.server.name+": bad task\""));
            
            ok = false;
        }
        else{
            ipsReceived = contentMessageReceived.get("block IPs").asArray();
                
            if(messageReceived.getPerformativeInt() != ACLMessage.REQUEST){
                message.add("not prevented reason","BAD PERFORMATIVE");

                //Sending the message
                this.answerMessage(server, 
                            ACLMessage.NOT_UNDERSTOOD, 
                            message.toString(),
                            this.conversWithServer,
                            messageReceived.getReplyWith());
                
                dlogger.AddRecord(logMessage("\"status\":\"Error receiving preventing attack message from server agent "
                    +this.server.name+": bad performative\""));
                
                ok = false;
            }
            else{
                if(!messageReceived.getConversationId().equals(this.conversWithServer)){
                    message.add("not prevented reason","BAD CONVERSATION");

                    //Sending the message
                    this.answerMessage(server, 
                                ACLMessage.NOT_UNDERSTOOD, 
                                message.toString(),
                                this.conversWithServer,
                                messageReceived.getReplyWith());
                    
                    dlogger.AddRecord(logMessage("\"status\":\"Error receiving preventing attack message from server agent "
                        +this.server.name+": bad conversation\""));
                    
                    ok = false;
                }
                else{
                    for(int i = 0; i < ipsReceived.size() && ok; i++){
                        if(ok){
                            ip = ipsReceived.get(i).asString();

                            if(!this.checkIp(ip)){
                                ok = false;
                            }
                            else{
                                ips.add(ip);
                            }
                        }
                    }
                    
                    if(!ok){
                        message.add("not prevented reason","BAD IP");

                        //Sending the message
                        this.answerMessage(server, 
                                    ACLMessage.NOT_UNDERSTOOD, 
                                    message.toString(),
                                    this.conversWithServer,
                                    messageReceived.getReplyWith());
                        
                        dlogger.AddRecord(logMessage("\"status\":\"Error receiving preventing attack message from server agent "
                            +this.server.name+": bad IP\""));
                    }
                }
            }
        }
        
        
        //Now we ban the IPs (even if the message to the server failed)
        if(ok){
            IpLogger wait_to_re_allow_ips = new IpLogger(this.wait_to_reallow_filename);
            IpLogger reallowFile = new IpLogger(this.ips_to_reallow_filename);
            
            for(int i = 0; i < ips.size() && ok; i++){
                if(!reallowFile.isIP(ips.get(i))){
                    String cmd = "/sbin/iptables -I INPUT -s "+ips.get(i)+" -j DROP";
                    Process pb = Runtime.getRuntime().exec(cmd);
                }
                else{
                    ok = reallowFile.deleteIP(ips.get(i));
                }
                    
                if(ok){
                    ok = wait_to_re_allow_ips.addIP(ips.get(i));
                }
            }
        }
         
        if(ok){
            message.add("block IPs","ok");

            //Sending the message
            this.answerMessage(server, 
                        ACLMessage.INFORM, 
                        message.toString(),
                        this.conversWithServer,
                        messageReceived.getReplyWith());

            dlogger.AddRecord(logMessage("\"status\":\"Successful preventing SSH attacks received from the server\""));

        }
        
        if(ok)
            state = WAIT;
        else
            state = CANCEL_SUBS;
    }
    
    /**
     * Saves the SERV agent messages
     * @param msg The received message
     */
    @Override
    public void onMessage(ACLMessage msg){
        try {
            messagesQueue.Push(msg);
        } catch (InterruptedException ex) {
            dlogger.AddRecord(logMessage("\"status\":\"Error queueing message: queue is full\""));
        }
    }

    /**
     * Saves the X last lines in a file to a String
     * Reference: https://stackoverflow.com/questions/686231/quickly-read-the-last-line-of-a-text-file
     * @param file The file to extract the lines
     * @param lines The number of lines to extract
     * @return The last lines of the file
     */
    private String tail( File file, int lines) {
        java.io.RandomAccessFile fileHandler = null;
        try {
            fileHandler = 
                new java.io.RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0;

            for(long filePointer = fileLength; filePointer != -1; filePointer--){
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();

                 if( readByte == 0xA ) {
                    if (filePointer < fileLength) {
                        line = line + 1;
                    }
                } else if( readByte == 0xD ) {
                    if (filePointer < fileLength-1) {
                        line = line + 1;
                    }
                }
                if (line >= lines) {
                    break;
                }
                sb.append( ( char ) readByte );
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } catch( java.io.FileNotFoundException e ) {
            e.printStackTrace();
            return null;
        } catch( java.io.IOException e ) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (fileHandler != null )
                try {
                    fileHandler.close();
                } catch (IOException e) {
                }
        }
    }
}
