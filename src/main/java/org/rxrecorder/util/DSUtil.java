package org.rxrecorder.util;

/**
 * Created by daniel on 27/04/17.
 */
public class DSUtil {
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
