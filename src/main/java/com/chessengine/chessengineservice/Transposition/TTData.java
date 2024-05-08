package com.chessengine.chessengineservice.Transposition;

public class TTData {
    public final long zobristKey;
    public final int score;
    public final byte depth;
    public final byte nodeType;

    public TTData(long zobristKey, int score, byte depth, byte estimation) {
        this.zobristKey = zobristKey;
        this.score = score;
        this.depth = depth;
        this.nodeType = estimation;
    }
}
