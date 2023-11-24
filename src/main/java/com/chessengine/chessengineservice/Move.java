package com.chessengine.chessengineservice;

import java.util.ArrayList;
import java.util.List;

public class Move {
    private int x;
    private int y;
    private Move destination;

    public Move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Move(int x, int y, int destX, int destY) {
        this.x = x;
        this.y = y;
        this.destination = new Move(destX, destY);
    }

    public void setMove(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return  x;
    }

    public int getY() {
        return y;
    }

    public Move getDestination() {
        return destination;
    }
}
