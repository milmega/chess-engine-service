package com.chessengine.chessengineservice;

public class Bitboard {

    public long[] whitePieces;
    public long[] blackPieces;

    public Bitboard() {
        whitePieces = new long[7];
        blackPieces = new long[7];
        whitePieces[1] = 0xFF00L; // white pawns
        whitePieces[2] = 0x42L; // white knights
        whitePieces[3] = 0x24L; // white bishops
        whitePieces[4] = 0x81L; // white rooks
        whitePieces[5] = 0x10L; // white queen
        whitePieces[6] = 0x8L; // white king
        blackPieces[1] = 0xFFL << 12; // black pawns
        blackPieces[2] = 0x42L << 14; // black knights
        blackPieces[3] = 0x24L << 14; // black bishops
        blackPieces[4] = 0x81L << 14; // black rooks
        blackPieces[5] = 0x1L << 15; // black queen
        blackPieces[6] = 0x8L << 14; // black king
    }

    public void makeMove(int piece, int start, int destination) {
        placeValueAt(piece, start, 0);
        placeValueAt(piece, destination, 1);
    }

    private void placeValueAt(int piece, int index, int value) {
        long mask = 1L << (63 - index);
        if(value == 0) {
            if(piece > 0) {
                whitePieces[piece] &= ~mask;
            } else {
                blackPieces[-piece] &= ~mask;
            }
        } else { // if value is 1
            if(piece > 0) {
                whitePieces[piece] |= ~mask;
            } else {
                blackPieces[-piece] |= ~mask;
            }
        }
    }
}
