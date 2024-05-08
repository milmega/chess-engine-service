package com.chessengine.chessengineservice.Helpers;

import com.chessengine.chessengineservice.Structures.Pair;

public class BitboardHelper {

    public static long shiftBits(long value, int shift) {
        if (shift > 0) {
            return value << shift;
        } else {
            return value >> -shift;
        }
    }

    public static long setBit(long board, int index) {
        return board | 0x1L << index;
    }

    public static boolean isBitSet(long board, int index) {
        return ((board >> index) & 1) != 0;
    }

    public static long clearBit(long board, int index) {
        return board & ~(0x1L << index);
    }

    // Retrieve index of least significant set bit in a 64bit value. Sets the bit to zero.
    public static Pair<Integer, Long> popLeastSignificantBit(long board) {
        int i = Long.numberOfTrailingZeros(board);
        board &= (board - 1);
        return new Pair<>(63 - i, board);
    }
}
