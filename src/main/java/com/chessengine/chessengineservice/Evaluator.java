package com.chessengine.chessengineservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Evaluator {

    MoveGenerator moveGenerator;
    private static int moveCount = 0;
    private final int EVALUATION_DEPTH = 3;

    public Evaluator() {
        moveGenerator = new MoveGenerator();
    }

    public Move getBestMove(int colour, int[][] board) {

        List<Move> allMoves = moveGenerator.generateAllMoves(colour, board);

        Move bestMove = null;
        int bestScore = colour > 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : allMoves) {
            //make move
            int tmpPiece = board[move.getDestination().getX()][move.getDestination().getY()];
            makeMove(move, board);
            var prevMoves = new ArrayList<Move>();
            prevMoves.add(move);
            int score = minimax(-colour, board, EVALUATION_DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, -colour > 0, prevMoves).first; //TODO: add score proeprty to move so it can be evaluated and retuenred

            System.out.println("score: " + score + ", bestScore: " + bestScore);
            //printMove(move, score, colour, EVALUATION_DEPTH);
            if ((colour > 0 && score > bestScore) || (colour < 0 && score < bestScore)) {
                bestScore = score;
                bestMove = new Move(move.getX(), move.getY(), move.getDestination().getX(), move.getDestination().getY());

            }
            System.out.println("best score: " + bestScore);
            prevMoves.removeLast();
            //unmake move
            unmakeMove(move, tmpPiece, board);
        }
        System.out.println("Making move from: " + bestMove + " with score: " + bestScore);
        return bestMove;
    }

    private Pair<Integer, Move> minimax(int colour, int[][] board, int depth, int alpha, int beta, boolean maximizingPlayer, ArrayList<Move> prevMoves) {

        if (depth == 0) {
            var x = evaluateBoard(board);
            printPrevMoves(prevMoves, x);
            return new Pair<>(x, null);
        }


        List<Move> allMoves = moveGenerator.generateAllMoves(colour, board);
        if (allMoves.isEmpty()) {
            if (moveGenerator.isKingCheckmated(colour, board)) {
                return new Pair(maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE, null);
            }
            return new Pair(0, null);
        }
        int bestScore = 0;
        Move bestMove = null;
        if (maximizingPlayer) {
            bestScore = Integer.MIN_VALUE;
            for (Move move : allMoves) {
                int tmpPiece = board[move.getDestination().getX()][move.getDestination().getY()];
                makeMove(move, board);
                prevMoves.add(move);
                int score = minimax(-colour, board, depth-1, -beta, -alpha, false, prevMoves).first; //TODO: double check  alpha and beta values
                //printMove(move, score, colour, depth);
                unmakeMove(move, tmpPiece, board);
                prevMoves.removeLast();
                //bestScore = Math.max(bestScore, score);
                if(score > bestScore) {
                    bestScore = score;
                    bestMove = move.getCopy();
                }
                alpha = Math.max(alpha, bestScore);
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            bestScore = Integer.MAX_VALUE;
            for (Move move : allMoves) {
                int tmpPiece = board[move.getDestination().getX()][move.getDestination().getY()];
                makeMove(move, board);
                prevMoves.add(move);
                int score = minimax(-colour, board, depth-1, -beta, -alpha, true, prevMoves).first;
                unmakeMove(move, tmpPiece, board);
                prevMoves.removeLast();
                //bestScore = Math.min(bestScore, score);
                //printMove(move, score, colour, depth);
                if(score < bestScore) {
                    bestScore = score;
                    bestMove = move.getCopy();
                }
                beta = Math.min(beta, bestScore);
                if (beta <= alpha) {
                    break;
                }
            }
        }
        return new Pair<>(bestScore, bestMove);
    }

    private int evaluateBoard(int[][] board) {
        int score = 0;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if(board[i][j] == 0) {
                    continue;
                }
                score += getMaterialBalanceScore(board[i][j]); //TODO: ADD Weights
                //score += getMobilityScore(board[i][j], i, j, board);
                //score += getControlScore(board[i][j], i, j);
            }
        }
        return score;
    }

    private int getMaterialBalanceScore(int piece) {
        int[] materialValue = {0, 100, 300, 300, 500, 900, 10000};
        return materialValue[Math.abs(piece)] * (piece > 0 ? 1 : -1);
    }

    private int getMobilityScore(int piece, int x, int y, int[][] board) {
        return moveGenerator.getValidMoves(piece, x, y, GameEngine.getDeepCopy(board)).size() * (piece > 0 ? 1 : -1);
    }

    private int getControlScore(int piece, int x, int y) {
        if(piece == moveGenerator.PAWN) {
            return EvaluatorHelper.WHITE_PAWN_TABLE[x][y];
        }
        else if(piece == -moveGenerator.PAWN) {
            return -EvaluatorHelper.BLACK_PAWN_TABLE[x][y];
        }
        else if(piece == moveGenerator.KNIGHT) {
            return EvaluatorHelper.WHITE_KNIGHT_TABLE[x][y];
        }
        else if(piece == -moveGenerator.KNIGHT) {
            return -EvaluatorHelper.BLACK_KNIGHT_TABLE[x][y];
        }
        else if(piece == moveGenerator.BISHOP) {
            return EvaluatorHelper.WHITE_BISHOP_TABLE[x][y];
        }
        else if(piece == -moveGenerator.BISHOP) {
            return -EvaluatorHelper.BLACK_BISHOP_TABLE[x][y];
        }
        else if(piece == moveGenerator.ROOK) {
            return EvaluatorHelper.WHITE_ROOK_TABLE[x][y];
        }
        else if(piece == -moveGenerator.ROOK) {
            return -EvaluatorHelper.BLACK_ROOK_TABLE[x][y];
        }
        else if(piece == moveGenerator.QUEEN) {
            return EvaluatorHelper.WHITE_QUEEN_TABLE[x][y];
        }
        else if(piece == -moveGenerator.QUEEN) {
            return -EvaluatorHelper.BLACK_QUEEN_TABLE[x][y];
        }
        else if(piece == moveGenerator.KING) {
            return EvaluatorHelper.WHITE_KING_TABLE[x][y];
        }
        else if(piece == -moveGenerator.KING) {
            return -EvaluatorHelper.BLACK_KING_TABLE[x][y];
        }
        return 0;
    }

    private void makeMove(Move move, int[][] board) {
        Move destination = move.getDestination();
        board[destination.getX()][destination.getY()] = board[move.getX()][move.getY()];
        board[move.getX()][move.getY()] = 0;
    }

    private void unmakeMove(Move move, int tmpPiece, int[][] board) {
        board[move.getX()][move.getY()] = board[move.getDestination().getX()][move.getDestination().getY()];
        board[move.getDestination().getX()][move.getDestination().getY()] = tmpPiece;
    }

    private void printMove(Move move, int score, int colour, int depth) {
        moveCount++;
        for(int i = 0; i < 3-depth; i++) {
            System.out.print(" ");
        }
        System.out.print(depth + " - " + moveCount + " - ");
        System.out.println((colour > 0 ? "white: " : "black: ") + "Move from " + move.getX() + ", " + move.getY() + " to " + move.getDestination().getX() + ", " + move.getDestination().getY() + " is " + score);
    }

    private void printPrevMoves(ArrayList<Move> prevMoves, int score) {
        prevMoves.forEach(m -> {
            System.out.print("from (" + m.getX() + ", " + m.getY() + ") to (" + m.getDestination().getX() + ", " + m.getDestination().getY() + "), ");
        });
        System.out.println(", score: " + score);
    }
}