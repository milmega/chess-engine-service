package com.chessengine.chessengineservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoveGenerator {

    public final int EMPTY = 0;
    public final int PAWN = 1;
    public final int KNIGHT = 2;
    public final int BISHOP = 3;
    public final int ROOK = 4;
    public final int QUEEN = 5;
    public final int KING = 6;

    public List<Move> generateAllMoves(int colour, int[][] board) {
        List<Move> allMoves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if(!isSameColour(colour, board[i][j])) {
                    continue;
                }
                List<Move> validMoves = getValidMoves(board[i][j], i, j, GameEngine.getDeepCopy(board));
                int finalI = i;
                int finalJ = j;
                validMoves.forEach(move -> {
                    allMoves.add(new Move(finalI, finalJ, finalI + move.getX(), finalJ + move.getY())); //are these finals ok?
                });
            }
        }
        return allMoves;
    }

    //filters list of valid moves to prevent checking the king and enables moves that defend from checking
    public List<Move> getValidMoves(int piece, int x, int y, int[][] board) {
        int colour = piece > 0 ? 1 : -1;
        List<Move> moves = getMoves(piece, x, y, board);
        List<Move> validMoves = new ArrayList<>(moves.stream().filter(move -> {
            int newX = move.getX() + x;
            int newY = move.getY() + y;

            int[][] tempBoard = GameEngine.getDeepCopy(board);
            tempBoard[newX][newY] = piece;
            tempBoard[x][y] = 0;

            int[] kingPosition = GameEngine.getKingPosition(colour).clone();
            if (Math.abs(piece) == KING) {
                kingPosition[0] = newX;
                kingPosition[1] = newY;
            }
            return !isKingUnderCheck(kingPosition, tempBoard);
        }).toList());
        boolean[] castling = GameEngine.getCastling(colour);
        if (Math.abs(piece) == KING && !castling[0] && (!castling[1] || !castling[2])) {
            List<Move> castlingMoves = getCastlingMoves(colour, board);
            validMoves.addAll(castlingMoves);
        }
        return validMoves;
    }

    public List<Move> getMoves(int piece, int x, int y, int[][] board) {
        int colour = piece > 0 ? 1 : -1;
        List<Move> movesForFigure = getMovesForPiece(Math.abs(piece), colour, x, y, board); //take abs value to consider both colours
        if (Math.abs(piece) == KING || Math.abs(piece) == KNIGHT || Math.abs(piece) == PAWN) {
            return movesForFigure.stream().filter(move -> { // return valid moves
                int newX = x + move.getX();
                int newY = y + move.getY();
                if (newX < 0 || newX > 7 || newY < 0 || newY > 7) {
                    return false;
                }
                return !isSameColour(board[newX][newY], piece); //ignore if both are the same colour
            }).toList();
        }
        List<Move> allMovesForPiece = new ArrayList<>();
        movesForFigure.forEach(move -> {
            for (int j = 1; j < 8; j++) {
                int newX = x + move.getX() * j;
                int newY = y + move.getY() * j;
                if (newX < 0 || newX > 7 || newY < 0 || newY > 7) {
                    break;
                }
                int potentialSquare = board[newX][newY];
                if (potentialSquare == EMPTY) { //if the square is empty, add it to list of moves
                    allMovesForPiece.add(new Move(move.getX() * j, move.getY() * j));
                    continue;
                } else if ((potentialSquare > 0 && piece < 0) || (potentialSquare < 0 && piece > 0)) { //if opposite colours
                    allMovesForPiece.add(new Move(move.getX() * j, move.getY() * j));
                }
                break;
            }
        });
        return allMovesForPiece;
    }

    //returns list of attack moves for a figure to show a circle around potential prey or a list of all squares under attack that king cannot go to
    public List<Move> getAttackMoves(int fromX, int fromY, List<Move> possibleMoves, int[][] board) {
        int colour = board[fromX][fromY] > 0 ? 1 : -1;
        return possibleMoves.stream()
                .filter(move -> board[fromX + move.getX()][fromY + move.getY()] != 0 && !isSameColour(board[fromX + move.getX()][fromY + move.getY()], colour))
                .map(move -> new Move(fromX + move.getX(), fromY + move.getY())).toList();
    }

    public List<Move> getMovesForPiece(int piece, int colour, int x, int y, int[][] board) {
        List<Move> pieceMoves = new ArrayList<>();

        if (piece == PAWN) {
            List<Move> pawnMoves = new ArrayList<>();
            if(colour > 0) {
                if (board[x-1][y] == 0){
                    pawnMoves.add(new Move(-1, 0));
                }
                if (x == 6 && board[5][y] == 0 && board[4][y] == 0) {
                    pawnMoves.add(new Move(-2, 0));
                }
                if (x > 0 && y > 0 && board[x-1][y-1] < 0) { //value smaller than 0 it's black
                    pawnMoves.add(new Move(-1, -1));
                }
                if (x > 0 && y < 7 && board[x-1][y+1] < 0) { //value smaller than 0 it's black
                    pawnMoves.add(new Move(-1, 1));
                }
            } else {
                if (board[x+1][y] == 0){
                    pawnMoves.add(new Move(1, 0));
                }
                if (x == 1 && board[2][y] == 0 && board[3][y] == 0) {
                    pawnMoves.add(new Move(2, 0));
                }
                if (x < 7 && y > 0 && board[x+1][y-1] > 0) { //value bigger than 0 it's white
                    pawnMoves.add(new Move(1, -1));
                }
                if (x < 7 && y < 7 && board[x+1][y+1] > 0) { //value bigger than 0 it's white
                    pawnMoves.add(new Move(1, 1));
                }
            }
            pieceMoves = pawnMoves;
        }
        else if (piece == ROOK) { //ROOK
            pieceMoves = Arrays.asList(
                    new Move(1, 0),
                    new Move(0, 1),
                    new Move(0, -1),
                    new Move(-1, 0));
        }
        else if (piece == KNIGHT) { //KNIGHT
            pieceMoves = Arrays.asList(
                    new Move(2, 1),
                    new Move(2, -1),
                    new Move(-2, 1),
                    new Move(-2, -1),
                    new Move(1, 2),
                    new Move(1, -2),
                    new Move(-1, 2),
                    new Move(-1, -2));
        }
        else if (piece == BISHOP) { //BISHOP
            pieceMoves = Arrays.asList(
                    new Move(1, 1),
                    new Move(1, -1),
                    new Move(-1, 1),
                    new Move(-1, -1));
        }
        else if (piece == KING) { //KING
            pieceMoves = Arrays.asList(
                    new Move(1, 0),
                    new Move(1, 1),
                    new Move(1, -1),
                    new Move(0, 1),
                    new Move(0, -1),
                    new Move(-1, 1),
                    new Move(-1, 0),
                    new Move(-1, -1));
        }
        else if (piece == QUEEN) { //QUEEN
            pieceMoves = Arrays.asList(
                    new Move(1, 0),
                    new Move(1, 1),
                    new Move(1, -1),
                    new Move(0, 1),
                    new Move(0, -1),
                    new Move(-1, 1),
                    new Move(-1, 0),
                    new Move(-1, -1));
        }
        return pieceMoves;
    }

    //checks if king is under check
    private boolean isKingUnderCheck(int[] kingPosition, int[][] board) {
        int colour = board[kingPosition[0]][kingPosition[1]] > 0 ? 1 : -1;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == 0 || isSameColour(colour, board[i][j])) {
                    continue;
                }
                List<Move> moves = getMoves(board[i][j], i, j, board);
                List<Move> attackMoves = getAttackMoves(i, j, moves, board);
                if (attackMoves.stream().anyMatch(move -> kingPosition[0] == move.getX() && kingPosition[1] == move.getY())) {
                    return true;
                }
            }
        }
        return false;
    }

    //checks if king in checkmated
    private boolean isKingCheckmated(int colour, int[][] board) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == 0 || isSameColour(colour, board[i][j])) {
                    continue;
                }
                List<Move> validMoves = getValidMoves(board[i][j], i, j, board);
                if (!validMoves.isEmpty()) {
                    return false;
                }
            }
        }
        return true;

    }

    private List<Move> getCastlingMoves(int colour, int[][] board) {
        boolean[] castling = GameEngine.getCastling(colour);
        List<Move> castlingMoves = new ArrayList<>();
        int row = colour > 0 ? 7 : 0;
        boolean leftCastlingEnabled = true;
        boolean rightCastlingEnabled = true;

        for (int i = 0; i < 8; i++) {
            if (!leftCastlingEnabled && !rightCastlingEnabled) {
                break;
            }
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == 0 || isSameColour(colour, board[i][j])) {
                    continue;
                }
                List<Move> moves = getMoves(board[i][j], i, j, board);
                int finalI = i;
                int finalJ = j;
                leftCastlingEnabled = moves.stream().noneMatch(move -> move.getX() + finalI == row && move.getY() + finalJ < 5);
                rightCastlingEnabled = moves.stream().noneMatch(move -> move.getX() + finalI == row && move.getY() + finalJ > 3);
            }
        }
        if (!castling[1] && leftCastlingEnabled && board[row][1] == 0 && board[row][2] == 0 && board[row][3] == 0 ) { //if left rook hasn't moved
            castlingMoves.add(new Move(0, -2));
        }
        if (!castling[2] && rightCastlingEnabled && board[row][5] == 0 && board[row][6] == 0) { //if right rook hasn't moved
            castlingMoves.add(new Move(0, 2));
        }
        return castlingMoves;
    }

    private boolean isSameColour(int piece1, int piece2) {
        return (piece1 > 0 && piece2 > 0) || (piece1 < 0 && piece2 < 0);
    }
}
