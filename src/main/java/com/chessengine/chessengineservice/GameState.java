package com.chessengine.chessengineservice;

public class GameState {
    public boolean isGameLive;
    public Move lastMove;
    public int whiteTime;
    public int blackTime;

    public GameState(boolean isGameLive, Move lastMove, int whiteTime, int blackTime) {
        this.isGameLive = isGameLive;
        this.lastMove = lastMove;
        this.whiteTime = whiteTime;
        this.blackTime = blackTime;
    }
}