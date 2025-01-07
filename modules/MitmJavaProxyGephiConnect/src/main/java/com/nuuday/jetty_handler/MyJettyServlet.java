package com.nuuday.jetty_handler;

import com.nuuday.commons.CommonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 *
 * @author surajmathew
 */
public class MyJettyServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        StringBuffer bufferString = null;
        
        BufferedReader reader = null;
        
        reader = request.getReader();
        
        while(reader.ready()) {
            if(bufferString == null) {
                bufferString = new StringBuffer();
            }
            
            bufferString.append(reader.readLine());
        }
        
        reader.close();
        
        if(bufferString != null) {
            CommonUtils.addToMessageQueue(bufferString.toString());
        }
        
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
