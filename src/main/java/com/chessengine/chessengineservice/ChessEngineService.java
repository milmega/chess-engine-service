package com.chessengine.chessengineservice;

import java.util.List;

public class ChessEngineService {
    Evaluator evaluator;
    Board board;
    OpeningDatabase openingDatabase;
    boolean useOpeningDB = true;

    public ChessEngineService() {
        board = new Board();
        evaluator = new Evaluator(board);
        openingDatabase = new OpeningDatabase();
    }

    public Move getBestMove(int colour) {
        Move bestMove = evaluator.getBestMove(colour, board);
        if (useOpeningDB){
            List<Move> allMoves = getAllMoves(colour);
            Move moveFromDB = openingDatabase.getNextMove(board.pgnCode, allMoves);
            if(moveFromDB == null) {
                useOpeningDB = false;
            } else {
                board.makeMove(moveFromDB, false);
                return moveFromDB;
            }
        }
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

    public void reset() {
        board.resetBoard();
        openingDatabase.shuffleOpeningBook();
    }
}
