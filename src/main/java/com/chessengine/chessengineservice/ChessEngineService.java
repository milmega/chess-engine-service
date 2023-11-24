package com.chessengine.chessengineservice;

public class ChessEngineService {

    GameEngine gameEngine;
    public ChessEngineService() {
        gameEngine = new GameEngine();
    }

    public String GetNextMove(int colour, int[][] board) {

        Move nextMove = gameEngine.calculateNextMove(colour, board);

        return String.valueOf(nextMove.getX()) +
                String.valueOf(nextMove.getY()) +
                String.valueOf(nextMove.getDestination().getX()) +
                String.valueOf(nextMove.getDestination().getY());
    }
}
