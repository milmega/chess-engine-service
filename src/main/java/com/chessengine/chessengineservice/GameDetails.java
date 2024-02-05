package com.chessengine.chessengineservice;

public class GameDetails {
    public Move move;
    public boolean[] whiteCastling;
    public boolean[] blackCastling;
    public int gameStage;
    public int fullMoveCount;
    public int plysSinceCaptureOrPawnMove;
    public int captures;
    public int[] numberOfPieces;

    public GameDetails(Move move, boolean[] whiteCastling, boolean[] blackCastling, int gameStage, int fullMoveCount, int plysSinceCaptureOrPawnMove, int captures, int[] numberOfPieces) {
        this.move = move;
        this.whiteCastling = whiteCastling;
        this.blackCastling = blackCastling;
        this.gameStage = gameStage;
        this.fullMoveCount = fullMoveCount;
        this.plysSinceCaptureOrPawnMove = plysSinceCaptureOrPawnMove;
        this.captures = captures;
        this.numberOfPieces = numberOfPieces.clone();

    }
}
