package com.chessengine.chessengineservice;

import java.util.List;

public class Evaluator {

    public Evaluator() {

    }

    public Move getBestMove(List<Move> allMoves, int[][] board) {
        Move tempBestMove = null;
        int bestMoveScore = 0;
        for (Move currentMove : allMoves) {
            Move destination = currentMove.getDestination();
            int[][] tempBoard = GameEngine.getDeepCopy(board);
            tempBoard[destination.getX()][destination.getY()] = board[currentMove.getX()][currentMove.getY()];
            tempBoard[currentMove.getX()][currentMove.getY()] = 0;
            int score = evaluateBoard(tempBoard);
            if (score > bestMoveScore) {
                bestMoveScore = score;
                tempBestMove = new Move(currentMove.getX(), currentMove.getY(), destination.getX(), destination.getY());
            }
        }

        return tempBestMove;
    }

    private int evaluateBoard(int[][] board) {
        return 0;
    }
}