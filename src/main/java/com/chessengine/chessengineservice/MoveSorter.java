package com.chessengine.chessengineservice;

import java.util.Arrays;
import java.util.List;

import static com.chessengine.chessengineservice.Helpers.BitboardHelper.isBitSet;
import static com.chessengine.chessengineservice.Helpers.EvaluatorHelper.getPositionScore;
import static com.chessengine.chessengineservice.Piece.KING;
import static com.chessengine.chessengineservice.Piece.PAWN;
import static java.lang.Math.abs;

public class MoveSorter {

    private static final int winningCaptureBias = 8_000_000;
    private static final int losingCaptureBias = 2_000_000;
    private static final int promotionBias = 6_000_000;
    private static final int hashMoveScore = 100_000_000;


    public MoveSorter() {
    }

    public static List<Move> sort(Move hashMove, List<Move> moves, long attackMap, long pawnAttackMap, int gameStage, boolean quiescenceSearch, int ply) {
        Move[] movesAsArray = moves.toArray(new Move[0]);

        for (int i = 0; i < movesAsArray.length; i++) {

            Move move = movesAsArray[i]; //TODO: add rest of stuff from c# project
            /*if (hashMove != null && move.equals(hashMove))
            {
                scoresArray[i] = hashMoveScore;
                continue;
            }*/
            int score = 0;
            int colour = move.colour;
            int startSquare = move.startSquare;
            int targetSquare = move.targetSquare;
            int movePiece = move.piece;
            int pieceType = abs(movePiece);
            int targetPiece = move.targetPiece;
            boolean isCapture = targetPiece != Piece.EMPTY;
            boolean promotionFlag = move.promotionFlag;
            int pieceValue = Evaluator.materialValue[pieceType];

            if (isCapture) {
                // Order moves to try capturing the most valuable opponent piece with least valuable of own pieces first
                int captureMaterialDelta = Evaluator.materialValue[abs(targetPiece)] - pieceValue;
                boolean opponentCanRecapture = isBitSet(attackMap | pawnAttackMap, targetSquare);
                if (opponentCanRecapture) {
                    score += (captureMaterialDelta >= 0 ? winningCaptureBias : losingCaptureBias) + captureMaterialDelta;
                }
                else {
                    score += winningCaptureBias + captureMaterialDelta;
                }
            }
            if (pieceType == PAWN) {
                if (promotionFlag && !isCapture) {
                    score += promotionBias;
                }
            } else if (pieceType < KING){
                int toScore = getPositionScore(movePiece, targetSquare, gameStage) * colour;
                int fromScore = getPositionScore(movePiece, startSquare, gameStage) * colour;
                score += toScore - fromScore;

                if (isBitSet(pawnAttackMap, targetSquare)) {
                    score -= 50;
                }
                else if (isBitSet(attackMap, targetSquare)) {
                    score -= 25;
                }
            }
            move.score = score;
        }
        quickSort(movesAsArray, 0, movesAsArray.length-1);
        return Arrays.stream(movesAsArray).toList();
    }

    static void quickSort(Move[] moves, int left, int right) {
        if(left < right) {
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
