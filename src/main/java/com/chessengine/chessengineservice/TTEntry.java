package com.chessengine.chessengineservice;

public class TTEntry {
    public final long key;
    public final int score;
    public final Move move;
    public final byte depth;
    public final byte nodeType;

    public TTEntry(long key, int score, byte depth, byte nodeType, Move move) {
        this.key = key;
        this.score = score;
        this.depth = depth;
        this.nodeType = nodeType;
        this.move = move;
    }
}
