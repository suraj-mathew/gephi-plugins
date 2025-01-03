package com.nuuday.mitm_proxy_handler;

import com.nuuday.commons.CommonUtils;
import com.nuuday.commons.LoggerUtils;
import org.apache.http.Header;

import website.magyar.mitm.proxy.RequestInterceptor;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;


public class MyProxyRequestInterceptor implements RequestInterceptor {

	@Override
	public void process(MitmJavaProxyHttpRequest request) {

		String fullUrl = request.getMethod().getURI().toString();
		String messageId = request.getMessageId();
		String methodName = request.getMethod().getMethod();
		
		LoggerUtils.info(messageId+" -> fullUrl - "+fullUrl);
		Header[] headerArray = request.getMethod().getAllHeaders();
		
                CommonUtils.addToMessageQueue(fullUrl);
                
		/*for(int index=0; index < headerArray.length; index++) {
			LoggerUtils.info(messageId+" -> "+headerArray[index].getName()+" : "+headerArray[index].getValue());
		}*/
	}

}
