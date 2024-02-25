package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.Helpers.FenHelper;
import com.chessengine.chessengineservice.MoveGenerator.MoveGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.chessengine.chessengineservice.Helpers.EvaluatorHelper.getPositionScore;
import static com.chessengine.chessengineservice.MoveSorter.sort;
import static com.chessengine.chessengineservice.Piece.PAWN;
import static java.lang.Math.abs;

public class Evaluator {

    MoveGenerator moveGenerator;
    HashMap<String, List<Move>> moveCache;
    FenHelper fenHelper;
    Board board;
    TranspositionTable tTable;
    private final int EVALUATION_DEPTH = 5;
    private final int QSEARCH_DEPTH = 4;
    private final int MAX_VALUE = 1000000000;
    private final int MIN_VALUE = -1000000000;
    public final int maxNumOfExtensions = 2;
    public static int[] materialValue = {0, 100, 320, 330, 500, 900, 0};

    public Evaluator(Board board) {
        this.board = board;
        moveGenerator = board.moveGenerator;
        moveCache = new HashMap<>();
        fenHelper = new FenHelper();
        tTable = board.tTable;
    }

    public Move getBestMove(int colour) {
        List<Move> allMoves = moveGenerator.generateMoves(colour, false);
        allMoves = sort(null, allMoves, moveGenerator.opponentAttackMap, moveGenerator.opponentPawnAttackMap, board.getGameStage(), false, 0);
        List<Move> bestMoves = new ArrayList<>();
        int bestScore = MIN_VALUE;

        for (Move move : allMoves) {
            board.makeMove(move, true);
            int score = -negamax(-colour, EVALUATION_DEPTH-1, 1, MIN_VALUE, MAX_VALUE, 0);
            board.unmakeMove(move);
            //System.out.println("From " + move.startSquare/8 + ", " + move.startSquare%8 + " to " + move.targetSquare/8 + ", " + move.targetSquare%8 + " - " + score);

            if (score == bestScore) {
                bestMoves.add(move);
            } else if (score > bestScore) {
                bestMoves.clear();
                bestMoves.add(move);
                bestScore = score;
            }
        }
        return bestMoves.isEmpty() ? null : bestMoves.get(0);
    }

    private int negamax(int colour, int depth, int plyFromRoot, int alpha, int beta, int numOfExtensions) {
        int ttScore = tTable.lookupEvaluation(depth, plyFromRoot, alpha, beta);
        if (ttScore != -1) {
            return ttScore;
        }
        if (depth == 0) {
            return quiescenceNegamax(colour, QSEARCH_DEPTH, alpha, beta);
        }

        List<Move> allMoves = moveGenerator.generateMoves(colour, false);
        allMoves = sort(null, allMoves, moveGenerator.opponentAttackMap, moveGenerator.opponentPawnAttackMap, board.getGameStage(), false, 0);
        if (allMoves.isEmpty()) {
            if (moveGenerator.isKingInCheck()) {
                return MIN_VALUE + plyFromRoot; // if there are more ways to get a mate it prevents mate in the quickest way.
            }
            return 0; //TODO: add a penalty for a draw
        }

        int evaluationBound = tTable.upperBound;
        Move bestMove = allMoves.getFirst();
        for (int i = 0; i < allMoves.size(); i++) {
            Move move = allMoves.get(i);
            board.makeMove(move, true);

            boolean isCapture = board.square[move.targetSquare] != 0;
            int extension = 0;
            if (numOfExtensions < maxNumOfExtensions) {
                if (moveGenerator.isKingInCheck() || (abs(move.piece) == PAWN && (move.toX == 1 || move.toX == 6))) {
                    extension = 1;
                }
            }
            boolean fullSearch = true;
            int score = 0;
            if (extension == 0 && depth >= 3 && i >= 3 && !isCapture) { // Reduce the depth of the search for moves later in the move list as these are less likely to be good (assuming our move ordering isn't terrible)
                int reduceDepth = 1;
                score = -negamax(-colour, depth - 1 - reduceDepth, plyFromRoot + 1, -alpha - 1, -alpha, numOfExtensions);
                // If the evaluation is better than expected, we'd better to a full-depth search to get a more accurate evaluation
                fullSearch = score > alpha;
            }
            if (fullSearch) {
                score = -negamax(-colour, depth - 1 + extension, plyFromRoot + 1, -beta, -alpha, numOfExtensions + extension);
            }

            //int score = -negamax(-colour, depth - 1, plyFromRoot + 1, -beta, -alpha, numOfExtentions);
            board.unmakeMove(move);

            // (Beta-cutoff / Fail high) Move was *too* good, opponent will choose a different move earlier on to avoid this position.
            if (score >= beta) {
                tTable.storeEvaluation(depth, plyFromRoot, beta, tTable.lowerBound, move);
                return beta;
            }
            if (score > alpha) {
                evaluationBound = tTable.exact;
                bestMove = move;
                alpha = score;
            }
        }
        tTable.storeEvaluation(depth, plyFromRoot, alpha, evaluationBound, bestMove); //TODO: should i delete bestMove from storing
        return alpha;
    }

    // Search capture moves until a 'quiet' position is reached.
    private int quiescenceNegamax(int colour, int depth, int alpha, int beta) {
        int evaluation = evaluateBoard(colour);
        if (evaluation >= beta) {
            return beta;
        }
        alpha = Math.max(alpha, evaluation);
        if (depth == 0) {
            return alpha;
        }
        List<Move> allMoves = moveGenerator.generateMoves(colour, true);
        allMoves = sort(null, allMoves, moveGenerator.opponentAttackMap, moveGenerator.opponentPawnAttackMap, board.getGameStage(), true, 0);
        for (Move move : allMoves) {
            board.makeMove(move, true);
            int score = -quiescenceNegamax(-colour, depth - 1, -beta, -alpha);
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
        for (int i = 0; i < board.square.length; i++) {
            if (board.square[i] == 0) {
                continue;
            }
            score += getPositionScore(board.square[i], i, gameStage);
        }
        score += getCheckingScore(colour, gameStage);
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

    private int getMobilityScore(int pos) {
        return 0; //TODO: reimplement mobility score
        //return moveGenerator.getValidMoves(pos, board).size() * (board.square[pos] > 0 ? 1 : -1);
    }

    private int getCheckingScore(int colour, int gameStage) {
        int score = 0;
        int opponentKingPosition = board.getKingPosition(-colour);
        //moveGenerator.getAttackMoves() //TODO: use bitboard to keep squares that are being attacked
        return score;
    }

    private void printPrevMoves(ArrayList<Move> prevMoves, int score) {
        prevMoves.forEach(move -> {
            int fromX = move.startSquare / 8;
            int fromY = move.startSquare % 8;
            int toX = move.targetSquare / 8;
            int toY = move.targetSquare % 8;
            //System.out.print(", [from (" + fromX + ", " + fromY + ") to (" + toX + ", " + toY + ")] ");
        });
        System.out.println("score: " + score);
    }
}