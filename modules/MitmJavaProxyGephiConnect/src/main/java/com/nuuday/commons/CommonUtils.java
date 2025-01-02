package com.nuuday.commons;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author surajmathew
 */
public class CommonUtils {
    
    private static final Queue<String> MESSAGE_QUEUE = new LinkedList<String>();
    private static final Object MESSAGE_QUEUE_LOCK_OBJECT = new Object();
    
    public static void addToMessageQueue(String message) {
        synchronized (MESSAGE_QUEUE_LOCK_OBJECT) {
            if(isNotNull(message)) {
                MESSAGE_QUEUE.add(message);
            }
        }
    }
    
    public static String getFirstElementFromMessageQueue() {
        
        synchronized (MESSAGE_QUEUE_LOCK_OBJECT) {
            return MESSAGE_QUEUE.poll();
        }
    }
    
    public static boolean isNull(String inputValue) {
        boolean isNull = false;
        
        if(inputValue == null || inputValue.trim().length() == 0) {
            isNull = true;
        }
        
        return isNull;
    }
    
    public static boolean isNotNull(String inputValue) {
        return !isNull(inputValue);
    }
}
