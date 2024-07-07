package com.chessengine.chessengineservice.MoveGenerator;

import com.chessengine.chessengineservice.Board.Bitboard;
import com.chessengine.chessengineservice.Board.Board;
import com.chessengine.chessengineservice.Structures.Move;
import com.chessengine.chessengineservice.Structures.Pair;

import java.util.ArrayList;
import java.util.List;

import static com.chessengine.chessengineservice.Board.Bitboard.*;
import static com.chessengine.chessengineservice.Helpers.BitboardHelper.*;
import static com.chessengine.chessengineservice.Helpers.BoardHelper.*;
import static com.chessengine.chessengineservice.MoveGenerator.PrecomputedMoveData.*;
import static com.chessengine.chessengineservice.Structures.Piece.*;

public class MoveGenerator {
    final long MAX_LONG = 0xFFFFFFFFFFFFFFFFL;
    final long wKingCastlingMask = 0x6L;
    final long wQueenCastlingMask = 0x70L;
    final long wQueenCastlingMask2 = 0x30L;
    final long bKingCastlingMask = 0x6L << 56;
    final long bQueenCastlingMask = 0x7L << 60;
    final long bQueenCastlingMask2 = 0x3L << 60;
    //  N, S, W, E, NW, SE, NE, SW
    final int[] squareChangeOffset = { -8, 8, -1, 1, -9, 9, -7, 7 };
    int friendlyColour;
    int opponentColour;
    int friendlyKingPos;
    int friendlyIndex;
    int opponentIndex;

    boolean isInCheck;
    boolean isInDoubleCheck;

    long pathInCheckMask;
    long pinnedPaths;
    long notPinnedPaths;
    long oppAttackMapWithoutPawns;
    public long oppAttackMap;
    public long oppPawnAttackMap;
    long oppSlidingAttacks;
    boolean quietMoves;
    Board board;

    long opponentPieces;
    long friendlyPieces;
    long allPieces;
    long emptySquares;
    long opponentOrEmptySquares;
    long captureMask;

    Bitboard bitboard;
    List<Move> allMoves;

    public MoveGenerator(Board board) {
        this.board = board;
        this.bitboard = board.bitboard;
    }

    public boolean isKingInCheck() {
        return isInCheck;
    }

    public List<Move> computeAllMoves(int colour, boolean capturesOnly) {
        allMoves = new ArrayList<>();
        quietMoves = !capturesOnly;

        initializeBitboards(colour);
        computeMovesForKing();

        if (!isInDoubleCheck) {
            computeMovesForPawns();
            computeMovesForKnights();
            computeMovesForSliders();
        }
        return allMoves;
    }
    private void initializeBitboards(int colour) {
        friendlyColour = colour;
        opponentColour = -colour;
        friendlyKingPos = board.getKingPosition(colour);
        friendlyIndex = colour > 0 ? 0 : 1;
        opponentIndex = 1 - friendlyIndex;

        opponentPieces = bitboard.pieces[opponentIndex][0];
        friendlyPieces = bitboard.pieces[friendlyIndex][0];
        allPieces = bitboard.allPieces;
        emptySquares = ~allPieces;
        opponentOrEmptySquares = emptySquares | opponentPieces;
        captureMask = quietMoves ? MAX_LONG : opponentPieces;

        isInCheck = false;
        isInDoubleCheck = false;
        pathInCheckMask = 0;
        pinnedPaths = 0;
        allMoves = new ArrayList<>();

        computeAttackData(colour);
    }

    private void computeAttackData(int colour) {
        computeSlidingAttacks();
        int friendlyKingPos = board.getKingPosition(colour);
        int friendlyIndex = colour > 0 ? 0 : 1;
        int opponentIndex = 1 - friendlyIndex;
        int dirStart = 0;
        int dirEnd = 8;

        if (bitboard.pieces[opponentIndex][QUEEN] == 0) {
            dirStart = bitboard.pieces[opponentIndex][ROOK] > 0 ? 0 : 4;
            dirEnd = bitboard.pieces[opponentIndex][BISHOP] > 0 ? 8 : 4;
        }

        for (int i = dirStart; i < dirEnd; i++) {
            boolean diagonalMove = i > 3;
            long slidingMask = diagonalMove ? bitboard.diagonalSlider[opponentIndex] : bitboard.orthogonalSlider[opponentIndex];
            long mask = dirPathMask[i][friendlyKingPos];
            if ((mask & slidingMask) == 0) {
                continue;
            }

            int maxDist = distFromEdge[friendlyKingPos][i];
            int changeOffset = squareChangeOffset[i];
            boolean friendOnThePath = false;
            long pathMask = 0;

            for (int j = 0; j < maxDist; j++) {
                int position = friendlyKingPos + changeOffset * (j + 1);
                pathMask |= 1L << 63 - position;
                int piece = board.chessboard[position];

                if (piece != 0) {
                    if (isSameColour(piece, colour)) {
                        if (!friendOnThePath) {
                            friendOnThePath = true;
                        } else { break; }
                    } else {
                        if (diagonalMove && isQueenOrBishop(piece) || !diagonalMove && isQueenOrRook(piece)) {
                            if (friendOnThePath) {
                                pinnedPaths |= pathMask;
                            } else {
                                pathInCheckMask |= pathMask;
                                isInDoubleCheck = isInCheck;
                                isInCheck = true;
                            }
                            break;
                        }
                        else { break; }
                    }
                }
            }
            if (isInDoubleCheck) {
                break;
            }
        }

        notPinnedPaths = ~pinnedPaths;
        long oppKnightAttacks = 0;
        long knightsBB = bitboard.pieces[opponentIndex][KNIGHT];
        long kingBB = bitboard.pieces[friendlyIndex][KING];

        while (knightsBB != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(knightsBB);
            int position = plsbResult.first;
            knightsBB = plsbResult.second;
            long knightAttacks = bitboard.knightAttacks[position];
            oppKnightAttacks |= knightAttacks;

            if ((knightAttacks & kingBB) != 0) {
                isInDoubleCheck = isInCheck;
                isInCheck = true;
                pathInCheckMask |= 1L << 63 - position;
            }
        }

        oppPawnAttackMap = 0;
        long oppPawnsBB = bitboard.pieces[opponentIndex][PAWN];
        oppPawnAttackMap = bitboard.computePawnAttacks(opponentColour);
        if (isBitSet(oppPawnAttackMap, 63 - friendlyKingPos)) {
            isInDoubleCheck = isInCheck;
            isInCheck = true;
            long attackStartSquares = colour > 0
                    ? bitboard.wPawnAttacks[friendlyKingPos]
                    : bitboard.bPawnAttacks[friendlyKingPos];
            long pawnCheckBB = oppPawnsBB & attackStartSquares;
            pathInCheckMask |= pawnCheckBB;
        }

        int oppKingPos = board.getKingPosition(-colour);

        oppAttackMapWithoutPawns = bitboard.kingMoves[oppKingPos] | oppKnightAttacks | oppSlidingAttacks;
        oppAttackMap = oppPawnAttackMap | oppAttackMapWithoutPawns;

        if (!isInCheck) {
            pathInCheckMask = MAX_LONG;
        }
    }

    private void computeSlidingAttacks() {
        oppSlidingAttacks = 0;
        updateAttacks(bitboard.diagonalSlider[opponentIndex], true);
        updateAttacks(bitboard.orthogonalSlider[opponentIndex], false);
    }

    private void updateAttacks(long board, boolean diagonal) {
        long blockers = allPieces & ~(bitboard.pieces[friendlyIndex][6]); //friendly king
        while (board != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(board);
            int fromPosition = plsbResult.first;
            board = plsbResult.second;
            oppSlidingAttacks |= bitboard.computeSlidingAttacks(fromPosition, blockers, diagonal);
        }
    }

    private void computeMovesForPawns() {
        int piece = PAWN * friendlyColour;
        int moveDirection = friendlyColour;;
        int moveOffset = moveDirection * 8;

        long pawns = bitboard.pieces[friendlyIndex][PAWN];
        long promotionRow = friendlyColour > 0 ? firstRow : lastRow;
        long oneSquarePush = shiftBits(pawns, moveOffset) & emptySquares;
        long oneSquarePushWithoutPromo = oneSquarePush & pathInCheckMask & ~promotionRow;

        if (quietMoves) {
            while (oneSquarePushWithoutPromo != 0) {
                Pair<Integer, Long> plsbResult = popLeastSignificantBit(oneSquarePushWithoutPromo);
                int toPosition = plsbResult.first;
                oneSquarePushWithoutPromo = plsbResult.second;
                int fromPosition = toPosition + moveOffset;
                if (!isPiecePinned(fromPosition) || dirPathMask[fromPosition][friendlyKingPos] == pathMask[toPosition][friendlyKingPos]) {
                    allMoves.add(new Move(piece, fromPosition, toPosition, board.chessboard[toPosition]));
                }
            }

            long twoSquaresPushDestRow = friendlyColour > 0 ? row5 : row4;
            long twoSqauresPush = shiftBits(oneSquarePush, moveOffset) & emptySquares & twoSquaresPushDestRow & pathInCheckMask;

            while (twoSqauresPush != 0) {
                Pair<Integer, Long> plsbResult = popLeastSignificantBit(twoSqauresPush);
                int fromPosition = plsbResult.first;
                twoSqauresPush = plsbResult.second;
                int toPosition = fromPosition + moveOffset * 2;
                if (!isPiecePinned(toPosition) || pathMask[toPosition][friendlyKingPos] == pathMask[fromPosition][friendlyKingPos]) {
                    Move move = new Move(piece, toPosition, fromPosition, board.chessboard[fromPosition]);
                    move.pawnTwoSquaresMove = true;
                    allMoves.add(move);
                }
            }
        }

        long leftColumnMask = friendlyColour > 0 ? notFirstColumn : notLastColumn; // left capture from piece perspective
        long rightColumnMask = friendlyColour > 0 ? notLastColumn : notFirstColumn; // right capture from piece perspective
        long leftCapture = shiftBits(pawns & leftColumnMask, moveDirection * 9) & opponentPieces;
        long rightCapture = shiftBits(pawns & rightColumnMask, moveDirection * 7) & opponentPieces;
        long leftCapturePromo = leftCapture & promotionRow & pathInCheckMask;
        long rightCapturePromo = rightCapture & promotionRow & pathInCheckMask;
        leftCapture &= pathInCheckMask & ~promotionRow;
        rightCapture &= pathInCheckMask & ~promotionRow;

        while (leftCapture != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(leftCapture);
            int toPosition = plsbResult.first;
            leftCapture = plsbResult.second;
            int fromPosition = toPosition + moveDirection * 9;

            if (!isPiecePinned(fromPosition) || pathMask[fromPosition][friendlyKingPos] == pathMask[toPosition][friendlyKingPos]) {
                allMoves.add(new Move(piece, fromPosition, toPosition, board.chessboard[toPosition]));
            }
        }

        while (rightCapture != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(rightCapture);
            int toPosition = plsbResult.first;
            rightCapture = plsbResult.second;
            int fromPosition = toPosition + moveDirection * 7;

            if (!isPiecePinned(fromPosition) || pathMask[fromPosition][friendlyKingPos] == pathMask[toPosition][friendlyKingPos]) {
                allMoves.add(new Move(piece, fromPosition, toPosition, board.chessboard[toPosition]));
            }
        }

        long pushPromo = promotionRow & oneSquarePush & pathInCheckMask;
        while (pushPromo != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(pushPromo);
            int toPosition = plsbResult.first;
            pushPromo = plsbResult.second;
            int fromPosition = toPosition + moveOffset;

            if (!isPiecePinned(fromPosition)) {
                Move move = new Move(piece, fromPosition, toPosition, board.chessboard[toPosition]);
                move.promotionFlag = true;
                allMoves.add(move);
            }
        }

        while (leftCapturePromo != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(leftCapturePromo);
            int toPosition = plsbResult.first;
            leftCapturePromo = plsbResult.second;
            int fromPosition = toPosition + moveDirection * 9;

            if (!isPiecePinned(fromPosition) || pathMask[fromPosition][friendlyKingPos] == pathMask[toPosition][friendlyKingPos]){
                Move move = new Move(piece, fromPosition, toPosition, board.chessboard[toPosition]);
                move.promotionFlag = true;
                allMoves.add(move);
            }
        }

        while (rightCapturePromo != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(rightCapturePromo);
            int toPosition = plsbResult.first;
            rightCapturePromo = plsbResult.second;
            int fromPosition = toPosition + moveDirection * 7;

            if (!isPiecePinned(fromPosition) || pathMask[fromPosition][friendlyKingPos] == pathMask[toPosition][friendlyKingPos]) {
                Move move = new Move(piece, fromPosition, toPosition, board.chessboard[toPosition]);
                move.promotionFlag = true;
                allMoves.add(move);
            }
        }

        long enPassantRow = friendlyColour > 0 ? row4 : row5;
        long enPassantAttackingPawns = pawns & enPassantRow;
        Move lastMove = board.getLastMove();
        if (lastMove.pawnTwoSquaresMove) {
            while (enPassantAttackingPawns != 0) {
                Pair<Integer, Long> plsbResult = popLeastSignificantBit(enPassantAttackingPawns);
                int fromPosition = plsbResult.first;
                enPassantAttackingPawns = plsbResult.second;
                int toPosition = fromPosition - moveOffset - 1;
                if (fromPosition == lastMove.targetSquare + 1 && !isEnPassantInvalid(fromPosition, toPosition, lastMove.targetSquare)) {
                    Move move = new Move(piece, fromPosition, toPosition, board.chessboard[toPosition]);
                    move.enPassantFlag = true;
                    move.enPassantPosition = lastMove.targetSquare;
                    allMoves.add(move);
                }
                toPosition = fromPosition - moveOffset + 1;
                if (fromPosition == lastMove.targetSquare - 1 && !isEnPassantInvalid(fromPosition, toPosition, lastMove.targetSquare)) {
                    Move move = new Move(piece, fromPosition, toPosition, board.chessboard[toPosition]);
                    move.enPassantFlag = true;
                    move.enPassantPosition = lastMove.targetSquare;
                    allMoves.add(move);
                }
            }
        }
    }

    private void computeMovesForKnights() {
        long knights = bitboard.pieces[friendlyIndex][KNIGHT] & notPinnedPaths;
        long possibleDestinations = opponentOrEmptySquares & pathInCheckMask & captureMask;

        while (knights != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(knights);
            int fromPosition = plsbResult.first;
            knights = plsbResult.second;
            long destinations = bitboard.knightAttacks[fromPosition] & possibleDestinations;

            while (destinations != 0) {
                plsbResult = popLeastSignificantBit(destinations);
                int toPosition = plsbResult.first;
                destinations = plsbResult.second;
                allMoves.add(new Move(KNIGHT*friendlyColour, fromPosition, toPosition, board.chessboard[toPosition]));
            }
        }
    }

    private void computeMovesForSliders() {
        long possibleDestinations = opponentOrEmptySquares & pathInCheckMask & captureMask;
        long diagSlidingPieces = bitboard.diagonalSlider[friendlyIndex];
        long orthoSlidingPieces = bitboard.orthogonalSlider[friendlyIndex];

        if (isInCheck) {
            diagSlidingPieces &= ~pinnedPaths;
            orthoSlidingPieces &= ~pinnedPaths;
        }

        while (diagSlidingPieces != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(diagSlidingPieces);
            int fromPosition = plsbResult.first;
            diagSlidingPieces = plsbResult.second;
            long destinations = bitboard.computeSlidingAttacks(fromPosition, allPieces, true) & possibleDestinations;

            if (isPiecePinned(fromPosition)) {
                destinations &= dirPathMask[fromPosition][friendlyKingPos];;
            }
            while (destinations != 0) {
                plsbResult = popLeastSignificantBit(destinations);
                int toPosition = plsbResult.first;
                destinations = plsbResult.second;
                allMoves.add(new Move(board.chessboard[fromPosition], fromPosition, toPosition, board.chessboard[toPosition]));
            }
        }

        while (orthoSlidingPieces != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(orthoSlidingPieces);
            int fromPosition = plsbResult.first;
            orthoSlidingPieces = plsbResult.second;
            long destinations = bitboard.computeSlidingAttacks(fromPosition, allPieces, false) & possibleDestinations;

            if (isPiecePinned(fromPosition)) {
                destinations &= dirPathMask[fromPosition][friendlyKingPos];
            }
            while (destinations != 0) {
                plsbResult = popLeastSignificantBit(destinations);
                int toPosition = plsbResult.first;
                destinations = plsbResult.second;
                allMoves.add(new Move(board.chessboard[fromPosition], fromPosition, toPosition, board.chessboard[toPosition]));
            }
        }
    }

    private void computeMovesForKing() {
        long possibleDestinations = ~(oppAttackMap | friendlyPieces);
        long kingDestinations = possibleDestinations & captureMask & bitboard.kingMoves[friendlyKingPos];
        int piece = KING * friendlyColour;
        while (kingDestinations != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(kingDestinations);
            int toPosition = plsbResult.first;
            kingDestinations = plsbResult.second;
            allMoves.add(new Move(piece, friendlyKingPos, toPosition, board.chessboard[toPosition]));
        }

        if (!isInCheck && quietMoves) {
            long castlingBlockersMask = oppAttackMap | allPieces;
            if (board.isQueensideCastlingEnabled(friendlyColour)) {
                long castlingSideMask = friendlyColour > 0 ? wQueenCastlingMask : bQueenCastlingMask;
                long castlingBlockMask = friendlyColour > 0 ? wQueenCastlingMask2 : bQueenCastlingMask2;
                if ((castlingBlockMask & castlingBlockersMask) == 0 && (castlingSideMask & allPieces) == 0) {
                    int toPosition = friendlyColour > 0 ? 58 : 2;
                    Move move = new Move(piece, friendlyKingPos, toPosition, board.chessboard[toPosition]);
                    move.castlingFlag = true;
                    move.preCastlingPosition = toPosition - 2;
                    move.postCastlingPosition = toPosition + 1;
                    allMoves.add(move);
                }
            }
            if (board.isKingsideCastlingEnabled(friendlyColour)) {
                long castlingSideMask = friendlyColour > 0 ? wKingCastlingMask : bKingCastlingMask;
                if ((castlingSideMask & castlingBlockersMask) == 0) {
                    int toPosition = friendlyColour > 0 ? 62 : 6;
                    Move move = new Move(piece, friendlyKingPos, toPosition, board.chessboard[toPosition]);
                    move.castlingFlag = true;
                    move.preCastlingPosition = toPosition + 1;
                    move.postCastlingPosition = toPosition - 1;
                    allMoves.add(move);
                }
            }
        }
    }

    private boolean isEnPassantInvalid(int fromPos, int toPos, int capturePos) {
        if (!isPiecePinned(fromPos) || pathMask[fromPos][friendlyKingPos] == pathMask[toPos][friendlyKingPos]){
            long opponentOrthogonalSliders = bitboard.orthogonalSlider[opponentIndex];

            if (opponentOrthogonalSliders != 0) {
                long blockers = (allPieces ^ (1L << 63 - capturePos | 1L << 63 - fromPos | 1L << 63 - toPos));
                long rookAttacks = bitboard.computeSlidingAttacks(friendlyKingPos, blockers, false);
                return (rookAttacks & opponentOrthogonalSliders) != 0;
            }
            return false;
        }
        return  true;
    }

    private boolean isPiecePinned(int square)
    {
        return ((pinnedPaths >> 63 - square) & 1) != 0;
    }
}
