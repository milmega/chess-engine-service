package com.chessengine.chessengineservice;

public class ChessEngineService {
    MoveGenerator moveGenerator;
    Evaluator evaluator;
    Board board;

    public ChessEngineService() {
        moveGenerator = new MoveGenerator();
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
