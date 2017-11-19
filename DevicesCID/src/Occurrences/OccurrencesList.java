/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Occurrences;

import java.util.ArrayList;

/**
 * Class that manages an OccurrencesCounter array
 * @author Aaron Rodriguez Bueno
 */
public class OccurrencesList {
    private ArrayList<OccurrencesCounter> occurrences;
    
    /**
     * Constructor
     */
    public OccurrencesList(){
        occurrences = new ArrayList<OccurrencesCounter> ();
    }
    
    /**
     * Adds an occurrence to the list
     * @param name The IP
     * @param date The date of the first occurrence
     */
    public void addOccurrence(String name, String date){
        
        boolean found = false;
        
        for (int i = 0; i < occurrences.size() && !found; i++){
            if(occurrences.get(i).getIp().equals(name)){
                found = true;
                occurrences.get(i).incrementOccurrences();
            }
        }
        
        if(!found){
            occurrences.add(new OccurrencesCounter(name, date));
        }
    }
    
    /**
     * Adds the occurrence given to the list 
     * @param oc The occurrence
     */
    public void addOccurrence(OccurrencesCounter oc){
        occurrences.add(oc);
    }
    
    /**
     * Gets an OccurrenceCounter
     * @param index The index to get the OccurrenceCounter
     * @return The OccurrenceCounter
     */
    public OccurrencesCounter get(int index){
        return occurrences.get(index);
    }
    
    /**
     * Gets the size of the array
     * @return The size of the array
     */
    public int size(){
        return occurrences.size();
    }
    
    /**
     * Filters the array to extract the OccurrencesCounter with a number of occurrences
     * above or equal than the number given
     * @param number The minimun number of occurrences
     * @return A OccurrencesList with all the OccurrencesCounter with more or equal than 
     * the number given
     */
    public OccurrencesList occurrencesWithNumberAboveOrEqual(int number){
        OccurrencesList names = new OccurrencesList ();
        
        for (OccurrencesCounter i : occurrences){
            if(i.getNumOccurrences()>=number){
                names.addOccurrence(i);
            }
        }
        
        return names;
    }
}
