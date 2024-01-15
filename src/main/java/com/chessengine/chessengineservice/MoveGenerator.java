package com.chessengine.chessengineservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.chessengine.chessengineservice.Piece.*;
import static java.lang.Math.abs;

public class MoveGenerator {
    public List<Move> generateAllMoves(int colour, Board board) {
        List<Move> allMoves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if(!isSameColour(colour, board.square[i])) {
                continue;
            }
            List<Pair<Integer, Integer>> validMoves = getValidMoves(i, board);
            int finalI = i;
            validMoves.forEach(move -> {
                allMoves.add(new Move(finalI, finalI+move.first*8+move.second));
            });
        }
        return allMoves;
    }

    //filters list of valid moves to prevent checking the king and enables moves that defend from checking
    public List<Pair<Integer, Integer>> getValidMoves(int pos, Board board) {
        int piece = board.square[pos];
        int colour = piece > 0 ? 1 : -1;
        List<Pair<Integer, Integer>> moves = getMoves(pos, board);
        List<Pair<Integer, Integer>> validMoves = new ArrayList<>(moves.stream().filter(move -> {
            int newPos = pos+move.first*8+move.second;
            board.makeMove(new Move(pos, newPos), true);

            int kingPosition = board.getKingPosition(colour);
            boolean isKingUnderCheck = board.isInCheck(kingPosition, board);

            board.unmakeMove(new Move(pos, newPos));
            return !isKingUnderCheck;
        }).toList());

        boolean[] castling = board.getCastling(colour);
        if (abs(piece) == KING && !castling[0] && (!castling[1] || !castling[2])) {
            List<Pair<Integer, Integer>> castlingMoves = getCastlingMoves(colour, board);
            validMoves.addAll(castlingMoves);
        }
        return validMoves;
    }

    public List<Pair<Integer, Integer>> getMoves(int pos, Board board) {
        int piece = board.square[pos];
        int colour = piece > 0 ? 1 : -1;
        int x = pos/8;
        int y = pos%8;
        List<Pair<Integer, Integer>> movesForFigure = getMovesForPiece(colour, pos, board);
        Move lastMove = board.getLastMove();
        int lastMovePiece = lastMove.currentSquare == -1 ? 0 : board.square[lastMove.targetSquare];

        if (abs(lastMovePiece) == PAWN && abs(board.square[pos]) == PAWN) {
            Pair<Integer, Integer> enpassant = getEnpassantMove(piece, x, y, lastMove);
            if (enpassant != null) {
                movesForFigure.add(enpassant);
            }
        }

        if (abs(piece) == KING || abs(piece) == KNIGHT || abs(piece) == PAWN) {
            return movesForFigure.stream().filter(move -> { // return valid moves
                int newX = x + move.first;
                int newY = y + move.second;
                if (newX < 0 || newX > 7 || newY < 0 || newY > 7) {
                    return false;
                }
                return !isSameColour(board.square[newX*8+newY], piece); //ignore if both are the same colour
            }).toList();
        }
        List<Pair<Integer, Integer>> allMovesForPiece = new ArrayList<>();
        movesForFigure.forEach(move -> {
            for (int j = 1; j < 8; j++) {
                int newX = x + move.first * j;
                int newY = y + move.second * j;
                if (newX < 0 || newX > 7 || newY < 0 || newY > 7) {
                    break;
                }

                int potentialSquare = board.square[newX*8+newY];
                if (potentialSquare == EMPTY) { //if the square is empty, add it to list of moves
                    allMovesForPiece.add(new Pair<>(move.first * j, move.second * j));
                    continue;
                } else if ((potentialSquare > 0 && piece < 0) || (potentialSquare < 0 && piece > 0)) { //if opposite colours
                    allMovesForPiece.add(new Pair<>(move.first * j, move.second * j));
                }
                break;
            }
        });
        return allMovesForPiece;
    }

    //returns list of attack moves for a figure to show a circle around potential prey or a list of all squares under attack that king cannot go to
    public List<Move> getAttackMoves(int pos, List<Pair<Integer, Integer>> possibleMoves, Board board) {
        int colour = board.square[pos] > 0 ? 1 : -1;
        return possibleMoves.stream()
                .filter(move -> board.square[pos+move.first*8+move.second] != 0 && !isSameColour(board.square[pos+move.first*8+move.second], colour))
                .map(move -> new Move(pos, pos + move.first*8 + move.second)).toList();
    }

    public List<Pair<Integer, Integer>> getMovesForPiece(int colour, int pos, Board board) {
        List<Pair<Integer, Integer>> pieceMoves = new ArrayList<>();
        int piece = abs(board.square[pos]);
        int x = pos / 8;
        int y = pos % 8;
        if (piece == PAWN) {
            List<Pair<Integer, Integer>> pawnMoves = new ArrayList<>();
            if(colour > 0) {
                if (x > 0 && board.square[(x-1)*8+y] == 0){
                    pawnMoves.add(new Pair<>(-1, 0));
                }
                if (x == 6 && board.square[40+y] == 0 && board.square[32+y] == 0) {
                    pawnMoves.add(new Pair<>(-2, 0));
                }
                if (x > 0 && y > 0 && board.square[(x-1)*8+(y-1)] < 0) { //value smaller than 0 it's black
                    pawnMoves.add(new Pair<>(-1, -1));
                }
                if (x > 0 && y < 7 && board.square[(x-1)*8+(y+1)] < 0) { //value smaller than 0 it's black
                    pawnMoves.add(new Pair<>(-1, 1));
                }
            } else {
                if (x < 7 && board.square[(x+1)*8+y] == 0){
                    pawnMoves.add(new Pair<>(1, 0));
                }
                if (x == 1 && board.square[16+y] == 0 && board.square[24+y] == 0) {
                    pawnMoves.add(new Pair<>(2, 0));
                }
                if (x < 7 && y > 0 && board.square[(x+1)*8+(y-1)] > 0) { //value bigger than 0 it's white
                    pawnMoves.add(new Pair<>(1, -1));
                }
                if (x < 7 && y < 7 && board.square[(x+1)*8+(y+1)] > 0) { //value bigger than 0 it's white
                    pawnMoves.add(new Pair<>(1, 1));
                }
            }
            pieceMoves = pawnMoves;
        }
        else if (piece == ROOK) { //ROOK
            pieceMoves = Arrays.asList(
                    new Pair<>(1, 0),
                    new Pair<>(0, 1),
                    new Pair<>(0, -1),
                    new Pair<>(-1, 0));
        }
        else if (piece == KNIGHT) { //KNIGHT
            pieceMoves = Arrays.asList(
                    new Pair<>(2, 1),
                    new Pair<>(2, -1),
                    new Pair<>(-2, 1),
                    new Pair<>(-2, -1),
                    new Pair<>(1, 2),
                    new Pair<>(1, -2),
                    new Pair<>(-1, 2),
                    new Pair<>(-1, -2));
        }
        else if (piece == BISHOP) { //BISHOP
            pieceMoves = Arrays.asList(
                    new Pair<>(1, 1),
                    new Pair<>(1, -1),
                    new Pair<>(-1, 1),
                    new Pair<>(-1, -1));
        }
        else if (piece == KING) { //KING
            pieceMoves = Arrays.asList(
                    new Pair<>(1, 0),
                    new Pair<>(1, 1),
                    new Pair<>(1, -1),
                    new Pair<>(0, 1),
                    new Pair<>(0, -1),
                    new Pair<>(-1, 1),
                    new Pair<>(-1, 0),
                    new Pair<>(-1, -1));
        }
        else if (piece == QUEEN) { //QUEEN
            pieceMoves = Arrays.asList(
                    new Pair<>(1, 0),
                    new Pair<>(1, 1),
                    new Pair<>(1, -1),
                    new Pair<>(0, 1),
                    new Pair<>(0, -1),
                    new Pair<>(-1, 1),
                    new Pair<>(-1, 0),
                    new Pair<>(-1, -1));
        }
        return pieceMoves;
    }

    private Pair<Integer, Integer> getEnpassantMove(int piece, int x, int y, Move lastMove) {
        int lastMoveFromX = lastMove.currentSquare/8;
        int lastMoveToX = lastMove.targetSquare/8;
        int lastMoveToY = lastMove.targetSquare%8;

        if (Math.abs(lastMoveFromX - lastMoveToX) > 1 && x == lastMoveToX && Math.abs(y - lastMoveToY) == 1) {
            return new Pair<>(piece > 0 ? -1 : 1, lastMoveToY-y);
        }
        return null;
    }

    private List<Pair<Integer, Integer>> getCastlingMoves(int colour, Board board) {
        boolean[] castling = board.getCastling(colour);
        List<Pair<Integer, Integer>> castlingMoves = new ArrayList<>();

        int row = colour > 0 ? 7 : 0;
        boolean leftCastlingEnabled = true;
        boolean rightCastlingEnabled = true;

        for (int i = 0; i < 64; i++) {
            if (!leftCastlingEnabled && !rightCastlingEnabled) {
                break;
            }
            if (board.square[i] == 0 || isSameColour(colour, board.square[i])) {
                continue;
            }
            List<Pair<Integer, Integer>> moves = getMoves(i, board);
            int finalI = i/8;
            int finalJ = i%8;
            leftCastlingEnabled = moves.stream().noneMatch(move -> move.first + finalI  == row && move.second + finalJ < 5); //TODO: change the evaluation
            rightCastlingEnabled = moves.stream().noneMatch(move -> move.first + finalI == row && move.second + finalJ > 3);
        }
        if (!castling[1] && leftCastlingEnabled && board.square[row*8+1] == 0 && board.square[row*8+2] == 0 && board.square[row*8+3] == 0) { //if left rook hasn't moved
            castlingMoves.add(new Pair<>(0, -2));
        }
        if (!castling[2] && rightCastlingEnabled && board.square[row*8+5] == 0 && board.square[row*8+6] == 0) { //if right rook hasn't moved
            castlingMoves.add(new Pair<>(0, 2));
        }
        return castlingMoves;
    }

    private boolean isSameColour(int piece1, int piece2) {
        return (piece1 > 0 && piece2 > 0) || (piece1 < 0 && piece2 < 0);
    }
}
