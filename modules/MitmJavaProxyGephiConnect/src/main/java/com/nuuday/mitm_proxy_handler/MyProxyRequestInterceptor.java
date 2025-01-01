package com.nuuday.mitm_proxy_handler;

import org.apache.http.Header;

import website.magyar.mitm.proxy.RequestInterceptor;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;


public class MyProxyRequestInterceptor implements RequestInterceptor {

	@Override
	public void process(MitmJavaProxyHttpRequest request) {

		String fullUrl = request.getMethod().getURI().toString();
		String messageId = request.getMessageId();
		String methodName = request.getMethod().getMethod();
		
		System.out.println(messageId+" -> fullUrl - "+fullUrl);
		Header[] headerArray = request.getMethod().getAllHeaders();
		
		for(int index=0; index < headerArray.length; index++) {
			System.out.println(messageId+" -> "+headerArray[index].getName()+" : "+headerArray[index].getValue());
		}
	}

}
