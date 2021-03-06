package org.dyndns.fzoli.rccar.test;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Ping szerű kimenetet ad.
 * Röptében írt osztály, ezért nincs megjegyzés.
 * @author zoli
 */
public class DisconnectProcessTester {

    private Date lastDate;
    private long max = 0, sum = 0, count = 0;
    private boolean timeout = false, disconnected = false;
    private final Date startDate = new Date();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("[mm:ss] ");

    public void onDisconnect() {
        disconnected = true;
        System.out.println("Disconnect!");
    }
    
    public void onTimeout() {
        timeout = true;
        System.out.println(disconnected ? "Already disconnected" : "Timeout!");
    }
    
    public void beforeAnswer() {
        lastDate = new Date();
    }

    public void afterAnswer() {
        Date now = new Date();
        long ping = now.getTime() - lastDate.getTime();
        max = Math.max(max, ping);
        sum += ping;
        count++;
        System.out.println(dateFormat.format(new Date(now.getTime() - startDate.getTime())) + ping + " ms (max. " + (timeout ? "too big" : (max + " ms")) + " ; avg. " + (sum / count) + " ms)");
    }

}
