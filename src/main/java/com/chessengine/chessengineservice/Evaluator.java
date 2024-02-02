package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.Helpers.EvaluatorHelper;
import com.chessengine.chessengineservice.Helpers.FenHelper;
import com.chessengine.chessengineservice.MoveGenerator.MoveGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.chessengine.chessengineservice.Piece.*;

public class Evaluator {

    MoveGenerator moveGenerator;
    HashMap<String, List<Move>> moveCache;
    FenHelper fenHelper;
    private final int EVALUATION_DEPTH = 3;
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

        List<Move> allMoves = moveGenerator.generateAllMoves(colour, board);
        System.out.println(allMoves.size());
        List<Move> bestMoves = new ArrayList<>();
        int bestScore = MIN_VALUE;

        for (Move move : allMoves) {
            board.makeMove(move, true);

            int score = -negamax(-colour, board, EVALUATION_DEPTH, 0, MIN_VALUE, MAX_VALUE);
            System.out.println("From " + move.currentSquare/8 + ", " + move.currentSquare%8 + " to " + move.targetSquare/8 + ", " + move.targetSquare%8 + " - " + score);

            if (score == bestScore) {
                bestMoves.add(move);
            } else if (score > bestScore) {
                bestMoves.clear();
                bestMoves.add(move);
                bestScore = score;
            }
            board.unmakeMove(move);
        }

        if (bestMoves.isEmpty()) {
            return null;
        }
        moveCache.put(fenCode, bestMoves);
        return bestMoves.get(ThreadLocalRandom.current().nextInt(0, bestMoves.size())); //TODO: don't do it randomly, if capturing do it with the least value piece
    }

    private int negamax(int colour, Board board, int depth, int plyFromRoot, int alpha, int beta) {
        if (depth == 0) {
            return evaluateBoard(colour, board);
            //TODO: instead of evaluating board immediately, start Quiescence search to get to a quiet position
            //return quiescenceNegamax(colour, board, alpha, beta);
        }

        List<Move> allMoves = moveGenerator.generateAllMoves(colour, board);
        if (allMoves.isEmpty()) {
            if (board.isInCheck(colour)) {
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
        List<Move> allMoves = moveGenerator.generateAllMoves(colour, board); //TODO: generate captures only
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
        for (int i = 0; i < board.square.length; i++) {
            if (board.square[i] == 0) {
                continue;
            }
            score += getMaterialScore(board.square[i]); //TODO should there be any weights
            score += getPositionScore(board.square[i], i, gameStage);
        }
        score += getCheckingScore(colour, board);
        return score * colour;
    }

    private int getMaterialScore(int piece) {
        return materialValue[Math.abs(piece)] * (piece > 0 ? 1 : -1);
    }

    private int getMobilityScore(int pos, Board board) {
        return moveGenerator.getValidMoves(pos, board).size() * (board.square[pos] > 0 ? 1 : -1);
    }

    private int getPositionScore(int piece, int pos, int gameStage) {
        if (piece == PAWN) {
            return EvaluatorHelper.WHITE_PAWN_TABLE[pos];
        }
        else if (piece == -PAWN) {
            return -EvaluatorHelper.BLACK_PAWN_TABLE[pos];
        }
        else if (piece == KNIGHT) {
            return EvaluatorHelper.WHITE_KNIGHT_TABLE[pos];
        }
        else if (piece == -KNIGHT) {
            return -EvaluatorHelper.BLACK_KNIGHT_TABLE[pos];
        }
        else if (piece == BISHOP) {
            return EvaluatorHelper.WHITE_BISHOP_TABLE[pos];
        }
        else if (piece == -BISHOP) {
            return -EvaluatorHelper.BLACK_BISHOP_TABLE[pos];
        }
        else if (piece == ROOK) {
            return EvaluatorHelper.WHITE_ROOK_TABLE[pos];
        }
        else if (piece == -ROOK) {
            return -EvaluatorHelper.BLACK_ROOK_TABLE[pos];
        }
        else if (piece == QUEEN) {
            return EvaluatorHelper.WHITE_QUEEN_TABLE[pos];
        }
        else if (piece == -QUEEN) {
            return -EvaluatorHelper.BLACK_QUEEN_TABLE[pos];
        }
        else if (piece == KING && gameStage < 2) {
            return gameStage < 2
                    ? EvaluatorHelper.WHITE_KING_TABLE_MIDDLE[pos]
                    : EvaluatorHelper.WHITE_KING_TABLE_END[pos]; //TODO: change it dependding on the game state middle/ending
        }
        else if (piece == -KING && gameStage < 2) {
            return gameStage < 2 //TODO: update gameStage to endgame
                    ? -EvaluatorHelper.BLACK_KING_TABLE_MIDDLE[pos]
                    : -EvaluatorHelper.BLACK_KING_TABLE_END[pos];
        }
        return 0;
    }

    private int getCheckingScore(int colour, Board board) {
        int score = 0;
        int opponentKingPosition = board.getKingPosition(-colour);
        //moveGenerator.getAttackMoves() //TODO: implement bitboard to keep squares that are being attacked
        return score;
    }

    private void printPrevMoves(ArrayList<Move> prevMoves, int score) {
        prevMoves.forEach(move -> {
            int fromX = move.currentSquare / 8;
            int fromY = move.currentSquare % 8;
            int toX = move.targetSquare / 8;
            int toY = move.targetSquare % 8;
            //System.out.print(", [from (" + fromX + ", " + fromY + ") to (" + toX + ", " + toY + ")] ");
        });
        System.out.println("score: " + score);
    }
}