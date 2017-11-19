/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ConfigFile;

import com.eclipsesource.json.JsonObject;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;


/**
 *
 * @author Luis Castillo
 */
public class ConfigFile {
   private String filename;
   public JsonObject config;
   
   public ConfigFile()  {
      filename="defaultconfig.json";
      config=null;
   }
    public ConfigFile(String file) {
        filename = file;
    }
    public ConfigFile(String path, String file) {
        filename = path+file;
    }
    
    public String getConfigFileName()  {
        return filename;
    }
    public boolean Init()  {
        File file;
        String str="";

        file = new File(filename);
        if (file != null)
            if (file.exists()){
                if (file.isFile())  {
                    try {
                        str = FileUtils.readFileToString(file, "utf-8");
                    } catch (IOException ex) {
                        return false;
                    }
                    System.out.println("CONFIG FILE: "+str);
                    config = JsonObject.readFrom(str);
                    return true;
                }
                else{
                    return false;
                }
            }
            else{
                return false;
            }
        else{
            return false;
        }
    }
   
 
}
