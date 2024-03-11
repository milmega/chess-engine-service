package com.chessengine.chessengineservice;

import java.util.Timer;
import java.util.TimerTask;

public class TimerManager {
    boolean running;
    int secondsLeft;
    Timer timer;
    TimerTask task;
    final int TIMER_SECONDS = 15 * 60;

    public TimerManager() {
        this.secondsLeft = TIMER_SECONDS;
        this.running = false;
    }

    public void start() {
        if(!running) {
            timer = new Timer();
            task = new TimerTask() {
                public void run() {
                    if (secondsLeft > 0) {
                        secondsLeft -= 1;
                    } else {
                        stop();
                    }
                }
            };
            timer.scheduleAtFixedRate(task, 0, 1000);
            running = true;
        }
    }

    public void stop() {
        if (running) {
            timer.cancel();
            running = false;
        }
    }

    public void reset() {
        this.secondsLeft = TIMER_SECONDS;
        this.running = false;
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }
}