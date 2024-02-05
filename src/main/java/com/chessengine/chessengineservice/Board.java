package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.MoveGenerator.MoveGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.*;
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
    private int[][] material;
    public int[] square;
    private int whiteKingPosition;
    private int blackKingPosition;
    private boolean[] whiteCastling;
    private boolean[] blackCastling;
    private int captures;
    private int gameStage;
    private List<Move> moveHistory;
    private int fullMoveCount;
    private int movesSinceCaptureOrPawnMove;

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
        bitboard.reset();
        gameDetailsStack.clear();
        whiteKingPosition = 60;
        blackKingPosition = 4;
        whiteCastling = new boolean[] {false, false, false};
        blackCastling = new boolean[] {false, false, false};
        captures = 0;
        moveHistory = new ArrayList<>();
        fullMoveCount = 1;
        movesSinceCaptureOrPawnMove = 0;
        gameStage = GAME_START;
        // 0 - white, 1 - black = skip | pawns | knights | bishops | rooks | queens | king
        material = new int[][] {{0, 8, 2, 2, 2, 1, 1}, {0, 8, 2, 2, 2, 1, 1}};
    }

    public List<Move> getAllMoves(int colour) {
        return moveGenerator.generateMoves(colour, this, false);
    }

    /* this method is called after each move to check if it led to a mate or a draw
     * returns: 0 if nothing happened, 1 = checkmate, 5 = stalemate
     * 2 = draw by insufficient material, 3 = draw by 3fold repetition, 4 = draw by 50 moves rule*/
    public int getGameResult(int colour) {
        List<Move> possibleMoves = getAllMoves(colour);
        if (possibleMoves.isEmpty()) {
            if (isKingInCheck()) {
                return 1; // checkmate
            }
            return 5; // stalemate
        } else {
            return getDrawResult();
        }
    }

    // unmakeMove determines if the move will be reversed later. Saves necessary details to restore them later.
    public void makeMove(Move move, boolean unmakeMove) {
        int start = move.startSquare;
        int target = move.targetSquare;
        int piece = square[start];
        int targetPiece = square[target];

        if (unmakeMove) {
            gameDetailsStack.push(new GameDetails(move, whiteCastling, blackCastling, gameStage, fullMoveCount, movesSinceCaptureOrPawnMove, captures, material));
        }

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
        if (move.colour == -1) {
            fullMoveCount++;
            movesSinceCaptureOrPawnMove++;
        }
        if (abs(piece) == PAWN) {
            movesSinceCaptureOrPawnMove = 0;
        }
        if (targetPiece != 0) {
            captures++;
            movesSinceCaptureOrPawnMove = 0;
            if (captures == 3) { //TODO: is it good value?
                gameStage = GAME_MIDDLE;
            }
            material[targetPiece > 0 ? 0 : 1][abs(targetPiece)]--;
            if (material[0][KNIGHT] + material[0][BISHOP] + material[0][ROOK] + material[0][QUEEN] < 4 ||
                    material[1][KNIGHT] + material[1][BISHOP] + material[1][ROOK] + material[1][QUEEN] < 4 ) {
                gameStage = GAME_END;
            }
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
        movesSinceCaptureOrPawnMove = gameDetails.movesSinceCaptureOrPawnMove;
        captures = gameDetails.captures;
        material = getDeepCopy(gameDetails.material);
        bitboard.updateBitboards();
    }

    // checks if it is a draw, return 0 if not a draw, otherwise returns a draw reason
    public int getDrawResult() {
        if(isDrawByInsufficientMaterial()) {
            return 2;
        }
        if(isDrawBy3foldRepetition()) {
            return 3;
        }
        if(isDrawBy50MovesRule()) {
            return 4;
        }
        return 0;
    }

    private boolean isDrawByInsufficientMaterial() {
        if((material[0][1] | material[0][4] | material[0][5] | material[1][1] | material[1][4] | material[1][5]) > 0) { // if any side has pawns, rooks or queen, it is not a draw
            return false;
        }
        if((material[0][2] | material[0][3] | material[1][2] | material[1][3]) == 0) { //if only 2 kings left, it is a draw
            return true;
        }
        if(((material[0][2] | material[0][3]) == 1 && (material[1][2] | material[1][3]) == 0) ||
                ((material[0][2] | material[0][3]) == 0 && (material[1][2] | material[1][3]) == 1)) { //if one side has one knight/bishop and the other one has only king, it's a draw
            return true;
        }
        if((material[0][2] == 1 && material[0][3] == 0 && material[1][2] == 0 && material[1][3] == 1) ||
                (material[0][2] == 0 && material[0][3] == 1 && material[1][2] == 1 && material[1][3] == 0)) { // if one side has a knight and the other one has a bishop
            return true;
        }
        if(material[0][2] == 0 && material[0][3] == 1 && material[1][2] == 0 && material[1][3] == 1) { //if both sides have a bishop - check if on the same colour
        int whiteBishop = Long.numberOfLeadingZeros(bitboard.pieces[0][BISHOP]);
        int whiteBishopRow = posToX(whiteBishop);
        int whiteBishopColumn = posToY(whiteBishop);
        int blackBishop = Long.numberOfLeadingZeros(bitboard.pieces[1][BISHOP]);
        int blackBishopRow = posToX(blackBishop);
        int blackBishopColumn = posToY(blackBishop);
        boolean whiteOnWhite = (whiteBishopRow%2 == 0 && whiteBishopColumn % 2 == 0) || (whiteBishopRow%2 == 1 && whiteBishopColumn % 2 == 1);
        boolean blackOnWhite = (blackBishopRow%2 == 0 && blackBishopColumn % 2 == 0) || (blackBishopRow%2 == 1 && blackBishopColumn % 2 == 1);
            return whiteOnWhite == blackOnWhite;
        }
        return false;
    }

    private boolean isDrawBy3foldRepetition() {
        if(moveHistory.size() < 8) {
            return false;
        }
        Move[] last8Moves = moveHistory.subList(moveHistory.size() - 8, moveHistory.size()).toArray(new Move[0]);
        return last8Moves[0].startSquare == last8Moves[4].startSquare && last8Moves[0].targetSquare == last8Moves[4].targetSquare &&
                last8Moves[1].startSquare == last8Moves[5].startSquare && last8Moves[1].targetSquare == last8Moves[5].targetSquare &&
                last8Moves[2].startSquare == last8Moves[6].startSquare && last8Moves[2].targetSquare == last8Moves[6].targetSquare &&
                last8Moves[3].startSquare == last8Moves[7].startSquare && last8Moves[3].targetSquare == last8Moves[7].targetSquare;
    }

    private boolean isDrawBy50MovesRule() {
        return movesSinceCaptureOrPawnMove >= 50;
    }

    public boolean isKingInCheck() {
        return moveGenerator.isKingInCheck();
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

    public int getMovesSinceCaptureOrPawnMove() {
        return movesSinceCaptureOrPawnMove;
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
    private void movePiece(int start, int target, int targetPiece, boolean unmake) {
        int piece = square[start];
        square[target] = piece;
        square[start] = 0;
        bitboard.toggleSquares(piece, start, target);
        if (targetPiece != 0) {
            if(unmake) {
                square[start] = targetPiece;
                bitboard.toggleSquare(targetPiece, start);
            } else {
                bitboard.toggleSquare(targetPiece, target);
            }
        }

    }
}
