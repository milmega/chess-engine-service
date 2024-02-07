package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.Helpers.FenHelper;
import com.chessengine.chessengineservice.MoveGenerator.MoveGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.chessengine.chessengineservice.Helpers.EvaluatorHelper.getPositionScore;
import static com.chessengine.chessengineservice.MoveSorter.sort;

public class Evaluator {

    MoveGenerator moveGenerator;
    HashMap<String, List<Move>> moveCache;
    FenHelper fenHelper;
    private final int EVALUATION_DEPTH = 4;
    private final int MAX_VALUE = 1000000000;
    private final int MIN_VALUE = -1000000000;
    public static int[] materialValue = {0, 100, 320, 330, 500, 900, 0};

    public Evaluator() {
        moveGenerator = new MoveGenerator();
        moveCache = new HashMap<>();
        fenHelper = new FenHelper();
    }

    public Move getBestMove(int colour, Board board) {
        //check if current board has been already evaluated
        String fenCode = fenHelper.ConvertBoardToFenCode(board);
        List<Move> movesFromCache = moveCache.getOrDefault(fenCode, new ArrayList<>()); //TODO: is this map not gonna be too big?
        if (!movesFromCache.isEmpty()) { //TODO: dont do it randomly
            return movesFromCache.get(ThreadLocalRandom.current().nextInt(0, movesFromCache.size()));
        }

        List<Move> allMoves = moveGenerator.generateMoves(colour, board, false);
        allMoves = sort(null, allMoves, moveGenerator.opponentAttackMap, moveGenerator.opponentPawnAttackMap, board.getGameStage(), false, 0);
        System.out.println(allMoves.size());
        List<Move> bestMoves = new ArrayList<>();
        int bestScore = MIN_VALUE;

        for (Move move : allMoves) {
            board.makeMove(move, true);
            int score = -negamax(-colour, board, EVALUATION_DEPTH-1, 0, MIN_VALUE, MAX_VALUE);
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
        //moveCache.put(fenCode, bestMoves);

        //TODO: don't do it randomly, if capturing do it with the least value piece
        return bestMoves.isEmpty() ? null : bestMoves.get(ThreadLocalRandom.current().nextInt(0, bestMoves.size()));
    }

    private int negamax(int colour, Board board, int depth, int plyFromRoot, int alpha, int beta) {
        if (depth == 0) {
            return evaluateBoard(colour, board);
            //TODO: instead of evaluating board immediately, start Quiescence search to get to a quiet position
            //return quiescenceNegamax(colour, board, alpha, beta);
        }

        List<Move> allMoves = moveGenerator.generateMoves(colour, board, false);
        if (allMoves.isEmpty()) {
            if (moveGenerator.isKingInCheck()) {
                return MIN_VALUE + plyFromRoot; // if there are more ways to get a mate it prevents mate in the quickest way.
            }
            return 0; //TODO: add a penalty for a draw
        }
        //if is in check, return penalty for being in check

        for (Move move : allMoves) {
            board.makeMove(move, true);
            int score = -negamax(-colour, board, depth - 1, plyFromRoot + 1, -beta, -alpha);
            board.unmakeMove(move);

            // Move was *too* good, opponent will choose a different move earlier on to avoid this position.
            // (Beta-cutoff / Fail high)
            if (score >= beta) {
                return beta;
            }
            alpha = Math.max(alpha, score);
        }
        return alpha;
    }

    // Search capture moves until a 'quiet' position is reached.
    private int quiescenceNegamax(int colour, Board board, int alpha, int beta) {
        int evaluation = evaluateBoard(colour, board);
        if (evaluation >= beta) {
            return beta;
        }
        alpha = Math.max(alpha, evaluation);
        List<Move> allMoves = moveGenerator.generateMoves(colour, board, true);
        allMoves = sort(null, allMoves, moveGenerator.opponentAttackMap, moveGenerator.opponentPawnAttackMap, board.getGameStage(), true, 0);
        for (Move move : allMoves) {
            board.makeMove(move, true);
            int score = -quiescenceNegamax(-colour, board, -beta, -alpha);
            board.unmakeMove(move);

            if (score >= beta) {
                return beta;
            }
            alpha = Math.max(alpha, score);
        }
        return alpha;
    }

    private int evaluateBoard(int colour, Board board) {
        int score = 0;
        int gameStage = board.getGameStage();
        score += getMaterialScore(board.getMaterial());
        for (int i = 0; i < board.square.length; i++) {
            if (board.square[i] == 0) {
                continue;
            }
            score += getMaterialScore(board.square[i]); //TODO should there be any weights
            score += getPositionScore(board.square[i], i, gameStage);
        }
        score += getCheckingScore(colour, gameStage, board);
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

    private int getMobilityScore(int pos, Board board) {
        return 0; //TODO: reimplement mobility score
        //return moveGenerator.getValidMoves(pos, board).size() * (board.square[pos] > 0 ? 1 : -1);
    }

    private int getCheckingScore(int colour, int gameStage, Board board) {
        int score = 0;
        int opponentKingPosition = board.getKingPosition(-colour);
        //moveGenerator.getAttackMoves() //TODO: implement bitboard to keep squares that are being attacked
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