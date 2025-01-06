package com.nuuday.mitm_proxy_handler;

import com.nuuday.commons.LoggerUtils;
import website.magyar.mitm.proxy.ProxyServer;

public class MyProxyStarter {

    private final int PROXY_TIMEOUT = 10000; //10 sec
    private ProxyServer proxyServer;
    private int proxyPort = 12230;
    
    public void startProxy() throws Exception {
    	if(proxyServer == null) {
                LoggerUtils.info("Going to start mitm server...");
                System.setProperty("jdk.tls.namedGroups", "secp256r1, secp384r1, ffdhe2048, ffdhe3072");
                
    		proxyServer = new ProxyServer(proxyPort); //0 means random port
    		proxyServer.start(PROXY_TIMEOUT);
    		proxyPort = proxyServer.getPort(); //get the proxy server port
    		proxyServer.addRequestInterceptor(new MyProxyRequestInterceptor());
    		
    		LoggerUtils.info("\nServer started in port - "+proxyPort);
    	} else {
    		LoggerUtils.info("\n Proxy server already running");
    	}
    }

    public void stopProxy() throws Exception {
        if (proxyServer != null) {
            proxyServer.stop();
            Thread.sleep(3000);
            proxyServer = null;
        }
    }

}
