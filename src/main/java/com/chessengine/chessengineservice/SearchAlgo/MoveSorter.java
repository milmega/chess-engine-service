package com.chessengine.chessengineservice.SearchAlgo;

import com.chessengine.chessengineservice.Structures.Move;
import com.chessengine.chessengineservice.Structures.Piece;

import java.util.Arrays;
import java.util.List;

import static com.chessengine.chessengineservice.Helpers.BitboardHelper.isBitSet;
import static com.chessengine.chessengineservice.Helpers.EvaluatorHelper.getPositionScore;
import static com.chessengine.chessengineservice.Structures.Piece.KING;
import static com.chessengine.chessengineservice.Structures.Piece.PAWN;
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
            int piece = move.piece;
            int pieceType = abs(piece);
            int pieceValue = Evaluator.materialValue[pieceType];
            int targetPiece = move.targetPiece;
            int fromPosition = move.startSquare;
            int toPosition = move.targetSquare;
            boolean isCapture = targetPiece != Piece.EMPTY;
            boolean promotionFlag = move.promotionFlag;


            if (pieceType == PAWN) {
                if (promotionFlag && !isCapture) {
                    score += promotionScore;
                }
            } else if (pieceType < KING){
                int destinationScore = getPositionScore(piece, toPosition, gameStage) * colour;
                int startScore = getPositionScore(piece, fromPosition, gameStage) * colour;
                score += destinationScore - startScore;

                if (isBitSet(pawnAttackMap, toPosition)) {
                    score -= 50;
                } else if (isBitSet(attackMap, toPosition)) {
                    score -= 25;
                }
            }
            if (isCapture) {
                int captureScoreGain = Evaluator.materialValue[abs(targetPiece)] - pieceValue;
                boolean isRecapturePossible = isBitSet(attackMap | pawnAttackMap, toPosition);
                if (isRecapturePossible) {
                    score += (captureScoreGain >= 0 ? positiveCaptureScore : negativeCaptureScore) + captureScoreGain;
                } else {
                    score += positiveCaptureScore + captureScoreGain;
                }
            }
            move.score = score;
        }
        sortMoves(movesAsArray, 0, movesAsArray.length-1);
        return Arrays.stream(movesAsArray).toList();
    }

    static void sortMoves(Move[] moves, int left, int right) {
        if (left < right) {
            int pivot = partition(moves, left, right);
            sortMoves(moves, left, pivot - 1);
            sortMoves(moves, pivot + 1, right);
        }
    }

    static int partition(Move[] moves, int left, int right) {
        int pivot = moves[right].score;
        int index = left - 1;

        for (int j = left; j < right; j++) {
            if (moves[j].score >= pivot) {
                index++;
                Move temp = moves[index].getCopy();
                moves[index] = moves[j].getCopy();
                moves[j] = temp;
            }
        }
        Move temp = moves[index + 1].getCopy();
        moves[index + 1] = moves[right].getCopy();
        moves[right] = temp;
        return index + 1;
    }
}
