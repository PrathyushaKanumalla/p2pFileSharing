package project;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
	
	static private FileHandler fileHandler;
    static private LogFormatter logFormatter;
    private static Logger logger;
    
    private Log () {}
    
    public static class SingletonLogHelper {
    	private static final Log singleton = new Log();
    }
    
    public static Log getLogger () {
		return SingletonLogHelper.singleton;
	}
    
    protected static void setUpSingleTonLog (int peerId) {
    	SingletonLogHelper.singleton.setup(peerId);
    }
    
    protected static void addLog(String row) {
    	SingletonLogHelper.singleton.addRow(row);
    }
    
    private void setup(int peerID) {
    	try {
	    	// get the global logger to configure it
    		 // System.getProperty("user.dir") + File.separator + 
	        logger = Logger.getLogger(Log.class.getName());
	        logger.setUseParentHandlers(false);
	        
	        Handler[] handlers = logger.getHandlers();
	        if (handlers != null && handlers.length > 0 && handlers[0] instanceof ConsoleHandler) {
	        	logger.removeHandler(handlers[0]);
	        }
	        logger.setLevel(Level.INFO);
	        
	        // get current path and generate a log file
	     	String fileName = Constants.FILE_PREFIX + File.separator + 
	     			Constants.LOGFILEPREFIX + peerID + Constants.LOGFILESUFFIX;
	        fileHandler = new FileHandler(fileName);
	        // create a TXT formatter
	        logFormatter = new LogFormatter();
	        fileHandler.setFormatter(logFormatter);
	        
	        logger.addHandler(fileHandler);
    	} catch (Exception e) {
    		System.err.print("*Loger can't be set Up*");
    	}
    	System.out.print("*Logging started*");
    }
    
    private void addRow(String row) {
		logger.info(row);
	}
    
}
