package com.chessengine.chessengineservice;

public class MoveDetails {

    private int targetPiece;
    private int whiteKingPosition;
    private int blackKingPosition;
    private boolean[] whiteCastling;
    private boolean[] blackCastling;
    private Pair<Integer, Integer> rookMove;

    public MoveDetails(int targetPiece, int whiteKingPosition, int blackKingPosition, boolean[] whiteCastling, boolean[] blackCastling) {
        this.targetPiece = targetPiece;
        this.whiteKingPosition = whiteKingPosition;
        this.blackKingPosition = blackKingPosition;
        this.whiteCastling = whiteCastling;
        this.blackCastling = blackCastling;
        rookMove = new Pair<>(-1, -1);
    }

    public boolean[] getBlackCastling() {
        return blackCastling;
    }

    public boolean[] getWhiteCastling() {
        return whiteCastling;
    }

    public int getBlackKingPosition() {
        return blackKingPosition;
    }

    public int getWhiteKingPosition() {
        return whiteKingPosition;
    }

    public int getTargetPiece() {
        return targetPiece;
    }

    public Pair<Integer, Integer> getRookMove() {
        return rookMove;
    }

    public void setRookMove(Pair<Integer, Integer> move) {
        rookMove = new Pair<>(move.first, move.second);
    }
}
