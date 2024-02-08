package com.chessengine.chessengineservice.Helpers;

import com.chessengine.chessengineservice.Board;
import com.chessengine.chessengineservice.Move;

import static com.chessengine.chessengineservice.Piece.*;
import static java.lang.Math.abs;

public class FenHelper {

    public FenHelper() {

    }

    public String ConvertBoardToFenCode(Board board) {
        StringBuilder code = new StringBuilder();
        int emptySquare = 0;
        for (int i = 0; i < board.square.length; i++) {
            if (i%8 == 0 && i != 0) {
                if (emptySquare > 0) {
                    code.append(emptySquare);
                    emptySquare = 0;
                }
                code.append("/");
            }
            if (board.square[i] == 0) {
                emptySquare++;
            } else {
                if (emptySquare > 0) {
                    code.append(emptySquare);
                    emptySquare = 0;
                }
                code.append(intToFen(board.square[i]));
            }
        }

        //side to move
        if (board.getHalfMoveCount() % 2 == 0) {
            code.append(" w ");
        } else {
            code.append(" b ");
        }

        //castling rights
        boolean wqCastlingRight = board.isQueensideCastlingEnabled(1);
        boolean wkCastlingRight = board.isKingsideCastlingEnabled(1);
        boolean bqCastlingRight = board.isQueensideCastlingEnabled(-1);
        boolean bkCastlingRight = board.isKingsideCastlingEnabled(-1);
        if (!wqCastlingRight && !wkCastlingRight && !bqCastlingRight && !bkCastlingRight) {
            code.append("-");
        } else {
            if (wkCastlingRight) {
                code.append("K");
            }
            if (wqCastlingRight) {
                code.append("Q");
            }
            if (bkCastlingRight) {
                code.append("k");
            }
            if (bqCastlingRight) {
                code.append("q");
            }
        }

        //possible enPassant move
        Move lastMove = board.getLastMove();
        if (lastMove.startSquare != -1 &&
                abs(board.square[lastMove.targetSquare]) == PAWN
                && abs(lastMove.targetSquare - lastMove.startSquare) == 16) {
            int colour = board.square[lastMove.targetSquare];
            int enPassantSquare = lastMove.startSquare - 8*colour;
            code.append(convertIntToSquareCode(enPassantSquare));
        } else {
            code.append(" -");
        }

        code.append(" ").append(board.getHalfMoveCount());
        code.append(" ").append(board.getFullMoveCount());
        //TODO: finish fen code - missing half moves clock and full moves clock
        return code.toString();
    }

    private String intToFen(int piece) {
        if (piece > 0) {
            if (piece == PAWN) {
                return "P";
            }
            if (piece == KNIGHT) {
                return "N";
            }
            if (piece == BISHOP) {
                return "B";
            }
            if (piece == ROOK) {
                return "R";
            }
            if (piece == QUEEN) {
                return "Q";
            }
            if (piece == KING) {
                return "K";
            }
        } else {
            if (piece == -PAWN) {
                return "p";
            }
            if (piece == -KNIGHT) {
                return "n";
            }
            if (piece == -BISHOP) {
                return "b";
            }
            if (piece == -ROOK) {
                return "r";
            }
            if (piece == -QUEEN) {
                return "q";
            }
            if (piece == -KING) {
                return "k";
            }
        }
        return "";
    }

    public Board convertFenToBoard(String fenCode) { //TODO: finish this method as it's not needed for now.
        Board board = new Board();
        String[] fenCodeArray = fenCode.split(" ");
        String boardCode = fenCodeArray[0];
        int colourToMove = fenCodeArray[1].equals("w") ? 1 : -1;
        String castling = fenCodeArray[2];
        int index = 0;
        for (int i = 0; i < boardCode.length(); i++) {
            char character = fenCode.charAt(i);
            if (character == '/') {
                i--;
                continue;
            }
            if (character > 48 && character < 57) {
                int emptySquares = character - 48;
                while(emptySquares > 0) {
                    board.square[index] = 0;
                    index++;
                    emptySquares--;
                }
                continue;
            }
            board.square[index] = convertCharToPiece(character);
            index++;
        }
        return board;
    }

    private String convertIntToSquareCode(int index) {
        String row = Integer.toString(8 - index / 8);
        char column = (char)((index % 8) + 97);
        return " " + column + row;
    }

    private int convertCharToPiece(char character) {
        if (character == 'p') { return PAWN; }
        if (character == 'n') { return KNIGHT; }
        if (character == 'b') { return BISHOP; }
        if (character == 'r') { return ROOK; }
        if (character == 'q') { return QUEEN; }
        if (character == 'k') { return KING; }
        if (character == 'P') { return -PAWN; }
        if (character == 'N') { return -KNIGHT; }
        if (character == 'B') { return -BISHOP; }
        if (character == 'R') { return -ROOK; }
        if (character == 'Q') { return -QUEEN; }
        if (character == 'K') { return -KING; }
        return 0;
    }
}
