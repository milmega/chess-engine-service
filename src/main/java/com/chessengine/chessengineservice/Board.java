package com.chessengine.chessengineservice;

import java.util.List;
import java.util.Stack;

import static com.chessengine.chessengineservice.Piece.*;
import static java.lang.Math.abs;

public class Board {
    private final int GAME_START = 0;
    private final int GAME_MIDDLE = 1;
    private final int GAME_END = 2;
    private final Stack<MoveDetails> moveDetailsStack;
    private final MoveGenerator moveGenerator;
    private final Bitboard bitboard;
    private int[] numberOfPieces;
    public int[] square;
    private int whiteKingPosition;
    private int blackKingPosition;
    private boolean[] whiteCastling;
    private boolean[] blackCastling;
    private Move lastMove;
    private int captures;
    private int gameStage;
    private int moveCount;


    public Board() {
        moveGenerator = new MoveGenerator();
        moveDetailsStack = new Stack<>();
        bitboard = new Bitboard();
        resetBoard();
    }
    /*
    * {
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
        whiteKingPosition = 60;
        blackKingPosition = 4;
        whiteCastling = new boolean[] {false, false, false};
        blackCastling = new boolean[] {false, false, false};
        lastMove = new Move(-1, -1);
        captures = 0;
        moveCount = 0;
        gameStage = GAME_START;
        numberOfPieces = new int[] {7, 8, 7, 8}; // white not pawns, white pawns, black not pawns, black pawns
    }

    // unmakeMove determines if the move will be reversed later. Saves necessary details to restore them later.
    public void makeMove(Move move, boolean unmakeMove) {
        int start = move.currentSquare;
        int target = move.targetSquare;
        int piece = square[start];
        int targetPiece = square[target];
        int deltaY = abs(target%8 - start%8); // number of squares moved in Y coordinates
        MoveDetails md = createMoveDetails(move);
        Pair<Integer, Integer> rookMove = new Pair<>(-1, -1);

        if (piece == KING) { // white castling
            setKingPosition(1, target);
            setCastling(1, new boolean[] {true, true, true});
            md.castlingFlag = true;
            if (deltaY > 1) { // if king is doing castling, move rook accordingly
                if (target == 58) {
                    rookMove = new Pair<>(56, 59);
                    movePiece(56, 59, 0);
                } else {
                    rookMove = new Pair<>(63, 61);
                    movePiece(63, 61, 0);
                }
            }
        } else if (piece == -KING) { // black castling
            setKingPosition(-1, target);
            setCastling(-1, new boolean[] {true, true, true});
            md.castlingFlag = true;
            if (deltaY > 1) { // if king is doing castling, move rook accordingly
                if (target == 2) {
                    rookMove = new Pair<>(0, 3);
                    movePiece(0, 3, 0);
                } else {
                    rookMove = new Pair<>(7, 5);
                    movePiece(7, 5, 0);
                }
            }
        }
        // if a rook is moved, then disable castling
        disableCastlingIfRookInvolved(start);
        // if a rook is captured then disable castling
        disableCastlingIfRookInvolved(target);

        movePiece(start, target, 0);
        if (md.promotionFlag) {
            square[target] = QUEEN*md.colour;
            bitboard.clearSquare(PAWN*md.colour, target);
            bitboard.setSquare(QUEEN*md.colour, target);
        } else if (md.enpassantFlag) {
            square[md.enpassantPosition] = 0;
            bitboard.clearSquare(-PAWN*md.colour, md.enpassantPosition);
        }

        if (unmakeMove) {
            md.rookMove = rookMove;
            moveDetailsStack.push(md);
        } else {
            lastMove = move.getCopy();
            moveCount++;
            if (square[target] != 0) {
                captures++;
                if (captures == 3) { //TODO: is it good value?
                    gameStage = GAME_MIDDLE;
                }
                if(targetPiece > 0) {
                    numberOfPieces[targetPiece > 1 ? 0 : 1]--;
                } else {
                    numberOfPieces[targetPiece < -1 ? 2 : 3]--;
                }
                if(numberOfPieces[0] < 4 || numberOfPieces[2] < 4) {
                    gameStage = GAME_END;
                }
            }
        }
        bitboard.update();
    }

    public void unmakeMove(Move move) {
        int start = move.targetSquare;
        int target = move.currentSquare;
        MoveDetails md = moveDetailsStack.pop();
        setKingPosition(1, md.whiteKingPosition);
        setKingPosition(-1, md.blackKingPosition);
        setCastling(1, md.whiteCastling);
        setCastling(-1, md.blackCastling);
        Pair<Integer, Integer> rookMove = md.rookMove;
        if (md.castlingFlag) {
            movePiece(rookMove.second, rookMove.first, 0);
        }
        movePiece(start, target, md.targetPiece);

        if (md.promotionFlag) {
            square[target] = PAWN*md.colour;
            bitboard.clearSquare(QUEEN*md.colour, target);
            bitboard.setSquare(PAWN*md.colour, target);
        } else if (md.enpassantFlag) {
            square[md.enpassantPosition] = -PAWN*md.colour;
            bitboard.setSquare(-PAWN*md.colour, md.enpassantPosition);
        }
        bitboard.update();
    }

    //checks if king is under check
    public boolean isInCheck(int colour) {
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
        if(colour > 0) {
            whiteKingPosition = pos;
        } else {
            blackKingPosition = pos;
        }
    }

    public boolean[] getCastling(int colour) {
        return colour > 0 ? whiteCastling : blackCastling;
    }

    public void setCastling(int colour, String castling) {
        if(colour > 0) {
            whiteCastling[0] = castling.charAt(0) == '1';
            whiteCastling[1] = castling.charAt(1) == '1';
            whiteCastling[2] = castling.charAt(2) == '1';
        } else {
            blackCastling[0] = castling.charAt(0) == '1';
            blackCastling[1] = castling.charAt(1) == '1';
            blackCastling[2] = castling.charAt(2) == '1';
        }
    }

    public void setCastling(int colour, boolean[] castling) {
        if (colour > 0) {
            whiteCastling = castling.clone();
        } else {
            blackCastling = castling.clone();
        }
    }

    public Move getLastMove() {
        return lastMove;
    }

    public boolean isSameColour(int piece1, int piece2) {
        return (piece1 > 0 && piece2 > 0) || (piece1 < 0 && piece2 < 0);
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

    private MoveDetails createMoveDetails(Move move) {
        MoveDetails md = new MoveDetails();
        md.colour = move.colour;
        md.targetPiece = square[move.targetSquare];
        md.whiteKingPosition = whiteKingPosition;
        md.blackKingPosition = blackKingPosition;
        md.whiteCastling = whiteCastling;
        md.blackCastling = blackCastling;
        md.rookMove = new Pair<>(-1, -1);
        int positionDelta = abs(move.targetSquare - move.currentSquare);
        if (abs(square[move.currentSquare]) == PAWN) {
            if (move.targetSquare < 8 || move.targetSquare > 55) {
                md.promotionFlag = true;
            }
            else if(positionDelta == 7 || positionDelta == 9) { //if diagonal move
                md.enpassantFlag = true;
                md.enpassantPosition = move.currentSquare + move.changeY;
            }
        }
        return md;
    }

    // moves a piece from start to target square. TargetPiece is a piece that was on a target square (needed for unmaking a move)
    private void movePiece(int start, int target, int targetPiece) {
        int piece = square[start];
        square[target] = piece;
        square[start] = targetPiece;
        bitboard.toggleSquares(piece, start, target);
        if(targetPiece != 0) {
            bitboard.toggleSquare(targetPiece, start);
        }
    }

    public int getGameStage() {
        return gameStage;
    }

    public void setGameStage(int newGameStage) {
        gameStage = newGameStage;
    }
}
