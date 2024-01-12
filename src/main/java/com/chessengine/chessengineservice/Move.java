package com.chessengine.chessengineservice;

public class Move {
    public int currentSquare;
    public int targetSquare;

    public Move(int currentSquare, int targetSquare) {
        this.currentSquare = currentSquare;
        this.targetSquare = targetSquare;
    }

    public Move getCopy() {
        return new Move(this.currentSquare, this.targetSquare);
    }
}
