package com.chessengine.chessengineservice;

import java.util.Arrays;
import java.util.List;

import static com.chessengine.chessengineservice.Helpers.BitboardHelper.isBitSet;
import static com.chessengine.chessengineservice.Helpers.EvaluatorHelper.getPositionScore;
import static com.chessengine.chessengineservice.Piece.KING;
import static com.chessengine.chessengineservice.Piece.PAWN;
import static java.lang.Math.abs;

public class MoveSorter {

    private static final int positiveCaptureScore = 8_000_000;
    private static final int negativeCaptureScore = 2_000_000;
    private static final int promotionScore = 6_000_000;

    public MoveSorter() {}

    public static List<Move> sort(List<Move> moves, long attackMap, long pawnAttackMap, int gameStage) {
        Move[] movesAsArray = moves.toArray(new Move[0]);

        for(Move move : movesAsArray) {
            int score = 0;
            int colour = move.colour;
            int start = move.startSquare;
            int target = move.targetSquare;
            int movePiece = move.piece;
            int pieceType = abs(movePiece);
            int targetPiece = move.targetPiece;
            boolean isCapture = targetPiece != Piece.EMPTY;
            boolean promotionFlag = move.promotionFlag;
            int pieceValue = Evaluator.materialValue[pieceType];

            if (pieceType == PAWN) {
                if (promotionFlag && !isCapture) {
                    score += promotionScore;
                }
            } else if (pieceType < KING){
                int toScore = getPositionScore(movePiece, target, gameStage) * colour;
                int fromScore = getPositionScore(movePiece, start, gameStage) * colour;
                score += toScore - fromScore;

                if (isBitSet(pawnAttackMap, target)) {
                    score -= 50;
                }
                else if (isBitSet(attackMap, target)) {
                    score -= 25;
                }
            }
            if (isCapture) {
                int captureGain = Evaluator.materialValue[abs(targetPiece)] - pieceValue;
                boolean isRecapturePossible = isBitSet(attackMap | pawnAttackMap, target);
                if (isRecapturePossible) {
                    score += (captureGain >= 0 ? positiveCaptureScore : negativeCaptureScore) + captureGain;
                } else {
                    score += positiveCaptureScore + captureGain;
                }
            }
            move.score = score;
        }
        quickSort(movesAsArray, 0, movesAsArray.length-1);
        return Arrays.stream(movesAsArray).toList();
    }

    static void quickSort(Move[] moves, int left, int right) {
        if (left < right) {
            int pivot = partition(moves, left, right);
            quickSort(moves, left, pivot - 1);
            quickSort(moves, pivot + 1, right);
        }
    }

    static int partition(Move[] moves, int left, int right) {
        int pivot = moves[right].score;
        int i = left - 1;

        for (int j = left; j < right; j++) {
            if (moves[j].score >= pivot) {
                i++;
                Move temp = moves[i].getCopy();
                moves[i] = moves[j].getCopy();
                moves[j] = temp;
            }
        }
        Move temp = moves[i + 1].getCopy();
        moves[i + 1] = moves[right].getCopy();
        moves[right] = temp;
        return i + 1;
    }
}
