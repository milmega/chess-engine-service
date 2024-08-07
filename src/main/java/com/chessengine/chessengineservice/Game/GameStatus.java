package com.chessengine.chessengineservice.Game;

import com.chessengine.chessengineservice.Structures.Move;

public class GameStatus {
    public boolean isGameLive;
    public Move lastMove;
    public int whiteTime;
    public int blackTime;

    public GameStatus(boolean isGameLive, Move lastMove, int whiteTime, int blackTime) {
        this.isGameLive = isGameLive;
        this.lastMove = lastMove;
        this.whiteTime = whiteTime;
        this.blackTime = blackTime;
    }
}