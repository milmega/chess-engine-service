package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.OpeningDB.OpeningDatabase;

import java.util.List;

public class Game {
    Evaluator evaluator;
    Board board;
    OpeningDatabase openingDatabase;
    boolean useOpeningDB = true;
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
        long endTime = System.currentTimeMillis();
        sum += endTime - startTime;
        minTime = Math.min(minTime, endTime-startTime);
        maxTime = Math.max(maxTime, endTime-startTime);

        //long startTimeTT = System.currentTimeMillis();
        //evaluator.tTable.disabled = false;
        //Move bestMoveTT = evaluator.getBestMove(colour);
        //long endTimeTT = System.currentTimeMillis();
        //sumTT += endTimeTT - startTimeTT;
        //minTimeTT = Math.min(minTimeTT, endTimeTT-startTimeTT);
        //maxTimeTT = Math.max(maxTimeTT, endTimeTT-startTimeTT);

        count++;
        System.out.println("elapsed time: " + (endTime-startTime)/* + " with TT: "+ (endTimeTT-startTimeTT)*/);
        if (count == 5) {
            System.out.println("min: " + minTime + " max: " + maxTime + " avg: " + (double)(sum/count)/* + " vs " + "min: " + minTimeTT + " max: " + maxTimeTT + " avg: " + (double)(sumTT/count)*/);
            minTime = 1000000000;
            maxTime = 0;
            sum = 0;
            //minTimeTT = 1000000000;
            //maxTimeTT = 0;
            //sumTT = 0;
            count = 0;
        }

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
