package com.chessengine.chessengineservice.Helpers;

import com.chessengine.chessengineservice.Board.Board;

import java.util.Random;

public class Zobrist {
    public static long colourToMove;
    public static long[][] allPieces = new long[15][64];
    public static long[] castling = new long[16];
    public static long[] enPassantColumn = new long[9];

    public static long createZobristKey(Board board) {
        initialize();
        long key = 0;
        for (int i = 0; i < 64; i++) {
            int piece = board.chessboard[i];
            if (piece != 0) {
                key ^= allPieces[pieceToIndex(piece)][i];
            }
        }
        key ^= enPassantColumn[board.getEnPassantColumn()];
        if (board.getColourToMove() == -1) {
            key ^= colourToMove;
        }
        key ^= castling[board.getCastling()];
        return key;
    }

    public static int pieceToIndex(int piece) {
        if (piece > 0) {
            return piece;
        }
        return 8-piece; // converting piece value to index (white from 1 to 6, black from 9 to 14)
    }

    static long generateRandomNumber(Random randomNumberGenerator) {
        byte[] buffer = new byte[8];
        randomNumberGenerator.nextBytes(buffer);
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
        int seed = 56709751;
        Random randomNumGenerator = new Random(seed);

        colourToMove = generateRandomNumber(randomNumGenerator);
        for (int i = 0; i < 64; i++) {
            for (int j = 1; j < 7; j++) {
                allPieces[j][i] = generateRandomNumber(randomNumGenerator);
            }
            for (int j = 9; j < 15; j++) {
                allPieces[j][i] = generateRandomNumber(randomNumGenerator);
            }
        }
        enPassantColumn[0] = 0;
        for (int i = 1; i < enPassantColumn.length; i++) {
            enPassantColumn[i] = generateRandomNumber(randomNumGenerator);
        }
        for (int i = 0; i < castling.length; i++) {
            castling[i] = generateRandomNumber(randomNumGenerator);
        }
    }
}
