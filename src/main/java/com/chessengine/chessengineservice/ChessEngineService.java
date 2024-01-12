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

    public String GetNextMove(int colour) {
        Move nextMove = evaluator.getBestMove(colour, board);
        board.makeMove(nextMove, false);

        return String.valueOf(nextMove.currentSquare/8) + //TODO: convert frontend and return to handle 1d array
                String.valueOf(nextMove.currentSquare%8) +
                String.valueOf(nextMove.targetSquare/8) +
                String.valueOf(nextMove.targetSquare%8);
    }

    public Board getBoard() {
        return this.board;
    }
}
