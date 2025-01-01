package com.nuuday.mitm_proxy_handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.InputMismatchException;

import website.magyar.mitm.proxy.ProxyServer;

public class MyProxyStarter {

	private final int PROXY_TIMEOUT = 10000; //5 sec
    public ProxyServer proxyServer;
    public int proxyPort;
    
    static MyProxyStarter myProxyStarter = new MyProxyStarter();
    
	public static void main(String[] args) {
		
		myProxyStarter.displayMenu();
	}

    public void startProxy() throws Exception {
    	if(proxyServer == null) {
    		proxyServer = new ProxyServer(12230); //0 means random port
    		proxyServer.start(PROXY_TIMEOUT);
    		proxyPort = proxyServer.getPort(); //get the proxy server port
    		proxyServer.addRequestInterceptor(new MyProxyRequestInterceptor());
    		
    		System.out.println("\nServer started in port - "+proxyPort);
    	} else {
    		System.out.println("\n Proxy server already running");
    	}
    }

    public void stopProxy() throws Exception {
        if (proxyServer != null) {
            proxyServer.stop();
            Thread.sleep(3000);
            proxyServer = null;
        }
    }
    
    public void displayMenu() {
		
    	BufferedReader reader = null;
		String userSelection;
		boolean isActive = true;
		
		while(isActive) {
			System.out.println("1. Start proxy server");
			System.out.println("2. Stop proxy server");
			System.out.println("3. Exit");
			System.out.println("\n\nEnter your choice:");
			
			try {
				reader = new BufferedReader(new InputStreamReader(System.in));
				userSelection = reader.readLine();
				
				switch(userSelection) {
				case "1": try {
						myProxyStarter.startProxy();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}  break;
				case "2": try {
						myProxyStarter.stopProxy();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} break;
				case "3": isActive = false; break;
				default: System.err.println("\n ************ Invalid option.\n\n");
				}
				
			} catch (InputMismatchException inputMismatchException) {
				System.err.println(inputMismatchException.getMessage());
				System.out.println("\n\n");
			} catch (IOException ioException) {
				System.err.println(ioException.getMessage());
				System.out.println("\n\n");
			}
		}
		
		System.out.println("\n\nExited");
    }
}