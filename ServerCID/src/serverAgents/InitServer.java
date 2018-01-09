package serverAgents;

import ConfigFile.ConfigFile;
import DiskLogger.DiskLogger;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;

/**
 * Class that launches the server agent
 * 
 * @author Aaron Rodriguez Bueno
 */
public class InitServer {
    
    
    /**
    * Server program start
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
            sshlog =        "ssh_attacks.log",
            serverlog =     "serverlog.json",
            servername =    "ServerAgent";
        int port =          6000;
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
            System.err.println("*** Error loading configuration ["+configfile+"]");
            System.exit(1);
        }
        else  {
            if (Config.config.get("sshlog")!=null)
                sshlog = Config.config.get("sshlog").asString();
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
            if (Config.config.get("serverlog")!=null)
                serverlog = Config.config.get("serverlog").asString();
            if (Config.config.get("servername")!=null)
                servername = Config.config.get("servername").asString();
        }
        
        //Creating the log
        System.out.println("\nCreating log ... "+sshlog);
        dlogger = new DiskLogger(serverlog);
        if (!dlogger.Init())
            System.err.println("*** Error creating logs file.");
        
        // We connect with Magentix server
        System.out.println("-------------------- Starting session with Magentix Server ----------------");
        try {
            AgentsConnection.connect(host,port,virtualhost,username, password, ssl);
        }catch(Exception ex) {
            System.err.println(/*dlogger.AddRecord(*/"*** Error connecting with Magentix Server in "+host+"["+port+"] <"+virtualhost+">"/*)*/);
            return;
        }
        System.out.println(/*dlogger.AddRecord(*/"Connect succesfull with Magentix Server in "+host+"["+port+"] <"+virtualhost+">"/*)*/);
                //return;
        
        // We try to initialize and start the server agent
        try {
            SERV server = new SERV(new AgentID(servername), dlogger, serverlog, sshlog);
            server.start();

        } catch(Exception ex) {
            System.err.println(dlogger.AddRecord(" *** Error creating "+servername));
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
        
    }
}
