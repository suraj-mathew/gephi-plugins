package com.nuuday.jetty_handler;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

/**
 *
 * @author surajmathew
 */
public class MyJettyStarter {
    
    private Server server;
    private ServletHandler servletHandler;
    private final int PORT = 12345;
    
    public void startServer() throws Exception {
        server = new Server(PORT);
        servletHandler = new ServletHandler();
        server.setHandler(servletHandler);
        
        servletHandler.addServletWithMapping(MyJettyServlet.class, "/url_to_gephi");
        
        server.start();
    }
    
    public void stopServer() throws Exception {
    
        server.stop();
       
    }
}
