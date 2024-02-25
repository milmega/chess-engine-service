package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.OpeningDB.OpeningDatabase;

import java.util.List;

public class Game {
    Evaluator evaluator;
    Board board;
    OpeningDatabase openingDatabase;
    boolean useOpeningDB = false;
    public int gameId;
    public int playerId;
    public int opponentId;
    public Move lastMove;

    public Game(int gameId, int playerId, int opponentId) {
        board = new Board();
        evaluator = new Evaluator(board);
        openingDatabase = new OpeningDatabase();
        this.gameId = gameId;
        this.playerId = playerId;
        this.opponentId = opponentId;
        this.lastMove = new Move(0, 0, 0, 0);
    }

    public Move getBestMove(int colour) {
        if (useOpeningDB){
            List<Move> allMoves = getAllMoves(colour);
            Move moveFromDB = openingDatabase.getNextMove(board.pgnCode, allMoves);
            if (moveFromDB == null) {
                useOpeningDB = false;
            } else {
                board.makeMove(moveFromDB, false);
                return moveFromDB;
            }
        }

        Move bestMove = evaluator.getBestMove(colour);
        if(bestMove != null) {
        if (bestMove != null) {
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
