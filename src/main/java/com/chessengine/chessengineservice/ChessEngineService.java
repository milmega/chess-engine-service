package com.chessengine.chessengineservice;

import java.util.List;

public class ChessEngineService {
    Evaluator evaluator;
    Board board;

    public ChessEngineService() {
        board = new Board();
        evaluator = new Evaluator(board);
    }

    public Move getBestMove(int colour) {
        Move bestMove = evaluator.getBestMove(colour, board);
        if(bestMove != null) {
            board.makeMove(bestMove, false);
        } else {
            bestMove = new Move(0, 0, 0, 0);
        }
        bestMove.gameResult = board.getGameResult(-colour);
        return bestMove;
    }

    public List<Move> getAllMoves(int colour) {
        return board.getAllMoves(colour);
    }

    public Board getBoard() {
        return this.board;
    }
}
