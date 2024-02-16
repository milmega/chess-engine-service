package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.Helpers.Zobrist;
import com.chessengine.chessengineservice.MoveGenerator.MoveGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.chessengine.chessengineservice.Helpers.BoardHelper.*;
import static com.chessengine.chessengineservice.Helpers.Zobrist.pieceToIndex;
import static com.chessengine.chessengineservice.MoveGenerator.PrecomputedMoveData.precomputeMoveData;
import static com.chessengine.chessengineservice.Piece.*;
import static java.lang.Math.abs;

public class Board {
    private final int GAME_START = 0;
    private final int GAME_MIDDLE = 1;
    private final int GAME_END = 2;
    private final Stack<GameDetails> gameDetailsStack;
    public final MoveGenerator moveGenerator;
    public final Bitboard bitboard;
    public int[] square;
    public long zobristKey;
    public TranspositionTable tTable;
    private int[][] material;
    private int colourToMove;
    private int whiteKingPosition;
    private int blackKingPosition;
    private int castlingRights;
    private int enPassantColumn;
    private int captures;
    private int gameStage;
    private List<Move> moveHistory;
    private int fullMoveCount;
    private int movesSinceCaptureOrPawnMove;

    public Board() {
        bitboard = new Bitboard();
        tTable = new TranspositionTable(this);
        moveGenerator = new MoveGenerator(this);
        gameDetailsStack = new Stack<>();
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
        tTable.reset();
        colourToMove = 1;
        whiteKingPosition = 60;
        blackKingPosition = 4;
        castlingRights = 0;
        enPassantColumn = 8;
        captures = 0;
        moveHistory = new ArrayList<>();
        fullMoveCount = 1;
        movesSinceCaptureOrPawnMove = 0;
        gameStage = GAME_START;
        // 0 - white, 1 - black = skip | pawns | knights | bishops | rooks | queens | king
        material = new int[][] {{0, 8, 2, 2, 2, 1, 1}, {0, 8, 2, 2, 2, 1, 1}};
        zobristKey = Zobrist.createZobristKey(this);
    }

    public List<Move> getAllMoves(int colour) {
        return moveGenerator.generateMoves(colour, false);
    }

    /* this method is called after each move to check if it led to a mate or a draw
     * returns: 0 if nothing happened, 1 = checkmate, 5 = stalemate
     * 2 = draw by insufficient material, 3 = draw by 3fold repetition, 4 = draw by 50 moves rule*/
    public int getGameResult(int colour) {
        List<Move> possibleMoves = getAllMoves(colour);
        if (possibleMoves.isEmpty()) {
            if (moveGenerator.isKingInCheck()) {
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
        int oldCastling = castlingRights;
        int newCastling = castlingRights;
        long newZobristKey = zobristKey;
        int colourIndex = move.colour > 0 ? 0 : 1;

        if (unmakeMove) {
            gameDetailsStack.push(new GameDetails(move, zobristKey, castlingRights, enPassantColumn, gameStage, fullMoveCount, movesSinceCaptureOrPawnMove, captures, material));
        }
        enPassantColumn = 8; // columns are from 0 to 7 so for the zobrist key index 8 implies no enPassant

        if (abs(piece) == KING) {
            setKingPosition(move.colour, target);
            newCastling = move.colour > 0 ? 0b1100 : 0b0011;
            if (move.castlingFlag) { // if king is doing castling, move rook accordingly
                movePiece(move.preCastlingPosition, move.postCastlingPosition, 0, false);
                newZobristKey ^= Zobrist.piecesArray[pieceToIndex(ROOK*move.colour)][move.preCastlingPosition];
                newZobristKey ^= Zobrist.piecesArray[pieceToIndex(ROOK*move.colour)][move.postCastlingPosition];
            }
        }

        if (move.startSquare == 56 || move.targetSquare == 56) {
            newCastling |= 0b1000;
        } else if (move.startSquare == 63 || move.targetSquare == 63) {
            newCastling |= 0b0100;
        }
        if (move.startSquare == 0 || move.targetSquare == 0) {
            newCastling |= 0b0010;
        } else if (move.startSquare == 7 || move.targetSquare == 7) {
            newCastling |= 0b0001;
        }
        castlingRights |= newCastling;

        movePiece(start, target, targetPiece, false);
        if (move.promotionFlag) {
            square[target] = QUEEN*move.colour;
            material[colourIndex][QUEEN]++;
            material[colourIndex][PAWN]--;
            bitboard.clearSquare(move.piece, target);
            bitboard.setSquare(QUEEN*move.colour, target);
        } else if (move.enPassantFlag) {
            square[move.enPassantPosition] = 0;
            material[1-colourIndex][PAWN]--;
            bitboard.clearSquare(-PAWN*move.colour, move.enPassantPosition);
            newZobristKey ^= Zobrist.piecesArray[pieceToIndex(-PAWN*move.colour)][move.enPassantPosition];
        } else if (move.pawnTwoSquaresMove) {
            newZobristKey ^= Zobrist.enPassantColumn[enPassantColumn];
            enPassantColumn = move.fromY;
            newZobristKey ^= Zobrist.enPassantColumn[enPassantColumn];
        }

        newZobristKey ^= Zobrist.sideToMove;
        newZobristKey ^= Zobrist.piecesArray[pieceToIndex(piece)][start];
        newZobristKey ^= Zobrist.piecesArray[pieceToIndex(square[target])][target];

        if (oldCastling != newCastling) {
            newZobristKey ^= Zobrist.castlingRights[oldCastling]; // remove old castling rights state
            newZobristKey ^= Zobrist.castlingRights[castlingRights]; // add new castling rights state //TODO: should i be changed?
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
            newZobristKey ^= Zobrist.piecesArray[pieceToIndex(targetPiece)][target];
            captures++;
            movesSinceCaptureOrPawnMove = 0;
            if (captures == 3) { //TODO: is it good value?
                gameStage = GAME_MIDDLE;
            }
            material[1-colourIndex][abs(targetPiece)]--;
            if (material[0][KNIGHT] + material[0][BISHOP] + material[0][ROOK] + material[0][QUEEN] < 4 ||
                    material[1][KNIGHT] + material[1][BISHOP] + material[1][ROOK] + material[1][QUEEN] < 4 ) {
                gameStage = GAME_END;
            }
        }
        colourToMove = -colourToMove;
        zobristKey = newZobristKey;
        bitboard.updateBitboards();
    }

    public void unmakeMove(Move move) {
        int start = move.targetSquare;
        int target = move.startSquare;

        if (abs(move.piece) == KING) {
            setKingPosition(move.colour, target);
        }
        GameDetails gameDetails = gameDetailsStack.pop();
        zobristKey = gameDetails.zobristKey;
        castlingRights = gameDetails.castling;
        enPassantColumn = gameDetails.enPassantColumn;
        movePiece(start, target, move.targetPiece, true);

        if (move.castlingFlag) {
            movePiece(move.postCastlingPosition, move.preCastlingPosition, 0, false);
        } else if (move.promotionFlag) {
            square[target] = PAWN*move.colour;
            bitboard.clearSquare(QUEEN*move.colour, target);
            bitboard.setSquare(move.piece, target);
        } else if (move.enPassantFlag) {
            square[move.enPassantPosition] = -PAWN*move.colour;
            bitboard.setSquare(-PAWN*move.colour, move.enPassantPosition);
        }

        moveHistory.removeLast();
        fullMoveCount = gameDetails.fullMoveCount;
        movesSinceCaptureOrPawnMove = gameDetails.movesSinceCaptureOrPawnMove;
        captures = gameDetails.captures;
        material = getDeepCopy(gameDetails.material);
        colourToMove = -colourToMove;
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

    public int getColourToMove() {
        return colourToMove;
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

    public int getCastling() {
        return castlingRights;
    }

    public boolean isQueensideCastlingEnabled(int colour) {
        int mask = colour > 0 ? 0b1000 : 0b0010;
        return (castlingRights & mask) == 0;
    }

    public boolean isKingsideCastlingEnabled(int colour) {
        int mask = colour > 0 ? 0b0100 : 0b0001;
        return (castlingRights & mask) == 0;
    }

    public int getEnPassantColumn() {
        return enPassantColumn;
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

    public int[][] getMaterial() {
        return material;
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
