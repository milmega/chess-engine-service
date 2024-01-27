package com.chessengine.chessengineservice;

public class MoveDetails {

    public int colour;
    public int targetPiece;
    public int whiteKingPosition;
    public int blackKingPosition;
    public boolean[] whiteCastling;
    public boolean[] blackCastling;
    public Pair<Integer, Integer> rookMove;
    public boolean promotionFlag;
    public boolean castlingFlag;
    public boolean enpassantFlag;
    public int enpassantPosition;

    public MoveDetails() {}
}
