/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IPLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Class that open/close/modify an IP   date1   date2   ... file
 * @author Aarón
 */
public class IpLogger {
    private String filename;
    
    /**
     * Constructor
     * @param filename 
     */
    public IpLogger(String filename) throws IOException{
        this.filename = filename;
        
        checkFile();
    }
    
    /**
     * Checks if the file can be opened. If the file doesn't exists, it will be created
     * @return true if can be readed/created, false otherwise
     * @throws IOException 
     */
    private boolean checkFile() throws IOException{
        boolean checked = true;
        
        File file = new File(filename);
        if(!file.exists() || !file.isFile()){
            try{
                //BufferedWriter bwtemp = new BufferedWriter(new FileWriter(filename));
                //bwtemp.close();
                boolean conseguido = file.createNewFile();
                if(conseguido)
                    System.out.println("ARCHIVO "+filename+" CREADO");
            }
            catch(Exception e){
                checked = false;
            }
        }
        
        return checked;
    }
    
    /**
     * Gets the content of a file
     * @return The content
     * @throws IOException 
     */
    public ArrayList<ArrayList<String>> getContent() throws IOException{
        ArrayList<ArrayList<String>> content = new ArrayList<ArrayList<String>>();
        ArrayList<String> ipDates;
        String line;
        String [] parts;
        
        if(checkFile()){
            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                while ((line = br.readLine()) != null) {
                    ipDates = new ArrayList<String>();
                    parts = line.split("\t");
                    
                    ipDates.add(parts[0]);  //IP
                    for (int i = 1; i < parts.length;i++ ){ //Dates
                        ipDates.add(parts[i]);
                    }
                    
                    content.add(ipDates);
                }
                br.close();
            }
        }
        
        return content;
    }
    
    /**
     * Deletes the content of a file
     * @return true if everything is correct, false otherwise
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public boolean eraseContent() throws FileNotFoundException, IOException{
        boolean erased = false;
        
        if(checkFile()){
            try{
                PrintWriter writer = new PrintWriter(filename);
                writer.print("");
                writer.close();
                erased = true;
            }
            catch(Exception e){

            }
        }
        
        return erased;
    }
    
    /**
     * Replaces the content of a file for another one
     * @param content The new content
     * @return true if everything is correct, false otherwise
     * @throws IOException 
     */
    private boolean modifyContent(ArrayList<ArrayList<String>> content) throws IOException{
        boolean modified = true;
        String auxFilename = "auxFileToIPLogger.log";
        
        try{
            //First we write the aux file
            File tempFile = new File(auxFilename);
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            for(int i = 0; i < content.size(); i++){
                writer.write(content.get(i).get(0));

                for(int j = 1; j < content.get(i).size(); j++)
                    writer.write("\t"+content.get(i).get(j));

                writer.newLine();
            }

            //Then, we rename the file
            File file = new File(filename);
            modified = tempFile.renameTo(file);

            writer.close();
        }
        catch(IOException e){
            System.err.println("ERROOOOOOORRRRR");
            modified = false;
        }
        
        return modified;
    }
    
    /**
     * Find if an IP is in the file content
     * @param ip The IP to find
     * @return true if the IP is in the file, false otherwise
     * @throws IOException 
     */
    public int findIP(String ip) throws IOException{
        int row = -1;
        
        ArrayList<ArrayList<String>> content = getContent();
        
        for (int i = 0; i < content.size() && row == -1; i++){
            if(content.get(i).get(0).equals(ip)){
                row = i;
            }
        }
        
        return row;
    }
    
    /**
     * Deletes the IP row (if exists)
     * @param ip The IP to delete
     * @return true if everything is correct, false otherwise
     * @throws IOException 
     */
    public boolean deleteIP(String ip) throws IOException{
        boolean ipDeleted = false;
        
        ArrayList<ArrayList<String>> content = getContent();
        content.remove(findIP(ip));
        ipDeleted = modifyContent(content);
        
        return ipDeleted;
    }
    
    /**
     * Deletes the row index given
     * @param index The row index to delete
     * @return true if everything is correct, false otherwise
     * @throws IOException 
     */
    public boolean deleteIndex(int index) throws IOException{
        boolean ipDeleted = false;
        ArrayList<ArrayList<String>> content = getContent();
        
        if(index >= 0 && index < content.size())
            content.remove(index);
        ipDeleted = modifyContent(content);
        
        return ipDeleted;
    }
    
    /**
     * Add a row with a IP (if it doesn't exists)
     * @param ip The IP to save
     * @return true if everything is correct, false otherwise
     * @throws IOException 
     */
    public boolean addIP(String ip) throws IOException{
        boolean added = false;
        
        ArrayList<ArrayList<String>> content = getContent();
        
        int row = findIP(ip);
        if(row == -1){
            ArrayList<String> ipName = new ArrayList<String>();
            ipName.add(ip);
            content.add(ipName);
            added = modifyContent(content);
        }
        else{
            added = true;
        }
        
        return added;
    }
    
    /**
     * Add a date for an IP row (or create it if it doesn't exists)
     * @param ip The IP
     * @param date The date
     * @return true if everything is correct, false otherwise
     * @throws IOException 
     */
    public boolean addRegistry(String ip, String date) throws IOException{
        boolean added = true;
        
        ArrayList<ArrayList<String>> content = getContent();
        
        int row = findIP(ip);
        if(row == -1){
            ArrayList<String> ipName = new ArrayList<String>();
            ipName.add(ip);
            ipName.add(date);
            content.add(ipName);
        }
        else{
            content.get(row).add(date);
        }
        
        added = modifyContent(content);
        
        return added;
    }
    
    /**
     * Check if the IP is in the file
     * @param ip The IP to find
     * @return true if it is, false otherwise
     * @throws IOException 
     */
    public boolean isIP(String ip) throws IOException{
        boolean exists = false;
        
        if(findIP(ip) != -1)
            exists = true;
        
        return exists;
    }
    
    /**
     * It returns the IP in the position given
     * @param index The index to get the IP
     * @return the IP (empty if it is out of range)
     * @throws IOException 
     */
    public String getIP(int index) throws IOException{
        String ip = "";
        ArrayList<ArrayList<String>> content = getContent();
        
        if(index >= 0 && index < content.size())
            ip = content.get(index).get(0);
        
        return ip;
    }
}
