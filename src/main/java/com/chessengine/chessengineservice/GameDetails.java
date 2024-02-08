package com.chessengine.chessengineservice;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.getDeepCopy;

public class GameDetails {
    public Move move;
    public int castling;
    public int gameStage;
    public int fullMoveCount;
    public int movesSinceCaptureOrPawnMove;
    public int captures;
    public int[][] material;

    public GameDetails(Move move, int castling, int gameStage, int fullMoveCount, int movesSinceCaptureOrPawnMove, int captures, int[][] material) {
        this.move = move;
        this.castling = castling;
        this.gameStage = gameStage;
        this.fullMoveCount = fullMoveCount;
        this.movesSinceCaptureOrPawnMove = movesSinceCaptureOrPawnMove;
        this.captures = captures;
        this.material = getDeepCopy(material);

    }
}
