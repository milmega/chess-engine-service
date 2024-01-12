package com.chessengine.chessengineservice;

import java.util.List;
import java.util.Stack;

import static com.chessengine.chessengineservice.Piece.*;
import static java.lang.Math.abs;

public class Board {
    public int[] square;
    private int whiteKingPosition;
    private int blackKingPosition;
    private boolean[] whiteCastling;
    private boolean[] blackCastling;
    private final Stack<MoveDetails> moveDetailsStack;

    private final MoveGenerator moveGenerator;

    public Board() {
        moveGenerator = new MoveGenerator();
        moveDetailsStack = new Stack<>();
        whiteKingPosition = 60;
        blackKingPosition = 6;
        whiteCastling = new boolean[] {false, false, false};
        blackCastling = new boolean[] {true, false, false};
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

    // unmakeMove determines if the move will be reversed later. Saves necessary details to restore them later.
    public void makeMove(Move move, boolean unmakeMove) {
        MoveDetails md = new MoveDetails(square[move.targetSquare], getKingPosition(1), getKingPosition(-1), getCastling(1), getCastling(-1));
        Pair<Integer, Integer> rookMove = new Pair<>(-1, -1);

        if (square[move.currentSquare] == KING) { // white castling
            setKingPosition(1, move.targetSquare);
            setCastling(1, new boolean[] {true, true, true});
            if (abs(move.targetSquare%8 - move.currentSquare%8) > 1) { // if king is doing castling, move rook accordingly
                if (move.targetSquare == 58) {
                    rookMove = new Pair<>(56, 59);
                    movePiece(56, 59, 0);
                } else {
                    rookMove = new Pair<>(63, 61);
                    movePiece(63, 61, 0);
                }
            }
        } else if (square[move.currentSquare] == -KING) { // black castling
            setKingPosition(-1, move.targetSquare);
            setCastling(-1, new boolean[] {true, true, true});
            if (abs(move.targetSquare%8 - move.currentSquare%8) > 1) { // if king is doing castling, move rook accordingly
                if (move.targetSquare == 2) {
                    rookMove = new Pair<>(0, 3);
                    movePiece(0, 3, 0);
                } else {
                    rookMove = new Pair<>(7, 5);
                    movePiece(7, 5, 0);
                }
            }
        }
        // if a rook is moved, then disable castling
        if (abs(square[move.currentSquare]) == ROOK) {
            if (square[move.currentSquare] > 0 && (move.currentSquare == 56 || move.currentSquare == 63)) {
                boolean[] whiteCastling = getCastling(1);
                whiteCastling[move.currentSquare == 56 ? 1 : 2] = true;
            } else if (square[move.currentSquare] < 0 && (move.currentSquare == 0 || move.currentSquare == 7)) {
                boolean[] blackCastling = getCastling(-1);
                blackCastling[move.currentSquare == 0 ? 1 : 2] = true;
            }
        }
        // if a rook is captured then disable castling
        if (abs(square[move.targetSquare]) == ROOK) {
            if (square[move.targetSquare] > 0 && (move.targetSquare == 56 || move.targetSquare == 63)) {
                boolean[] whiteCastling = getCastling(1);
                whiteCastling[move.targetSquare == 56 ? 1 : 2] = true;
            } else if (square[move.targetSquare] < 0 && (move.targetSquare == 0 || move.targetSquare == 7)) {
                boolean[] blackCastling = getCastling(-1);
                blackCastling[move.targetSquare == 0 ? 1 : 2] = true;
            }
        }
        if (unmakeMove) {
            md.setRookMove(rookMove);
            moveDetailsStack.push(md);
        }

        int start = move.currentSquare;
        int target = move.targetSquare;
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
    public boolean isInCheck(int kingPosition, Board board) {
        int colour = board.square[kingPosition] > 0 ? 1 : -1;
        for (int i = 0; i < 64; i++) {
            if (board.square[i] == 0 || isSameColour(colour, board.square[i])) {
                continue;
            }
            List<Pair<Integer, Integer>> moves = moveGenerator.getMoves(i, board);
            List<Move> attackMoves = moveGenerator.getAttackMoves(i, moves, board);
            if (attackMoves.stream().anyMatch(move -> kingPosition == move.targetSquare)) {
                return true;
            }
        }
        return false;
    }

    //checks if king in checkmated
    public boolean isInCheckmate(int colour) {
        for (int i = 0; i < 64; i++) {
            if (square[i] == 0 || isSameColour(colour, square[i])) {
                continue;
            }
            List<Pair<Integer, Integer>> validMoves = moveGenerator.getValidMoves(i, this);
            if (!validMoves.isEmpty()) {
                return false;
            }
        }
        return true;
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

    public boolean[] getCastling(int colour) { //TODO: update castling from the board from frontend
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

    public boolean isSameColour(int piece1, int piece2) {
        return (piece1 > 0 && piece2 > 0) || (piece1 < 0 && piece2 < 0);
    }

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
}
