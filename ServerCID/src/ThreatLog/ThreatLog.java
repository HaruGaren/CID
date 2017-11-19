/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ThreatLog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Class that manages a thread log file
 * @author Aaron Rodriguez Bueno
 */
public class ThreatLog {
    private String name;
    
    /**
     * Constructor
     * @param name 
     */
    public ThreatLog(String name){
        this.name = name;
    }
    
    /**
     * Gets the file name
     * @return The file name
     */
    public String getName(){
        return name;
    }
    
    /**
     * Adds an entry (row) to the file
     * @param date The date and time of the event
     * @param ip The agent device IP 
     * @param ipAttacker The IP attacker
     * @param threatType The thread type 
     * @param comments An objection about the event
     * @return True if everything was correct, false otherwise
     */
    public boolean addEntry(String date, String ip, String ipAttacker, String threatType, String comments){
        
        try{
            PrintWriter outfile = new PrintWriter(new BufferedWriter(new FileWriter(name, true)));
            BufferedWriter out = new BufferedWriter(outfile);
            //String timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
            String toRecord="Date: "+date+", IP: "+ip+", IP attacker: "+ipAttacker+", threat:"+threatType+", comments: "+comments; 
            outfile.println(toRecord);
            //System.out.println(toRecord);
            outfile.close();
            
            return true;
        }
        catch(Exception ex){
            return false;
        }
    }

    /**
     * Adds an entry (row) to the file
     * @param date The date and time of the event
     * @param ip The agent device IP 
     * @param ipAttacker The IP attacker
     * @param threatType The thread type 
     * @return True if everything was correct, false otherwise
     */    
    public boolean addEntry(String date, String ip, String ipAttacker, String threatType){
        
        try{
            PrintWriter outfile = new PrintWriter(new BufferedWriter(new FileWriter(name, true)));
            BufferedWriter out = new BufferedWriter(outfile);
            //String timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
            String toRecord="Date: "+date+", IP: "+ip+", IP attacker: "+ipAttacker+", threat:"+threatType; 
            outfile.println(toRecord);
            //System.out.println(toRecord);
            outfile.close();
            
            return true;
        }
        catch(Exception ex){
            return false;
        }
    }
}
