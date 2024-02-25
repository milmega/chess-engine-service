package com.chessengine.chessengineservice.Helpers;

import com.chessengine.chessengineservice.Board;

import java.util.Random;

public class Zobrist {

    public static long[][] piecesArray = new long[15][64];
    public static long[] castlingRights = new long[16];
    public static long[] enPassantColumn = new long[9];
    public static long sideToMove;

    public static long createZobristKey(Board board) {
        initialize();
        long key = 0;
        for (int i = 0; i < 64; i++) {
            int piece = board.square[i];
            if (piece != 0) {
                key ^= piecesArray[pieceToIndex(piece)][i];
            }
        }
        key ^= enPassantColumn[board.getEnPassantColumn()];
        if (board.getColourToMove() == -1) {
            key ^= sideToMove;
        }
        key ^= castlingRights[board.getCastling()];
        return key;
    }

    public static int pieceToIndex(int piece) {
        if (piece > 0) {
            return piece;
        }
        return 8-piece; // converting piece value to index (white from 1 to 6, black from 9 to 14)
    }

    static long random64BitNumber(Random rng) {
        byte[] buffer = new byte[8];
        rng.nextBytes(buffer);
        return convertToLong(buffer);
    }

    static long convertToLong(byte[] bytes) {
        long value = 0L;
        for (byte b : bytes) {
            value = (value << 8) + (b & 255);
        }
        return value;
    }

    static void initialize() {
        int seed = 29426028;
        Random rng = new Random(seed);

        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            for (int piece = 1; piece < 7; piece++) {
                piecesArray[piece][squareIndex] = random64BitNumber(rng);
            }
            for (int piece = 9; piece < 15; piece++) {
                piecesArray[piece][squareIndex] = random64BitNumber(rng);
            }
        }
        for (int i = 0; i < castlingRights.length; i++) {
            castlingRights[i] = random64BitNumber(rng);
        }
        enPassantColumn[0] = 0;
        for (int i = 1; i < enPassantColumn.length; i++) {
            enPassantColumn[i] = random64BitNumber(rng);
        }
        sideToMove = random64BitNumber(rng);
    }
}
