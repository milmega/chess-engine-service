package com.chessengine.chessengineservice;

import java.util.ArrayList;
import java.util.List;

public class Move {
    private int x;
    private int y;
    private List<Move> destinations = new ArrayList<>();

    public Move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Move(int x, int y, ArrayList<Move> destinations) {
        this.x = x;
        this.y = y;
        this.destinations = destinations;
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

    public List<Move> getDestinations() {
        return destinations;
    }
}
