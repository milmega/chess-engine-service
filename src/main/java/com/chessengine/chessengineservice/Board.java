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
        whiteKingPosition = 60;
        blackKingPosition = 4;
        whiteCastling = new boolean[] {false, false, false};
        blackCastling = new boolean[] {false, false, false};
        lastMove = new Move(-1, -1);
        captures = 0;
        moveCount = 0;
        gameStage = GAME_START;
        numberOfPieces = new int[] {7, 8, 7, 8}; // white not pawns, white pawns, black not pawns, black pawns
        initializeBoard();
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
        MoveDetails md = new MoveDetails(square[target], getKingPosition(1), getKingPosition(-1), getCastling(1), getCastling(-1));
        Pair<Integer, Integer> rookMove = new Pair<>(-1, -1);

        if (square[start] == KING) { // white castling
            setKingPosition(1, target);
            setCastling(1, new boolean[] {true, true, true});
            if (abs(target%8 - start%8) > 1) { // if king is doing castling, move rook accordingly
                if (target == 58) {
                    rookMove = new Pair<>(56, 59);
                    movePiece(56, 59, 0);
                } else {
                    rookMove = new Pair<>(63, 61);
                    movePiece(63, 61, 0);
                }
            }
        } else if (square[start] == -KING) { // black castling
            setKingPosition(-1, target);
            setCastling(-1, new boolean[] {true, true, true});
            if (abs(target%8 - start%8) > 1) { // if king is doing castling, move rook accordingly
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

        if (unmakeMove) {
            md.setRookMove(rookMove);
            moveDetailsStack.push(md);
        } else {
            lastMove = move.getCopy();
            moveCount++;
            if (square[target] != 0) {
                captures++;
                if (captures == 3) { //TODO: is it good value?
                    gameStage = GAME_MIDDLE;
                }
                if(square[target] > 0) {
                    numberOfPieces[square[target] > 1 ? 0 : 1]--;
                } else {
                    numberOfPieces[square[target] < -1 ? 2 : 3]--;
                }
                if(numberOfPieces[0] < 4 || numberOfPieces[2] < 4) {
                    gameStage = GAME_END;
                }
            }
        }
        movePiece(start, target, 0);
    }

    public void unmakeMove(Move move) {
        int start = move.targetSquare;
        int target = move.currentSquare;
        MoveDetails moveDetails = moveDetailsStack.pop();
        movePiece(start, target, moveDetails.getTargetPiece());
        setKingPosition(1, moveDetails.getWhiteKingPosition());
        setKingPosition(-1, moveDetails.getBlackKingPosition());
        setCastling(1, moveDetails.getWhiteCastling());
        setCastling(-1, moveDetails.getBlackCastling());
        Pair<Integer, Integer> rookMove = moveDetails.getRookMove();
        if (rookMove.first != -1) {
            movePiece(rookMove.second, rookMove.first, 0);
        }
    }

    public void parseBoard(String boardCode) {
        int index = 0;
        for (int i = 0; i < boardCode.length(); i++) {
            String character = boardCode.substring(i, i+1);
            if(character.equals("-")) {
                i++;
                character = character.concat(boardCode.substring(i, i+1));
            }
            square[index] = Integer.parseInt(character);
            index++;
        }
    }

    //checks if king is under check
    public boolean isInCheck(int colour) {
        int kingPosition = getKingPosition(colour);
        for (int i = 0; i < 64; i++) {
            if (square[i] == 0 || isSameColour(colour, square[i])) {
                continue;
            }
            List<Pair<Integer, Integer>> moves = moveGenerator.getMoves(i, this);
            List<Move> attackMoves = moveGenerator.getAttackMoves(i, moves, this);
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

    // moves a piece from start to target square. TargetPiece is a piece that was on a target square (needed for unmaking a move)
    private void movePiece(int start, int target, int targetPiece) {
        int piece = square[start];
        int colour = piece > 0 ? 1 : -1;
        if (abs(piece) == PAWN && (target < 8 || target > 55)) {
            square[target] = QUEEN*colour;
        } else {
            square[target] = piece;
        }
        square[start] = targetPiece;
    }

    public int getGameStage() {
        return gameStage;
    }

    public void setGameStage(int newGameStage) {
        gameStage = newGameStage;
    }
}
