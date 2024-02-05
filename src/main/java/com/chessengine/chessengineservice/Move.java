package com.chessengine.chessengineservice;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.posToX;
import static com.chessengine.chessengineservice.Helpers.BoardHelper.posToY;

public class Move {
    public int colour;
    public int piece;
    public int startSquare;
    public int targetSquare;
    public int targetPiece;
    public int fromX;
    public int fromY;
    public int toX;
    public int toY;
    public int changeX;
    public int changeY;
    public boolean pawnTwoSquaresMove;
    public boolean promotionFlag;
    public boolean enpassantFlag;
    public int enpassantPosition;
    public boolean castlingFlag;
    public int preCastlingPosition;
    public int postCastlingPosition;
    public int gameResult;

    public Move(int piece, int startSquare, int targetSquare, int targetPiece) {
        this.colour = piece > 0 ? 1 : -1;
        this.piece = piece;
        this.startSquare = startSquare;
        this.targetSquare = targetSquare;
        this.targetPiece = targetPiece;
        this.pawnTwoSquaresMove = false;
        this.promotionFlag = false;
        this.enpassantFlag = false;
        this.enpassantPosition = -1;
        this.castlingFlag = false;
        this.preCastlingPosition = -1;
        this.postCastlingPosition = -1;
        this.fromX = posToX(startSquare);
        this.fromY = posToY(startSquare);
        this.toX = posToX(targetSquare);
        this.toY = posToY(targetSquare);
        this.changeX = toX - fromX;
        this.changeY = toY - fromY;
        this.gameResult = 0;
    }

    @JsonIgnore
    public Move getCopy() {
        Move copy = new Move(this.piece, this.startSquare, this.targetSquare, this.targetPiece);
        copy.promotionFlag = this.promotionFlag;
        copy.enpassantFlag = this.enpassantFlag;
        copy.enpassantPosition = this.enpassantPosition;
        copy.castlingFlag = this.castlingFlag;
        copy.preCastlingPosition = this.preCastlingPosition;
        copy.postCastlingPosition = this.postCastlingPosition;
        copy.pawnTwoSquaresMove = this.pawnTwoSquaresMove;
        copy.gameResult = this.gameResult;
        return copy;
    }
}
