package com.chessengine.chessengineservice.SearchAlgo;

import com.chessengine.chessengineservice.Board.Board;
import com.chessengine.chessengineservice.Structures.Move;
import com.chessengine.chessengineservice.MoveGenerator.MoveGenerator;
import com.chessengine.chessengineservice.Transposition.TranspositionTable;

import java.util.HashMap;
import java.util.List;

import static com.chessengine.chessengineservice.Helpers.EvaluatorHelper.getPositionScore;
import static com.chessengine.chessengineservice.SearchAlgo.MoveSorter.sort;

public class Evaluator {

    MoveGenerator moveGenerator;
    HashMap<String, List<Move>> moveCache;
    Board board;
    TranspositionTable tTable;
    int level;
    private int EVALUATION_DEPTH = 5;
    private int QSEARCH_DEPTH = 4;
    private final int MAX_VALUE = 1000000000;
    private final int MIN_VALUE = -1000000000;
    public static int[] materialValue = {0, 100, 320, 330, 500, 900, 0};

    public Evaluator(Board board, int level) {
        this.board = board;
        this.level = level;
        if(level == 1) {
            EVALUATION_DEPTH = 2;
            QSEARCH_DEPTH = 0;
        } else if (level == 2) {
            EVALUATION_DEPTH = 3;
            QSEARCH_DEPTH = 2;
        }
        moveGenerator = board.moveGenerator;
        moveCache = new HashMap<>();
        tTable = board.tTable;
    }

    public Move getBestMove(int colour) {
        List<Move> allMoves = moveGenerator.computeAllMoves(colour, false);
        allMoves = sort(allMoves, moveGenerator.oppAttackMap,
                moveGenerator.oppPawnAttackMap, board.getGameStage());
        Move bestMove = null;
        int bestScore = MIN_VALUE;

        for (Move move : allMoves) {
            board.makeMove(move, true);
            int score = -negamax(-colour, EVALUATION_DEPTH-1, 1, MIN_VALUE, MAX_VALUE);
            board.unmakeMove(move);

            if (score > bestScore) {
                bestMove = move;
                bestScore = score;
            }
        }
        return bestMove;
    }

    private int negamax(int colour, int layersToExplore, int currDepth, int alpha, int beta) {
        int ttScore = tTable.retrieveScore(layersToExplore, currDepth, alpha, beta);
        if (ttScore != -1) {
            return ttScore;
        }
        if (layersToExplore == 0) {
            return quiescenceNegamax(colour, QSEARCH_DEPTH, alpha, beta);
        }
        List<Move> allMoves = moveGenerator.computeAllMoves(colour, false);
        allMoves = sort(allMoves, moveGenerator.oppAttackMap,
                moveGenerator.oppPawnAttackMap, board.getGameStage());
        if (allMoves.isEmpty()) {
            if (moveGenerator.isKingInCheck()) {
                return MIN_VALUE + currDepth;
            }
            return 0;
        }

        int evaluationBound = tTable.upperBound;
        for (Move move : allMoves) {
            board.makeMove(move, true);
            int score = -negamax(-colour, layersToExplore - 1, currDepth + 1, -beta, -alpha);
            board.unmakeMove(move);

            if (score >= beta) {
                tTable.saveScore(layersToExplore, currDepth, beta, tTable.lowerBound);
                return beta;
            }
            if (score > alpha) {
                evaluationBound = tTable.exact;
                alpha = score;
            }
        }
        tTable.saveScore(layersToExplore, currDepth, alpha, evaluationBound);
        return alpha;
    }

    private int quiescenceNegamax(int colour, int layersToExplore, int alpha, int beta) {
        int evaluation = evaluateBoard(colour);
        if (evaluation >= beta) {
            return beta;
        }
        alpha = Math.max(alpha, evaluation);
        if (layersToExplore == 0) {
            return alpha;
        }
        List<Move> allMoves = moveGenerator.computeAllMoves(colour, true);
        allMoves = sort(allMoves, moveGenerator.oppAttackMap,
                moveGenerator.oppPawnAttackMap, board.getGameStage());
        for (Move move : allMoves) {
            board.makeMove(move, true);
            int score = -quiescenceNegamax(-colour, layersToExplore - 1, -beta, -alpha);
            board.unmakeMove(move);

            if (score >= beta) {
                return beta;
            }
            alpha = Math.max(alpha, score);
        }
        return alpha;
    }

    private int evaluateBoard(int colour) {
        int score = 0;
        int gameStage = board.getGameStage();
        score += getMaterialScore(board.getMaterial());
        for (int i = 0; i < board.chessboard.length; i++) {
            if (board.chessboard[i] == 0) {
                continue;
            }
            if(level != 1) {
                score += getPositionScore(board.chessboard[i], i, gameStage);
            }
        }
        return score * colour;
    }

    private int getMaterialScore(int[][] material) {
        int score = 0;
        for (int i = 1; i < material[0].length; i++){
            score += materialValue[i] * material[0][i]; // add for white
            score -= materialValue[i] * material[1][i]; // subtract for black
        }
        return score;
    }
}