package com.chessengine.chessengineservice;

public class TTData {
    public final long zobristKey;
    public final int score;
    public final Move move;
    public final byte depth;
    public final byte nodeType;

    public TTData(long zobristKey, int score, byte depth, byte estimation, Move move) {
        this.zobristKey = zobristKey;
        this.score = score;
        this.depth = depth;
        this.nodeType = estimation;
        this.move = move;
    }
}
