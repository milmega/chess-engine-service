package com.chessengine.chessengineservice;

public class Move {
    public int colour;
    public int currentSquare;
    public int targetSquare;
    public int changeX;
    public int changeY;
    public boolean promotionFlag;
    public boolean enpassantFlag;
    public int enpassantPosition;
    public boolean castlingFlag;
    public int preCastlinPosition;
    public int postCastlingPosition;

    public Move(int currentSquare, int targetSquare, int colour) { //TODO: update flags earlier in the move generation
        this.currentSquare = currentSquare;
        this.targetSquare = targetSquare;
        this.colour = colour;
        this.promotionFlag = false;
        this.enpassantFlag = false;
        this.enpassantPosition = -1;
        this.castlingFlag = false;
        this.preCastlinPosition = -1;
        this.postCastlingPosition = -1;
        int fromX = currentSquare / 8;
        int fromY = currentSquare % 8;
        int toX = targetSquare / 8;
        int toY = targetSquare % 8;
        this.changeX = toX - fromX;
        this.changeY = toY - fromY;
    }

    public Move getCopy() {
        Move copy = new Move(this.currentSquare, this.targetSquare, this.colour);
        copy.promotionFlag = this.promotionFlag;
        copy.enpassantFlag = this.enpassantFlag;
        copy.enpassantPosition = this.enpassantPosition;
        copy.castlingFlag = this.castlingFlag;
        copy.preCastlinPosition = this.preCastlinPosition;
        copy.postCastlingPosition = this.postCastlingPosition;

        return copy;
    }
}
