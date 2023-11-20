package com.chessengine.chessengineservice;

public class GameEngine {

    private static int[][] board = {
            {-4, -2, -3, -5, -6, -3, -2, -4},
            {-1, -1, -1, -1, -1, -1, -1, -1},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {4, 2, 3, 5, 6, 3, 2, 4}
    };

    private static boolean turn = true; //TRUE = WHITE, FALSE = BLACK
    private static int whiteKingPositionX = 7;
    private static int whiteKingPositionY = 4;
    private static int blackKingPositionX = 0;
    private static int blackKingPositionY = 4;

    public static boolean[] whiteCastling = {false, false, false};
    public static boolean[] blackCastling = {false, false, false};

    public GameEngine() {

    }

    public void startGame() {

    }

    public void calculateNextMove() {

    }

    public static int[][] getBoard() {
        return board;
    }

    public static int[] getKingPosition(boolean colour) {
        return colour
                ? new int[] {whiteKingPositionX, whiteKingPositionY}
                : new int[] {blackKingPositionX, blackKingPositionY};
    }

    public static void setKingPosition(boolean colour, int x, int y) {
        if(colour) {
            whiteKingPositionX = x;
            whiteKingPositionY = y;
        } else {
            blackKingPositionX = x;
            blackKingPositionY = y;
        }

    }

    public static boolean[] getCastling(boolean colour) {
        return colour ? whiteCastling : blackCastling;
    }
}
