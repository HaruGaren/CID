package deviceAgents;

import ConfigFile.ConfigFile;
import DiskLogger.DiskLogger;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;

/**
 * Class that launches the device agents
 * 
 * @author Aaron Rodriguez Bueno
 */
public class InitAgents {
    
    
    /**
    * Device program start
    * 
    * @param args Command arguments when the program starts
    */
    public static void main(String[] args) {
        
        ConfigFile Config;
        String 
            host =          "",
            virtualhost =   "",
            username =      "",
            password =      "",
            sshagent =      "",
            serveragent =   "ServerAgent",
            sshfilename =   "/var/log/auth.log",
            sshlog =        "sshagent_log.json";
        int port =          6000,
            seconds =       300,
            attempts =      5,
            lines =         1000;
        boolean ssl =       false;
        
        DiskLogger dlogger;
        
        //String configfile="myconfig.json";
        String configfile="config.json";
        
        if(args.length > 0)
            configfile = args[0]; 
        
        //Loading configuration file
        System.out.println("Loading configuration ...");  

        Config = new ConfigFile(configfile);
        if (!Config.Init())  {
            System.err.println("*** Error loading configuration file ["+configfile+"]");
            System.exit(1);
        }
        else  {
            if (Config.config.get("host")!=null)
                host = Config.config.get("host").asString();
            if (Config.config.get("virtualhost")!=null)
                virtualhost = Config.config.get("virtualhost").asString();
            if (Config.config.get("username")!=null)
                username = Config.config.get("username").asString();
            if (Config.config.get("password")!=null)
                password = Config.config.get("password").asString();
            if (Config.config.get("port")!=null)
                port = Config.config.get("port").asInt();
            if (Config.config.get("ssl")!=null)
                ssl = Config.config.get("ssl").asBoolean();
            if (Config.config.get("servername")!=null)
                serveragent = Config.config.get("servername").asString();
            if (Config.config.get("seconds")!=null)
                seconds = Config.config.get("seconds").asInt();
            if (Config.config.get("seconds")!=null)
                seconds = Config.config.get("seconds").asInt();
            if (Config.config.get("lines")!=null)
                lines = Config.config.get("lines").asInt();
            if (Config.config.get("authlogfilename")!=null)
                sshfilename = Config.config.get("authlogfilename").asString();
            if (Config.config.get("sshlog")!=null)
                sshlog = Config.config.get("sshlog").asString();
            if (Config.config.get("sshagent")!=null)
                sshagent = Config.config.get("sshagent").asString();
            if (Config.config.get("attempts")!=null)
                attempts = Config.config.get("attempts").asInt();
        }
        
        //Creating the logs
        System.out.println("\nCreating log ... "+sshlog);
        dlogger = new DiskLogger(sshlog);
        if (!dlogger.Init())
            System.err.println("*** SSH: Error creating logs file.");
        
        // We connect with Magentix server     
        try{
            AgentsConnection.connect(host,port,virtualhost,username, password, ssl);
        }
        catch(Exception ex) {
            System.err.println(dlogger.AddRecord("*** Error connecting with Magentix Server in "+host+"["+port+"] <"+virtualhost+">"));
            return;
        }
        System.out.println(dlogger.AddRecord("Connect succesfull with Magentix Server in "+host+"["+port+"] <"+virtualhost+">"));

        // We try to initialize and start the device agents
        try {
            if(!sshagent.equals("")){
                SSH sshAgent = new SSH(new AgentID(sshagent),new AgentID(serveragent), dlogger, sshfilename, seconds, attempts, lines);
                sshAgent.start();
            }

        } catch(Exception ex) {
            System.err.println(dlogger.AddRecord(" *** Error creating agent \""+sshagent+"\""));
            System.exit(-1);
        }
    }    
}