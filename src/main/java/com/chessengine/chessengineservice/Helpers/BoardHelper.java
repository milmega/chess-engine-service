package com.chessengine.chessengineservice.Helpers;

import com.chessengine.chessengineservice.Structures.Pair;
import com.chessengine.chessengineservice.Structures.Piece;

import java.util.Arrays;

import static java.lang.Math.abs;

public class BoardHelper {
    public static boolean isSameColour(int piece1, int piece2) {
        return (piece1 > 0 && piece2 > 0) || (piece1 < 0 && piece2 < 0);
    }

    public static int posToX(int pos) {
        return pos >> 3;
    }

    public static int posToY(int pos) {
        return pos & 0b000111;
    }

    public static int coorsToPos(int x, int y) {
        return x * 8 + y;
    }

    public static boolean areCoorsValid(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public static Pair<Integer, Integer>[] getRookMovement() {
        return new Pair[] {new Pair<>(0, -1), new Pair<>(0, 1), new Pair<>(1, 0), new Pair<>(-1, 0)};
    }

    public static Pair<Integer, Integer>[] getBishopMovement() {
        return new Pair[] {new Pair<>(1, -1), new Pair<>(1, 1), new Pair<>(-1, 1), new Pair<>(-1, -1)};
    }

    public static boolean isQueenOrBishop(int piece) {
        return abs(piece) == Piece.BISHOP || abs(piece) == Piece.QUEEN;
    }

    public static boolean isQueenOrRook(int piece) {
        return abs(piece) == Piece.ROOK || abs(piece) == Piece.QUEEN;
    }

    public static int[][] getDeepCopy(int[][] array) {
        if (array == null) {
            return null;
        }
        int[][] copy =  new int[array.length][array[0].length];

        for (int i = 0; i < array.length; i++) {
            copy[i] = Arrays.copyOf(array[i], array[0].length);
        }
        return copy;
    }
}
