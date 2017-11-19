/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DiskLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author Luis Castillo
 */
public class DiskLogger {
    private String filename;
    private boolean valid;
    
    public DiskLogger() {
        filename="defaultlog.json";
        valid = false;
    }
    public DiskLogger(String file) {
        filename = file;
        valid = false;    
    }
    public DiskLogger(String path, String file) {
        filename = path+file;
        valid = false;    
    }
    
    public String getLoggerName()  {
        return filename;
    }
   
    public boolean isValid()  {
        return valid;
    }
    public boolean Init()  {
        File file;

        file = new File(filename);
        if (file != null)
            if (file.exists())
                if (file.isFile())
                    return (valid = true);    // Fichero existe
                else
                    return false;   // Es Directorio
            else   {
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    return false;   // Fichero nuevo MAL
                }
                return (valid=true);        // Fichero nuevo OK
            }
        else
           return false;            // null
    }
    
    public String AddRecord(String s) {
        if (valid)  {
            String timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
            PrintWriter outfile;
            try {
                outfile = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            } catch (IOException ex) {
                return "";
            }
            BufferedWriter out = new BufferedWriter(outfile);
            String toRecord="{\"date\":\""+timeStamp+"\", \"value\":\""+s+"\"}"; 
            outfile.println(toRecord);
            //System.out.println(toRecord);
            outfile.close();
            return s;
        }
        else
            return "";
    }
    public String AddObject(String s) {
        if (valid)  {
            String timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
            PrintWriter outfile;
            try {
                outfile = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            } catch (IOException ex) {
                return "";
            }
            BufferedWriter out = new BufferedWriter(outfile);
            String toRecord="{\"date\":\""+timeStamp+"\", \"value\":"+s+"}"; 
            outfile.println(toRecord);
            outfile.close();
            return s;
        }
        else
            return "";
    }
}
