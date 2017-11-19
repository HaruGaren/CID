/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Occurrences;

/**
 * Class that counts the number of occurrences of a IP (and the first time)
 * @author Aaron Rodriguez Bueno
 */
public class OccurrencesCounter {
    
    private String ip;
    private int numOccurrences;
    private String date;
    
    /**
     * Default constructor
     */
    public OccurrencesCounter(){
        ip = new String();
        numOccurrences = 0;
        date = new String();
    }
    
    /**
     * Copy constructor
     * @param o The OccurrencesNumber to copy
     */
    public OccurrencesCounter(OccurrencesCounter o){
        this.ip = o.ip;
        this.date = o.date;
        this.numOccurrences = o.numOccurrences;
    }
    
    /**
     * Constructor
     * @param name The IP
     * @param date The time of the first occurrence
     */
    public OccurrencesCounter(String name, String date){
        this.ip = name;
        this.numOccurrences = 1;
        this.date = date;
    }
    
    /**
     * Constructor
     * @param name The IP
     * @param date The time of the first occurrence
     * @param numOccurrences The number of occurrences
     */
    public OccurrencesCounter(String name, String date, int numOccurrences){
        this.ip = name;
        this.numOccurrences = numOccurrences;
        this.date = date;
    }
    
    /**
     * To get the IP
     * @return the IP
     */
    public String getIp(){
        return ip;
    }
    
    /**
     * To set the IP
     * @param name The IP
     */
    public void setName(String name){
        this.ip = name;
    }
    
    /**
     * To get the number of occurrences
     * @return The number of occurrences
     */
    public int getNumOccurrences(){
        return this.numOccurrences;
    }
    
    /**
     * To set the number of occurrences
     * @param numOccurrences The number of occurrences
     */
    public void setNumOccurrences(int numOccurrences){
        this.numOccurrences = numOccurrences;
    }
    
    /**
     * To get the time of the first occurrence
     * @return The date
     */
    public String getDate(){
        return date;
    }
    
    /**
     * To set the time of the first occurrence
     * @param date The time
     */
    public void setDate(String date){
        this.date = date;
    }
    
    /**
     * To increment the occurrences number
     */
    public void incrementOccurrences(){
        this.numOccurrences++;
    }
}
