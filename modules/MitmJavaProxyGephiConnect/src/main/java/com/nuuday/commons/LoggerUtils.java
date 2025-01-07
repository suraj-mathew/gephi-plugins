
package com.nuuday.commons;

import com.nuuday.gephi_handler.MyGephiGenerator;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.openide.util.Exceptions;

/**
 *
 * @author surajmathew
 */
public final class LoggerUtils {
    private static final Logger LOG;
    private static final SimpleDateFormat DDMONYYYYHH24MI = new SimpleDateFormat("ddMMMyyyy-HHmm");
    
    private static FileHandler logFileHandler;
    
    static {
        try {
            logFileHandler = new FileHandler("MitmJavaProxyGephiConnector-"+DDMONYYYYHH24MI.format(new Date())+".log");
            logFileHandler.setFormatter(new SimpleFormatter());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        LOG = Logger.getLogger(MyGephiGenerator.class.getName());
        LOG.setLevel(Level.INFO);
        LOG.addHandler(logFileHandler);
    }
    
    public static void info(String message) {
        LOG.info(message);
    }
    
    public static void warn(String message) {
        LOG.warning(message);
    }
    
    public static void severe(String message) {
        LOG.severe(message);
    }
}
