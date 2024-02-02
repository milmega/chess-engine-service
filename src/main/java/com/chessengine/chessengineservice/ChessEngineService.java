package com.chessengine.chessengineservice;

public class ChessEngineService {
    Evaluator evaluator;
    Board board;

    public ChessEngineService() {
        board = new Board();
        evaluator = new Evaluator();
    }

    public String GetNextMoveAsString(int colour) {
        Move nextMove = evaluator.getBestMove(colour, board);
        board.makeMove(nextMove, false);

        return nextMove.currentSquare + "," + nextMove.targetSquare;
    }

    public Board getBoard() {
        return this.board;
    }
}
