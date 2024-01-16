package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.Helpers.EvaluatorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.chessengine.chessengineservice.Piece.*;

public class Evaluator {

    MoveGenerator moveGenerator;
    private static int moveCount = 0;
    private final int EVALUATION_DEPTH = 3;

    public Evaluator() {
        moveGenerator = new MoveGenerator();
    }

    public Move getBestMove(int colour, Board board) {

        List<Move> allMoves = moveGenerator.generateAllMoves(colour, board);
        System.out.println(allMoves.size());
        List<Move> bestMoves = new ArrayList<>();
        int bestScore = Integer.MIN_VALUE;

        for(Move move : allMoves) {
            board.makeMove(move, true);

            int score = -negamax(-colour, board, EVALUATION_DEPTH-1, Integer.MIN_VALUE, Integer.MAX_VALUE);
            System.out.println("From " + move.currentSquare/8 + ", " + move.currentSquare%8 + " to " + move.targetSquare/8 + ", " + move.targetSquare%8 + " - " + score);

            if(score == bestScore) {
                bestMoves.add(move);
            } else if (score > bestScore) {
                bestMoves.clear();
                bestMoves.add(move);
                bestScore = score;
            }
            board.unmakeMove(move);
        }

        if(bestMoves.isEmpty()) {
            return null;
        }
        return bestMoves.get(ThreadLocalRandom.current().nextInt(0, bestMoves.size())); //TODO: don't do it randomly, if capturing do it with the least value piece
    }

    private int negamax(int colour, Board board, int depth, int alpha, int beta) {
        if (depth == 0) {
            var x = evaluateBoard(board) * colour;
            //printPrevMoves(prevMoves, x);
            return x;
        }

        List<Move> allMoves = moveGenerator.generateAllMoves(colour, board);
        if (allMoves.isEmpty()) {
            if (board.isInCheck(colour)) {
                return MIN_VALUE;
            }
            return 0;
        }
        //if is in check, return penalty for being in check

        int bestScore = Integer.MIN_VALUE;
        for (Move move : allMoves) {
            board.makeMove(move, true);
            int score = -negamax(-colour, board, depth - 1, -beta, -alpha);
            board.unmakeMove(move);

            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, score);

            if (alpha >= beta) {
                break;
            }
        }
        return bestScore;
    }

    private int evaluateBoard(Board board) {
        int score = 0;
        for (int i = 0; i < board.square.length; i++) {
            if(board.square[i] == 0) {
                continue;
            }
            score += getMaterialScore(board.square[i]); //TODO should there be any weights
            score += getPositionScore(board.square[i], i);
        }
        return score;
    }

    private int getMaterialScore(int piece) {
        int[] materialValue = {0, 100, 320, 330, 500, 900, 0};
        return materialValue[Math.abs(piece)] * (piece > 0 ? 1 : -1);
    }

    private int getMobilityScore(int pos, Board board) {
        return moveGenerator.getValidMoves(pos, board).size() * (board.square[pos] > 0 ? 1 : -1);
    }

    private int getPositionScore(int piece, int pos) {
        if(piece == PAWN) {
            return EvaluatorHelper.WHITE_PAWN_TABLE[pos];
        }
        else if(piece == -PAWN) {
            return -EvaluatorHelper.BLACK_PAWN_TABLE[pos];
        }
        else if(piece == KNIGHT) {
            return EvaluatorHelper.WHITE_KNIGHT_TABLE[pos];
        }
        else if(piece == -KNIGHT) {
            return -EvaluatorHelper.BLACK_KNIGHT_TABLE[pos];
        }
        else if(piece == BISHOP) {
            return EvaluatorHelper.WHITE_BISHOP_TABLE[pos];
        }
        else if(piece == -BISHOP) {
            return -EvaluatorHelper.BLACK_BISHOP_TABLE[pos];
        }
        else if(piece == ROOK) {
            return EvaluatorHelper.WHITE_ROOK_TABLE[pos];
        }
        else if(piece == -ROOK) {
            return -EvaluatorHelper.BLACK_ROOK_TABLE[pos];
        }
        else if(piece == QUEEN) {
            return EvaluatorHelper.WHITE_QUEEN_TABLE[pos];
        }
        else if(piece == -QUEEN) {
            return -EvaluatorHelper.BLACK_QUEEN_TABLE[pos];
        }
        else if(piece == KING) {
            return EvaluatorHelper.WHITE_KING_TABLE_MIDDLE[pos]; //TODO: change it dependding on the game state middle/ending
        }
        else if(piece == -KING) {
            return -EvaluatorHelper.BLACK_KING_TABLE_MIDDLE[pos];
        }
        return 0;
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