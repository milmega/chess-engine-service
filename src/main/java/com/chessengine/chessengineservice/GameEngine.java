package com.chessengine.chessengineservice;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GameEngine {
    private static boolean turn = true; //TRUE = WHITE, FALSE = BLACK
    private static int whiteKingPositionX = 7;
    private static int whiteKingPositionY = 4;
    private static int blackKingPositionX = 0;
    private static int blackKingPositionY = 4;

    public static boolean[] whiteCastling = {false, false, false};
    public static boolean[] blackCastling = {false, false, false};

    MoveGenerator moveGenerator;
    Evaluator evaluator;

    public GameEngine() {
        moveGenerator = new MoveGenerator();
        evaluator = new Evaluator();
    }

    public static void startGame() {

    }

    public Move calculateNextMove(int colour, int[][] board) {
        //Move bestMove = evaluator.getBestMove(colour, board, 5); //TODO: write some evaluation functions to determine best move
        Move bestMove = evaluator.getBestMove(colour, board);
        return bestMove;

    }

    public static int[] getKingPosition(int colour) {
        return colour > 0
                ? new int[] {whiteKingPositionX, whiteKingPositionY}
                : new int[] {blackKingPositionX, blackKingPositionY};
    }

    public static void setKingPosition(int colour, int x, int y) {
        if(colour > 0) {
            whiteKingPositionX = x;
            whiteKingPositionY = y;
        } else {
            blackKingPositionX = x;
            blackKingPositionY = y;
        }

    }

    public static boolean[] getCastling(int colour) { //TODO: update castling from the board from frontend
        return colour > 0 ? whiteCastling : blackCastling;
    }

    public static void setCastling(int colour, String castling) {
        if(colour > 0) {
            whiteCastling[0] = castling.charAt(0) == '1';
            whiteCastling[1] = castling.charAt(1) == '1';
            whiteCastling[2] = castling.charAt(2) == '1';
        } else {
            blackCastling[0] = castling.charAt(0) == '1';
            blackCastling[1] = castling.charAt(1) == '1';
            blackCastling[2] = castling.charAt(2) == '1';
        }
    }

    public static int[][] getDeepCopy(int[][] array) {
        int[][] resultArray = new int[array.length][array[0].length];
        for(int i = 0; i < array.length; i++) {
            int[] resultRow = array[i].clone();
            resultArray[i] = resultRow;
        }
        return resultArray;
    }
}
