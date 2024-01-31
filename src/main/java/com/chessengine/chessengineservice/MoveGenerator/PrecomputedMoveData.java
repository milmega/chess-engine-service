package com.chessengine.chessengineservice.MoveGenerator;

import com.chessengine.chessengineservice.Helpers.BoardHelper;
import com.chessengine.chessengineservice.Pair;

import java.util.ArrayList;
import java.util.List;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.*;
import static java.lang.Math.*;

public class PrecomputedMoveData {

    public static long[][] pathMask;
    public static long[][] rayPathMask;

    //  (N, S, W, E, NW, SE, NE, SW)
    public static final int[] squareChangeOffset = { 8, -8, -1, 1, 7, -7, 9, -9 };
    public static final Pair<Integer, Integer>[] squareChangeOffset2D = new Pair[] {
            new Pair<>(0, 1),
            new Pair<>(0, -1),
            new Pair<>(-1, 0),
            new Pair<>(1, 0),
            new Pair<>(-1, 1),
            new Pair<>(1, -1),
            new Pair<>(1, 1),
            new Pair<>(-1, -1)
    };
    public static final int[] dirLookup; //TODO: is it even needed or used anywhere?

    public static final int[][] whitePawnAttacks;
    public static final int[][] blackPawnAttacks;
    public static final long[] knightAttackBitboard;
    public static final long[] kingAttackBitboard;
    public static final long[][] pawnAttackBitboard;

    // keeps number of squares from each position in every direction: N, S, W, E, NW, SE, NE, SW
    public static final int[][] distFromEdge;

    public static final long[] rookMoves;
    public static final long[] bishopMoves;
    public static final long[] queenMoves;
    public static final int[][] knightMoves;
    public static final int[][] kingMoves;
    // Aka manhattan distance (answers how many moves for a rook to get from square a to square b)
    public static int[][] orthogonalDist;
    // Aka chebyshev distance (answers how many moves for a king to get from square a to square b)
    public static int[][] kingDist;
    public static int[] centreManhattanDist;

    static {
        distFromEdge = new int[64][];

        rookMoves = new long[64];
        bishopMoves = new long[64];
        queenMoves = new long[64];
        knightMoves = new int[64][];
        kingMoves = new int[64][];

        whitePawnAttacks= new int[64][];
        blackPawnAttacks = new int[64][];
        pawnAttackBitboard = new long[64][];
        knightAttackBitboard = new long[64];
        kingAttackBitboard = new long[64];

        int[] possibleKnightJumps = new int[] { 15, 17, -17, -15, 10, -6, 6, -10 };

        for (int i = 0; i < 64; i++) {
            int x = posToX(i);
            int y = posToY(i);
            int north = x;
            int south = 7 - y;
            int west = y;
            int east = 7 - y;
            distFromEdge[i] = new int[8];
            distFromEdge[i][0] = north;
            distFromEdge[i][1] = south;
            distFromEdge[i][2] = west;
            distFromEdge[i][3] = east;
            distFromEdge[i][4] = min(north, west);
            distFromEdge[i][5] = min(south, east);
            distFromEdge[i][6] = min(north, east);
            distFromEdge[i][7] = min(south, west);

            // compute all squares that knight can jump to from the current square
            List<Integer> legalKnightMoves = new ArrayList<>();
            long knightBitboard = 0;
            for (int jumpDelta : possibleKnightJumps) {
                int newSquare = i + jumpDelta;
                if (newSquare >= 0 && newSquare < 64) {
                    int knightNewX = posToX(newSquare);
                    int knightNewY = posToY(newSquare);
                    // make sure knight moved max of 2 squares on x or y
                    int maxDistChange = max(abs(x - knightNewX), abs(y - knightNewY));
                    if (maxDistChange == 2) {
                        legalKnightMoves.add(newSquare);
                        knightBitboard |= 1L << newSquare;
                    }
                }
            }
            knightMoves[i] = legalKnightMoves.stream().mapToInt(Integer::intValue).toArray();
            knightAttackBitboard[i] = knightBitboard;

            // compute all square that king can go to (no castling)
            List<Integer> legalKingMoves = new ArrayList<>();
            for (int offset : squareChangeOffset) {
                int newSquare = i + offset;
                if (newSquare >= 0 && newSquare < 64) {
                    int kingNewX = posToX(newSquare);
                    int kingNewY = posToY(newSquare);
                    // make sure king moved max 1 square on x or y
                    int maxDistChange = max(abs(x - kingNewX), abs(y - kingNewY));
                    if (maxDistChange == 1)
                    {
                        legalKingMoves.add(newSquare);
                        kingAttackBitboard[i] |= 1L << newSquare;
                    }
                }
            }
            kingMoves[i] = legalKingMoves.stream().mapToInt(Integer::intValue).toArray();

            // compute legal pawn captures
            List<Integer> whitePawnCaptures = new ArrayList<>();
            List<Integer> blackPawnCaptures = new ArrayList<>();
            pawnAttackBitboard[i] = new long[2];
            if (y < 7) {
                if (x > 0) {
                    whitePawnCaptures.add(i - 7);
                    pawnAttackBitboard[i][0] |= 1L << (i - 7);
                }
                if (x < 7) {
                    blackPawnCaptures.add(i + 9);
                    pawnAttackBitboard[i][1] |= 1L << (i + 9);
                }
            }
            if (y > 0) {
                if (x > 0) {
                    whitePawnCaptures.add(i - 9);
                    pawnAttackBitboard[i][0] |= 1L << (i - 9);
                }
                if (x < 7) {
                    blackPawnCaptures.add(i + 7);
                    pawnAttackBitboard[i][1] |= 1L << (i + 7);
                }
            }
            whitePawnAttacks[i] = whitePawnCaptures.stream().mapToInt(Integer::intValue).toArray();
            blackPawnAttacks[i] = blackPawnCaptures.stream().mapToInt(Integer::intValue).toArray();

            // compute rook moves
            for (int j = 0; j < 4; j++) {
                int squareChange = squareChangeOffset[i];
                for (int g = 0; g < distFromEdge[i][j]; g++) {
                    int newSquare = i + squareChange * (g + 1);
                    rookMoves[i] |= 1L << newSquare;
                }
            }

            // compute bishop moves
            for (int j = 4; j < 8; j++) {
                int squareChange = squareChangeOffset[i];
                for (int g = 0; g < distFromEdge[i][j]; g++) {
                    int newSquare = i + squareChange * (g + 1);
                    bishopMoves[i] |= 1L << newSquare;
                }
            }

            // compute queen moves
            queenMoves[i] = rookMoves[i] | bishopMoves[i];
        }

        dirLookup = new int[127];
        for (int i = 0; i < 127; i++) {
            int offset = i - 63;
            int absOffset = abs(offset);
            int absDir = 1;
            if (absOffset % 9 == 0) {
                absDir = 9;
            } else if (absOffset % 8 == 0) {
                absDir = 8;
            } else if (absOffset % 7 == 0) {
                absDir = 7;
            }
            dirLookup[i] = (int) (absDir * signum(offset));
        }

        // Distance lookup
        orthogonalDist = new int[64][64];
        kingDist = new int[64][64];
        centreManhattanDist = new int[64];
        for (int fromSquare = 0; fromSquare < 64; fromSquare++) {
            int fromX = posToX(fromSquare);
            int fromY = posToY(fromSquare);
            int xDistFromCentre = max(3 - fromX, fromX - 4);
            int yDistFromCentre = max(3 - fromY, fromY - 4);
            centreManhattanDist[fromSquare] = xDistFromCentre + yDistFromCentre;

            for (int toSquare = 0; toSquare < 64; toSquare++) {
                int toX = posToX(toSquare);
                int toY = posToY(toSquare);
                int xDist = abs(fromX - toX);
                int yDist = abs(fromY - toY);
                orthogonalDist[fromSquare][toSquare] = xDist + yDist;
                kingDist[fromSquare][toSquare] = max(xDist, yDist);
            }
        }

        // pathMaks represents bitmasks indicating squares aligned in a straight line on a chessboard
        pathMask = new long[64][64];
        for (int fromSquare = 0; fromSquare < 64; fromSquare++) {
            for (int toSquare = 0; toSquare < 64; toSquare++) {
                int deltaX = posToX(toSquare) - posToX(fromSquare);
                int deltaY = posToY(toSquare) - posToY(fromSquare);
                int dirX = (int)signum(deltaX);
                int dirY = (int)signum(deltaY);

                for (int i = -8; i < 8; i++) {
                    int newX = fromSquare + dirX * i;
                    int newY = fromSquare + dirY * i;

                    if (BoardHelper.areCoorsValid(newX, newY)) {
                        pathMask[fromSquare][toSquare] |= 1L << (coorsToPos(newX, newY));
                    }
                }
            }
        }

        // rayPathMask represents the squares that are part of a straight line in a specific direction.
        // It covers the entire line, including both occupied and unoccupied squares.
        rayPathMask = new long[8][64];
        for (int offsetIndex = 0; offsetIndex < squareChangeOffset2D.length; offsetIndex++) {
            for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
                int x = posToX(squareIndex);
                int y = posToY(squareIndex);

                for (int i = 0; i < 8; i++) {
                    int newX = x + squareChangeOffset2D[offsetIndex].first * i;
                    int newY = y + squareChangeOffset2D[offsetIndex].second * i;
                    if (areCoorsValid(newX, newY)) {
                        rayPathMask[offsetIndex][squareIndex] |= 1L << (coorsToPos(newX, newY));
                    } else {
                        break;
                    }
                }
            }
        }
    }
}