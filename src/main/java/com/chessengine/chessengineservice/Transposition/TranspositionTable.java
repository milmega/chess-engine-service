package com.chessengine.chessengineservice.Transposition;

import com.chessengine.chessengineservice.Board.Board;
import com.chessengine.chessengineservice.Structures.Move;

import java.math.BigInteger;

import static java.lang.Math.abs;

public class TranspositionTable {

    Board board;
    TTData[] data;
    BigInteger count;
    private int MAX_VALUE = 1000000000;
    public int exact = 0;
    public int lowerBound = 1;
    public int upperBound = 2;

    public TranspositionTable(Board board) {
        this.board = board;
    }

    public void reset() {
        int dataSize = 16;
        int sizeInBytes = 64 * 1024 * 1024;
        int tableSize = sizeInBytes / dataSize;
        count = new BigInteger(String.valueOf(tableSize));
        data = new TTData[tableSize];
    }

    public int retrieveScore(int layersToExplore, int currDepth, int alpha, int beta) {
        TTData dataRow;
        int index = new BigInteger(Long.toBinaryString(board.zobristKey), 2).mod(count).intValue();
        dataRow = data[index];

        if (dataRow != null && dataRow.zobristKey == board.zobristKey && dataRow.depth >= layersToExplore) {
            int score = verifyScore(dataRow.score, currDepth, -1);
            if (dataRow.nodeType == exact) {
                return score;
            }
            if (dataRow.nodeType == upperBound && score <= alpha) {
                return score;
            }
            if (dataRow.nodeType == lowerBound && score >= beta) {
                return score;
            }
        }
        return -1;
    }

    public void saveScore(int layersToExplore, int currDepth, int score, int scoreType) {
        int index = new BigInteger(Long.toBinaryString(board.zobristKey), 2)
                .mod(count).intValue();
        TTData dataRow = new TTData(board.zobristKey, verifyScore(score, currDepth, 1),
                (byte)layersToExplore, (byte)scoreType);
        data[index] = dataRow;
    }

    private int verifyScore(int score, int exploredPlies, int isSaving) {
        if (abs(score) > MAX_VALUE - 1000) {
            int colour = score > 0 ? 1 : -1;
            return (score * colour + isSaving * exploredPlies) * colour;
        }
        return score;
    }
}
