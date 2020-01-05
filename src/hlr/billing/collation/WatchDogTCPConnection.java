/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hlr.billing.collation;

import java.io.BufferedOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author vazhenin
 */
public class WatchDogTCPConnection implements Runnable {

    Date startTime;
    long sleepTime = 5000; /* 1 second */

    BufferedOutputStream out;

    public WatchDogTCPConnection(BufferedOutputStream o) {
        this.out = o;
    }

    @Override
    public void run() {
        try {
            /* set socket start time */
            setCurrTime(new Date());
            while (true) {
                Thread.sleep(sleepTime);
                try {
                    out.write("test".getBytes());
                    out.flush();
                } catch (Exception e) {
                    /* mean connection has been closed */
                    System.err.println("Has been disconnected after " + TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - startTime.getTime()) + " minutes");
                    MainFrame.setStateDisconnected();
                }
            }
        } catch (Exception e) {
            Logging.put_log(e);
        }
    }

    public void setCurrTime(Date currTime) {
        this.startTime = currTime;
    }

}
