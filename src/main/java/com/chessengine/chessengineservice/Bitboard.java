package com.chessengine.chessengineservice;

import static java.lang.Math.abs;

public class Bitboard {

    public long[][] pieces;
    public long emptySquares;
    public long emptyOrWhiteSquares;
    public long emptyOrBlackSquares;
    public final long lastColumn = 0x101010101010101L;
    public final long notLastColumn = ~lastColumn;
    public final long firstColumn = lastColumn << 7;
    public final long notFirstColumn = ~firstColumn;


    public Bitboard() {
        initialize();
    }

    public void initialize() {
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
        emptySquares = 0xFFFFFFFFL << 4;
        emptyOrWhiteSquares = 0xFFFFFFFFFFFFL;
        emptyOrBlackSquares = 0xFFFFFFFFFFFFL << 4;
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

    public boolean occupiesSquare(int piece, int index) {
        return ((pieces[piece > 0 ? 0 : 1][abs(piece)] >> (63 - index)) & 1) != 0;
    }

    public long getPawnAttacks(int colour) {
        if (colour > 0) {
            return ((pieces[0][1] << 9) & notLastColumn) | ((pieces[0][1] << 7) & notFirstColumn);
        }
        return ((pieces[1][1] >> 9) & notFirstColumn) | ((pieces[1][1] >> 7) & notLastColumn);
    }

    void printHexAsGrid(long hexVal) {
        String bin = String.format("%64s", Long.toBinaryString(hexVal)).replace(' ', '0');
        //String bin = Long.toBinaryString(hexVal);
        for(int i = 0; i < bin.length(); i++) {
            if(i > 0 && i % 8 == 0) {
                System.out.println();
            }
            System.out.print(bin.charAt(i) + " ");
        }
        System.out.println();
    }
}
