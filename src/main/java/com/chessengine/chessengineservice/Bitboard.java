package com.chessengine.chessengineservice;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.*;
import static java.lang.Math.abs;

public class Bitboard {

    Pair<Integer, Integer>[] orthoMoves = new Pair[] { new Pair<>(-1, 0), new Pair<>(0, 1), new Pair<>(1, 0), new Pair<>(0, -1) };
    Pair<Integer, Integer>[] diagMoves = new Pair[] { new Pair<>(-1, -1), new Pair<>(-1, 1), new Pair<>(1, 1), new Pair<>(1, -1) };
    Pair<Integer, Integer>[] knightMoves = new Pair[] {
            new Pair<>(-2, -1),
            new Pair<>(-2, 1),
            new Pair<>(-1, 2),
            new Pair<>(1, 2),
            new Pair<>(2, 1),
            new Pair<>(2, -1),
            new Pair<>(1, -2),
            new Pair<>(-1, -2)
    };

    public long[][] pieces;
    public long allPieces;
    public static final long lastRow = 0xFFL;
    public static final long firstRow = lastRow << 56;
    public static final long lastColumn = 0x101010101010101L;
    public static final long notLastColumn = ~lastColumn;
    public static final long firstColumn = lastColumn << 7;
    public static final long notFirstColumn = ~firstColumn;
    public static final long row4 = lastRow << 32;
    public static final long row5 = lastRow << 24;
    public long[] knightAttacks;
    public long[] kingMoves;
    public long[] whitePawnAttacks;
    public long[] blackPawnAttacks;
    public long[] orthogonalSlider = {0, 0}; // map of rooks and queens
    public long[] diagonalSlider = {0, 0}; // map of bishops and queens
    MagicBitboard magicBitboard;

    public Bitboard() {
        reset();
        init();
        magicBitboard = new MagicBitboard();
    }

    public void reset() {
        pieces = new long[2][7];
        pieces[0][0] = 0xFFFFL; // all white pieces
        pieces[0][1] = 0xFF00L; // white pawns
        pieces[0][2] = 0x42L; // white knights
        pieces[0][3] = 0x24L; // white bishops
        pieces[0][4] = 0x81L; // white rooks
        pieces[0][5] = 0x10L; // white queen
        pieces[0][6] = 0x8L; // white king
        pieces[1][0] = 0xFFFFL << 48; // all black pieces
        pieces[1][1] = 0xFFL << 48; // black pawns
        pieces[1][2] = 0x42L << 56; // black knights
        pieces[1][3] = 0x24L << 56; // black bishops
        pieces[1][4] = 0x81L << 56; // black rooks
        pieces[1][5] = 0x1L << 60; // black queen
        pieces[1][6] = 0x8L << 56; // black king
        updateBitboards();
    }

    public void updateBitboards() {
        allPieces = pieces[0][0] | pieces[1][0];
        orthogonalSlider[0] = pieces[0][4] | pieces[0][5];
        orthogonalSlider[1] = pieces[1][4] | pieces[1][5];
        diagonalSlider[0] = pieces[0][3] | pieces[0][5];
        diagonalSlider[1] = pieces[1][3] | pieces[1][5];
    }

    public void init() {
        knightAttacks = new long[64];
        kingMoves = new long[64];
        whitePawnAttacks = new long[64];
        blackPawnAttacks = new long[64];

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                computeSquare(i, j);
            }
        }
    }

    private void computeSquare(int x, int y) {
        int square = coorsToPos(x, y);

        for (int dir = 0; dir < 4; dir++) {
            // pawn attacks
            if(areCoorsValid(x - 1, y + 1)) {
                whitePawnAttacks[square] |= 1L << 63 -  coorsToPos(x - 1, y + 1);
            }
            if(areCoorsValid(x - 1, y - 1)) {
                whitePawnAttacks[square] |= 1L << 63 -  coorsToPos(x - 1, y - 1);
            }
            if(areCoorsValid(x + 1, y + 1)) {
                blackPawnAttacks[square] |= 1L << 63 -  coorsToPos(x + 1, y + 1);
            }
            if(areCoorsValid(x + 1, y - 1)) {
                blackPawnAttacks[square] |= 1L << 63 -  coorsToPos(x + 1, y - 1);
            }

            // knight moves
            for (Pair<Integer, Integer> move : knightMoves) {
                int newX = x + move.first;
                int newY = y + move.second;
                if(areCoorsValid(newX, newY)) {
                    knightAttacks[square] |= 1L << 63 - coorsToPos(newX, newY);
                }
            }

            // king moves
            for (int distance = 1; distance < 8; distance++) {
                int newOrthoX = x + orthoMoves[dir].first * distance;
                int newOrthoY = y + orthoMoves[dir].second * distance;
                int newDiagX = x + diagMoves[dir].first * distance;
                int newDiagY = y + diagMoves[dir].second * distance;

                if(areCoorsValid(newOrthoX, newOrthoY)) {
                    if(distance == 1) {
                        kingMoves[square] |= 1L << 63 - coorsToPos(newOrthoX, newOrthoY);
                    }
                }
                if(areCoorsValid(newDiagX, newDiagY)) {
                    if(distance == 1) {
                        kingMoves[square] |= 1L << 63 - coorsToPos(newDiagX, newDiagY);
                    }
                }
            }
        }
    }

    public void setSquare(int piece, int index) {
        int colourIndex = piece > 0 ? 0 : 1;
        long mask = 0x1L << (63 - index);
        pieces[colourIndex][abs(piece)] |= mask;
        pieces[colourIndex][0] |= mask;
    }

    public void clearSquare(int piece, int index) {
        int colourIndex = piece > 0 ? 0 : 1;
        long mask = ~(0x1L << (63 - index));
        pieces[colourIndex][abs(piece)] &= mask;
        pieces[colourIndex][0] &= mask;
    }

    public void toggleSquare(int piece, int index) {
        int colourIndex = piece > 0 ? 0 : 1;
        long mask = 0x1L << (63 - index);
        pieces[colourIndex][abs(piece)] ^= mask;
        pieces[colourIndex][0] ^= mask;
    }

    public void toggleSquares(int piece, int indexA, int indexB) {
        int colourIndex = piece > 0 ? 0 : 1;
        long mask = 0x1L << (63 - indexA) | 0x1L << (63 - indexB);
        pieces[colourIndex][abs(piece)] ^= mask;
        pieces[colourIndex][0] ^= mask;
    }

    public long getPawnAttacks(int colour) {
        if (colour > 0) {
            return ((pieces[0][1] << 9) & notLastColumn) | ((pieces[0][1] << 7) & notFirstColumn);
        }
        return ((pieces[1][1] >> 9) & notFirstColumn) | ((pieces[1][1] >> 7) & notLastColumn);
    }


    public long getSliderAttacks(int startSquare, long blockers, boolean diagonal) {
        return magicBitboard.getSliderAttacks(63 - startSquare, blockers, diagonal);
    }
}
