package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.MoveGenerator.MoveGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.isSameColour;
import static com.chessengine.chessengineservice.MoveGenerator.PrecomputedMoveData.precomputeMoveData;
import static com.chessengine.chessengineservice.Piece.*;
import static java.lang.Math.abs;

public class Board {
    private final int GAME_START = 0;
    private final int GAME_MIDDLE = 1;
    private final int GAME_END = 2;
    private final Stack<GameDetails> gameDetailsStack;
    private final MoveGenerator moveGenerator;
    public final Bitboard bitboard;
    private int[] numberOfPieces;
    public int[] square;
    private int whiteKingPosition;
    private int blackKingPosition;
    private boolean[] whiteCastling;
    private boolean[] blackCastling;
    private int captures;
    private int gameStage;
    private List<Move> moveHistory;
    private int fullMoveCount;
    private int plysSinceCaptureOrPawnMove;

    public Board() {
        moveGenerator = new MoveGenerator();
        gameDetailsStack = new Stack<>();
        bitboard = new Bitboard();
        precomputeMoveData();
        resetBoard();
    }
    /*
     {
                -4, -2, -3, -5, -6, -3, -2, -4,
                -1, -1, -1, -1, -1, -1, -1, -1,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                1, 1, 1, 1, 1, 1, 1, 1,
                4, 2, 3, 5, 6, 3, 2, 4}
    * */

    public void initializeBoard() {
        square = new int[] {
                -4, -2, -3, -5, -6, -3, -2, -4,
                -1, -1, -1, -1, -1, -1, -1, -1,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                1, 1, 1, 1, 1, 1, 1, 1,
                4, 2, 3, 5, 6, 3, 2, 4};
    }

    public void resetBoard() {
        initializeBoard();
        bitboard.initialize();
        moveDetailsStack.clear();
        bitboard.reset();
        gameDetailsStack.clear();
        whiteKingPosition = 60;
        blackKingPosition = 4;
        whiteCastling = new boolean[] {false, false, false};
        blackCastling = new boolean[] {false, false, false};
        captures = 0;
        moveHistory = new ArrayList<>();
        fullMoveCount = 1;
        plysSinceCaptureOrPawnMove = 0;
        gameStage = GAME_START;
        numberOfPieces = new int[] {8, 7, 8, 7}; // white pawns, white not pawns, black pawns, black not pawns
    }

    public List<Move> getAllMoves(int colour) {
        return moveGenerator.generateMoves(colour, this, false);
    }


    // unmakeMove determines if the move will be reversed later. Saves necessary details to restore them later.
    public void makeMove(Move move, boolean unmakeMove) {
        int start = move.startSquare;
        int target = move.targetSquare;
        int piece = square[start];
        int targetPiece = square[target];

        GameDetails gameDetails = new GameDetails(move, whiteCastling, blackCastling, gameStage, fullMoveCount, plysSinceCaptureOrPawnMove, captures, numberOfPieces);

        if (abs(piece) == KING) {
            setKingPosition(move.colour, target);
            setCastling(move.colour, new boolean[] {true, true, true});
            if(move.castlingFlag) { // if king is doing castling, move rook accordingly
                movePiece(move.preCastlingPosition, move.postCastlingPosition, 0, false);
            }
        }

        disableCastlingIfRookInvolved(start); // if a rook is moved, then disable castling
        disableCastlingIfRookInvolved(target); // if a rook is captured then disable castling TODO: is it even needed?

        movePiece(start, target, targetPiece, false);
        if (move.promotionFlag) {
            square[target] = QUEEN*move.colour;
            bitboard.clearSquare(move.piece, target);
            bitboard.setSquare(QUEEN*move.colour, target);
        } else if (move.enpassantFlag) {
            square[move.enpassantPosition] = 0;
            bitboard.clearSquare(-PAWN*move.colour, move.enpassantPosition);
        }

        moveHistory.add(move);
        plysSinceCaptureOrPawnMove++;
        if (move.colour == -1) {
            fullMoveCount++;
        }
        if (abs(piece) == PAWN) {
            plysSinceCaptureOrPawnMove = 0;
        }
        if (targetPiece != 0) {
            captures++;
            plysSinceCaptureOrPawnMove = 0;
            if (captures == 3) { //TODO: is it good value?
                gameStage = GAME_MIDDLE;
            }
            if (targetPiece > 0) {
                numberOfPieces[targetPiece > 1 ? 1 : 0]--;
            } else {
                numberOfPieces[targetPiece < -1 ? 3 : 2]--;
            }
            if (numberOfPieces[1] < 4 || numberOfPieces[3] < 4) {
                gameStage = GAME_END;
            }
        }

        if (unmakeMove) {
            gameDetailsStack.push(gameDetails);
        }
        bitboard.updateBitboards();
    }

    public void unmakeMove(Move move) {
        int start = move.targetSquare;
        int target = move.startSquare;

        if (abs(move.piece) == KING) {
            setKingPosition(move.colour, target);
        }
        GameDetails gameDetails = gameDetailsStack.pop();
        setCastling(1, gameDetails.whiteCastling);
        setCastling(-1, gameDetails.blackCastling);
        movePiece(start, target, move.targetPiece, true);

        if (move.castlingFlag) {
            movePiece(move.postCastlingPosition, move.preCastlingPosition, 0, false);
        } else if (move.promotionFlag) {
            square[target] = PAWN*move.colour;
            bitboard.clearSquare(QUEEN*move.colour, target);
            bitboard.setSquare(move.piece, target);
        } else if (move.enpassantFlag) {
            square[move.enpassantPosition] = -PAWN*move.colour;
            bitboard.setSquare(-PAWN*move.colour, move.enpassantPosition);
        }

        moveHistory.removeLast();
        fullMoveCount = gameDetails.fullMoveCount;
        plysSinceCaptureOrPawnMove = gameDetails.plysSinceCaptureOrPawnMove;
        captures = gameDetails.captures;
        numberOfPieces = gameDetails.numberOfPieces.clone();
        bitboard.updateBitboards();
    }

    //checks if king is under check
    public boolean isInCheck(int colour) { //TODO: to delete after checking time of old move generation
        int kingPosition = getKingPosition(colour);
        for (int i = 0; i < 64; i++) {
            if (square[i] == 0 || isSameColour(colour, square[i])) {
                continue;
            }
            List<Move> attackMoves = moveGenerator.getAttackMoves(i, this);
            if (attackMoves.stream().anyMatch(move -> kingPosition == move.targetSquare)) {
                return true;
            }
        }
        return false;
    }

    public int getKingPosition(int colour) {
        return colour > 0 ? whiteKingPosition : blackKingPosition;
    }

    public void setKingPosition(int colour, int pos) {
        if (colour > 0) {
            whiteKingPosition = pos;
        } else {
            blackKingPosition = pos;
        }
    }

    public boolean[] getCastling(int colour) {
        return colour > 0 ? whiteCastling : blackCastling;
    }

    public void setCastling(int colour, boolean[] castling) {
        if (colour > 0) {
            whiteCastling = castling.clone();
        } else {
            blackCastling = castling.clone();
        }
    }

    private void disableCastlingIfRookInvolved(int index) {
        if (abs(square[index]) == ROOK) {
            if (square[index] > 0 && (index == 56 || index == 63)) {
                boolean[] whiteCastling = getCastling(1);
                whiteCastling[index == 56 ? 1 : 2] = true;
            } else if (square[index] < 0 && (index == 0 || index == 7)) {
                boolean[] blackCastling = getCastling(-1);
                blackCastling[index == 0 ? 1 : 2] = true;
            }
        }
    }

    // moves a piece from start to target square. TargetPiece is a piece that was on a target square (needed for unmaking a move)
    private void movePiece(int start, int target, int targetPiece) {
        int piece = square[start];
        square[target] = piece;
        square[start] = 0;
        bitboard.toggleSquares(piece, start, target);
        if (targetPiece != 0) {
            bitboard.toggleSquare(targetPiece, target);
            if(unmake) {
                square[start] = targetPiece;
            }
        }
    }

    public Move getLastMove() {
        return !moveHistory.isEmpty() ? moveHistory.getLast() : new Move(0, 0, 0, 0);
    }

    public int getGameStage() {
        return gameStage;
    }

    public void setGameStage(int newGameStage) {
        gameStage = newGameStage;
    }

    public int getFullMoveCount() {
        return fullMoveCount;
    }

    public int getHalfMoveCount() {
        return moveHistory.size();
    }

    public int getPlysSinceCaptureOrPawnMove() {
        return plysSinceCaptureOrPawnMove;
    }
}
