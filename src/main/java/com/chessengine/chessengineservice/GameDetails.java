package com.chessengine.chessengineservice;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.getDeepCopy;

public class GameDetails {
    public Move move;
    public long zobristKey;
    public int castling;
    public int enPassantColumn;
    public int gameStage;
    public int fullMoveCount;
    public int movesSinceCaptureOrPawnMove;
    public int captures;
    public int[][] material;

    public GameDetails(Move move, long zobristKey, int castling, int enPassantColumn, int gameStage, int fullMoveCount, int movesSinceCaptureOrPawnMove, int captures, int[][] material) {
        this.move = move;
        this.zobristKey = zobristKey;
        this.castling = castling;
        this.enPassantColumn = enPassantColumn;
        this.gameStage = gameStage;
        this.fullMoveCount = fullMoveCount;
        this.movesSinceCaptureOrPawnMove = movesSinceCaptureOrPawnMove;
        this.captures = captures;
        this.material = getDeepCopy(material);
    }
}
