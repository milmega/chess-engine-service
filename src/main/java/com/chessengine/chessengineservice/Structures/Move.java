package com.chessengine.chessengineservice.Structures;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.posToX;
import static com.chessengine.chessengineservice.Helpers.BoardHelper.posToY;

public class Move {
    public int id;
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
    public boolean enPassantFlag;
    public int enPassantPosition;
    public boolean castlingFlag;
    public int preCastlingPosition;
    public int postCastlingPosition;
    public int gameResult;
    public int score;
    static int newId = 0;

    public Move(int piece, int startSquare, int targetSquare, int targetPiece) {
        this.id = newId++;
        this.colour = piece > 0 ? 1 : -1;
        this.piece = piece;
        this.startSquare = startSquare;
        this.targetSquare = targetSquare;
        this.targetPiece = targetPiece;
        this.pawnTwoSquaresMove = false;
        this.promotionFlag = false;
        this.enPassantFlag = false;
        this.enPassantPosition = -1;
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
        this.score = 0;
    }

    @JsonIgnore
    public Move getCopy() {
        Move copy = new Move(this.piece, this.startSquare, this.targetSquare, this.targetPiece);
        copy.promotionFlag = this.promotionFlag;
        copy.enPassantFlag = this.enPassantFlag;
        copy.enPassantPosition = this.enPassantPosition;
        copy.castlingFlag = this.castlingFlag;
        copy.preCastlingPosition = this.preCastlingPosition;
        copy.postCastlingPosition = this.postCastlingPosition;
        copy.pawnTwoSquaresMove = this.pawnTwoSquaresMove;
        copy.gameResult = this.gameResult;
        copy.score = this.score;
        return copy;
    }
}
