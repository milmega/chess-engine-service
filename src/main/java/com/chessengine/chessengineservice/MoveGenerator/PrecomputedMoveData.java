package com.chessengine.chessengineservice.MoveGenerator;

import com.chessengine.chessengineservice.Structures.Pair;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.*;
import static java.lang.Math.*;

public class PrecomputedMoveData {

    public static long[][] pathMask;
    public static long[][] dirPathMask;
    public static Pair<Integer, Integer>[] squareChangeOffset2D = new Pair[] {
            new Pair<>(-1, 0),
            new Pair<>(1, 0),
            new Pair<>(0, -1),
            new Pair<>(0, 1),
            new Pair<>(-1, -1),
            new Pair<>(1, 1),
            new Pair<>(-1, 1),
            new Pair<>(1, -1)
    };

    public static int[][] distFromEdge;

    public static void precomputeMoveData() {
        distFromEdge = new int[64][];

        for (int i = 0; i < 64; i++) {
            int x = posToX(i);
            int y = posToY(i);
            distFromEdge[i] = new int[8];
            distFromEdge[i][0] = x;
            distFromEdge[i][1] = 7 - x;
            distFromEdge[i][2] = y;
            distFromEdge[i][3] = 7 - y;
            distFromEdge[i][4] = min(x, y);
            distFromEdge[i][5] = min(7 - x, 7 - y);
            distFromEdge[i][6] = min(x, 7 - y);
            distFromEdge[i][7] = min(7 - x, y);
        }

        // pathMask represents bitmasks indicating squares aligned in a straight line on a chessboard
        pathMask = new long[64][64];
        for (int fromSquare = 0; fromSquare < 64; fromSquare++) {
            for (int toSquare = 0; toSquare < 64; toSquare++) {
                int deltaX = posToX(toSquare) - posToX(fromSquare);
                int deltaY = posToY(toSquare) - posToY(fromSquare);
                int dirX = (int)signum(deltaX);
                int dirY = (int)signum(deltaY);

                for (int i = -8; i < 8; i++) {
                    int newX = posToX(fromSquare) + dirX * i;
                    int newY = posToY(fromSquare) + dirY * i;

                    if (areCoorsValid(newX, newY)) {
                        pathMask[fromSquare][toSquare] |= 1L << 63 - (coorsToPos(newX, newY));
                    }
                }
            }
        }

        // dirPathMask represents the squares that are part of a straight line in a specific direction.
        // It covers the entire line, including both occupied and unoccupied squares.
        dirPathMask = new long[8][64];
        for (int offsetIndex = 0; offsetIndex < squareChangeOffset2D.length; offsetIndex++) {
            for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
                int x = posToX(squareIndex);
                int y = posToY(squareIndex);

                for (int i = 0; i < 8; i++) {
                    int newX = x + squareChangeOffset2D[offsetIndex].first * i;
                    int newY = y + squareChangeOffset2D[offsetIndex].second * i;
                    if (areCoorsValid(newX, newY)) {
                        dirPathMask[offsetIndex][squareIndex] |= 1L << 63 - coorsToPos(newX, newY);
                    } else {
                        break;
                    }
                }
            }
        }
    }
}