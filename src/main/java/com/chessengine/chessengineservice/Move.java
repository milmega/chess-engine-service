package com.chessengine.chessengineservice;

import java.util.ArrayList;
import java.util.List;

public class Move {
    private int x;
    private int y;
    private Move destination;
    private int score;

    public Move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Move(int x, int y, int destX, int destY) {
        this.x = x;
        this.y = y;
        this.destination = new Move(destX, destY);
    }

    public Move(int x, int y, int destX, int destY, int score) {
        this.x = x;
        this.y = y;
        this.destination = new Move(destX, destY);
        this.score = score;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Move getCopy() {
        return new Move(this.getX(), this.getY(), this.getDestination().getX(), this.getDestination().getY(), this.getScore());
    }

    public String toString() {
        return getX() + ", " + getY() + " to " + getDestination().getX() + ", " + getDestination().getY();
    }
}
