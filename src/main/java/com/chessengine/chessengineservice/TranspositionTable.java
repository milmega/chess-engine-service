package com.chessengine.chessengineservice;

import java.math.BigInteger;
import java.util.HashMap;

import static java.lang.Math.abs;

public class TranspositionTable {

    Board board;
    TTEntry[] entries;
    BigInteger count;
    private int MAX_VALUE = 1000000000;
    public int exact = 0;
    public int lowerBound = 1;
    public int upperBound = 2;
    private boolean disabled = false;

    public TranspositionTable(Board board) {
        this.board = board;
    }

    public void reset() {
        int ttEntrySizeBytes = 16;
        int sizeInBytes = 64 * 1024 * 1024;
        int numEntries = sizeInBytes / ttEntrySizeBytes;
        count = new BigInteger(String.valueOf(numEntries));
        entries = new TTEntry[numEntries];
    }

    public int lookupEvaluation(int depth, int plyFromRoot, int alpha, int beta) {
        TTEntry entry;
        int index = new BigInteger(Long.toBinaryString(board.zobristKey), 2).mod(count).intValue();
        entry = entries[index];

        if (entry != null && entry.key == board.zobristKey && entry.depth >= depth) {
            int correctedScore = correctRetrievedMateScore(entry.score, plyFromRoot);
            if (entry.nodeType == exact) {
                return correctedScore;
            }
            if (entry.nodeType == upperBound && correctedScore <= alpha) {
                return correctedScore;
            }
            if (entry.nodeType == lowerBound && correctedScore >= beta) {
                return correctedScore;
            }
        }
        return -1;
    }

    public void storeEvaluation(int depth, int numPlySearched, int eval, int evalType, Move move) {
        int index = new BigInteger(Long.toBinaryString(board.zobristKey), 2).mod(count).intValue();
        TTEntry entry = new TTEntry(board.zobristKey, correctMateScoreForStorage(eval, numPlySearched), (byte)depth, (byte)evalType, move);
        entries[index] = entry;
    }

    private int correctMateScoreForStorage(int score, int numPlySearched) {
        if (abs(score) > MAX_VALUE - 1000) {
            int sign = score > 0 ? 1 : -1;
            return (score * sign + numPlySearched) * sign;
        }
        return score;
    }

    private int correctRetrievedMateScore(int score, int numPlySearched) {
        if (abs(score) > MAX_VALUE - 1000) {
            int sign = score > 0 ? 1 : -1;
            return (score * sign - numPlySearched) * sign;
        }
        return score;
    }
}
