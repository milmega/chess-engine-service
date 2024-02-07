package com.chessengine.chessengineservice.MoveGenerator;

import com.chessengine.chessengineservice.*;

import java.util.ArrayList;
import java.util.List;

import static com.chessengine.chessengineservice.Bitboard.*;
import static com.chessengine.chessengineservice.Helpers.BitboardHelper.*;
import static com.chessengine.chessengineservice.Helpers.BoardHelper.*;
import static com.chessengine.chessengineservice.MoveGenerator.PrecomputedMoveData.*;
import static com.chessengine.chessengineservice.Piece.*;

//TODO: implement opening tree
//TODO: implement endgaame
//TODO implement transposition table - convert board to unique fen key

public class MoveGenerator {

    final long MAX_LONG = 0xFFFFFFFFFFFFFFFFL;
    final long whiteKingsideMask = 0x6L;
    final long whiteQueensideMask = 0x70L;
    final long whiteQueensideMask2 = 0x30L;
    final long blackKingsideMask = 0x6L << 56;
    final long blackQueensideMask = 0x7L << 60;
    final long blackQueensideMask2 = 0x3L << 60;
    int friendlyColour;
    int opponentColour;
    int friendlyKingSquare;
    int friendlyIndex;
    int enemyIndex;

    boolean inCheck;
    boolean inDoubleCheck;

    // If in check, this bitboard contains squares in line from checking piece up to king
    // If not in check, all bits are set to 1
    long checkRayBitmask;
    long pinRays;
    long notPinRays;
    long opponentAttackMapNoPawns;
    public long opponentAttackMap;
    public long opponentPawnAttackMap;
    long opponentSlidingAttackMap;
    boolean generateQuietMoves;
    Board board;
    int currMoveIndex;

    long enemyPieces;
    long friendlyPieces;
    long allPieces;
    long emptySquares;
    long emptyOrEnemySquares;
    // If only captures should be generated, this will have 1s only in positions of enemy pieces.
    // Otherwise it will have 1s everywhere.
    long moveTypeMask;

    Bitboard bitboard;
    List<Move> allMoves;

    public boolean isKingInCheck() {
        return inCheck;
    }

    public List<Move> generateMoves(int colour, Board board, boolean capturesOnly) {
        allMoves = new ArrayList<>();
        this.board = board;
        this.bitboard = board.bitboard;
        generateQuietMoves = !capturesOnly;

        initialize(colour);

        getKingMoves();

        if (!inDoubleCheck) {
            getSlidingMoves();
            getKnightMoves();
            getPawnMoves();
        }
        return allMoves;
    }

    private void initialize(int colour) {
        // Reset state
        currMoveIndex = 0;
        inCheck = false;
        inDoubleCheck = false;
        checkRayBitmask = 0;
        pinRays = 0;
        allMoves = new ArrayList<>();

        // Store some info for convenience
        friendlyColour = colour;
        opponentColour = -colour;
        friendlyKingSquare = board.getKingPosition(colour);
        friendlyIndex = colour > 0 ? 0 : 1;
        enemyIndex = 1 - friendlyIndex;

        // Store some bitboards for convenience
        enemyPieces = bitboard.pieces[enemyIndex][0];
        friendlyPieces = bitboard.pieces[friendlyIndex][0];
        allPieces = bitboard.allPieces;
        emptySquares = ~allPieces;
        emptyOrEnemySquares = emptySquares | enemyPieces;
        moveTypeMask = generateQuietMoves ? MAX_LONG : enemyPieces;

        getAttackData(colour);
    }

    private void getAttackData(int colour) {
        getSlidingAttackMap();
        int friendlyKingSquare = board.getKingPosition(colour);
        int friendlyIndex = colour > 0 ? 0 : 1;
        int opponentIndex = 1 - friendlyIndex;
        int startIndex = 0;
        int endIndex = 8;

        if (bitboard.pieces[opponentIndex][QUEEN] == 0) {
            startIndex = bitboard.pieces[opponentIndex][ROOK] > 0 ? 0 : 4;
            endIndex = bitboard.pieces[opponentIndex][BISHOP] > 0 ? 8 : 4;
        }

        for (int dir = startIndex; dir < endIndex; dir++) {
            boolean isDiagonal = dir > 3;
            long slider = isDiagonal ? bitboard.diagonalSlider[opponentIndex] : bitboard.orthogonalSlider[opponentIndex];
            var m = dirRayMask[dir][friendlyKingSquare];
            if ((m & slider) == 0) {
                continue;
            }

            int n = distFromEdge[friendlyKingSquare][dir];
            int directionOffset = squareChangeOffset[dir];
            boolean isFriendlyPieceAlongRay = false;
            long rayMask = 0;

            for (int i = 0; i < n; i++) {
                int squareIndex = friendlyKingSquare + directionOffset * (i + 1);
                rayMask |= 1L << 63 - squareIndex;
                int piece = board.square[squareIndex];

                if (piece != 0) {
                    if (isSameColour(piece, colour)) {
                        // First friendly piece we have come across in this direction, so it might be pinned
                        if (!isFriendlyPieceAlongRay) {
                            isFriendlyPieceAlongRay = true;
                        } else { // This is the second friendly piece we've found in this direction, therefore pin is not possible
                            break;
                        }
                    } else { // This square contains an enemy piece
                        // Check if piece is in bitmask of pieces able to move in current direction
                        if (isDiagonal && isQueenOrBishop(piece) || !isDiagonal && isQueenOrRook(piece)) {
                            // Friendly piece blocks the check, so this is a pin
                            if (isFriendlyPieceAlongRay) {
                                pinRays |= rayMask;
                            } else { // No friendly piece blocking the attack, so this is a check
                                checkRayBitmask |= rayMask;
                                inDoubleCheck = inCheck; // if already in check, then this is double check
                                inCheck = true;
                            }
                            break;
                        }
                        else {
                            // This enemy piece is not able to move in the current direction, and so is blocking any checks/pins
                            break;
                        }
                    }
                }
            }
            // Stop searching for pins if in double check, as the king is the only piece able to move in that case anyway
            if (inDoubleCheck) {
                break;
            }
        }

        notPinRays = ~pinRays;
        long opponentKnightAttacks = 0;
        long knights = bitboard.pieces[opponentIndex][KNIGHT];
        long friendlyKingBoard = bitboard.pieces[friendlyIndex][KING];

        while (knights != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(knights);
            int knightSquare = plsbResult.first;
            knights = plsbResult.second;
            long knightAttacks = bitboard.knightAttacks[knightSquare];
            opponentKnightAttacks |= knightAttacks;

            if ((knightAttacks & friendlyKingBoard) != 0)
            {
                inDoubleCheck = inCheck;
                inCheck = true;
                checkRayBitmask |= 1L << 63 - knightSquare;
            }
        }

        opponentPawnAttackMap = 0;
        long opponentPawnsBoard = bitboard.pieces[opponentIndex][PAWN];
        opponentPawnAttackMap = bitboard.getPawnAttacks(opponentColour);
        if (isBitSet(opponentPawnAttackMap, 63 - friendlyKingSquare)) {
            inDoubleCheck = inCheck; // if already in check, then this is double check
            inCheck = true;
            long possiblePawnAttackOrigins = colour > 0
                    ? bitboard.whitePawnAttacks[friendlyKingSquare]
                    : bitboard.blackPawnAttacks[friendlyKingSquare];
            long pawnCheckMap = opponentPawnsBoard & possiblePawnAttackOrigins;
            checkRayBitmask |= pawnCheckMap;
        }

        int enemyKingSquare = board.getKingPosition(-colour);

        opponentAttackMapNoPawns = opponentSlidingAttackMap | opponentKnightAttacks | bitboard.kingMoves[enemyKingSquare];
        opponentAttackMap = opponentAttackMapNoPawns | opponentPawnAttackMap;

        if (!inCheck) {
            checkRayBitmask = MAX_LONG;
        }
    }

    private void getSlidingAttackMap() {
        opponentSlidingAttackMap = 0;
        updateAttackMap(bitboard.diagonalSlider[enemyIndex], true); //enemy sliders
        updateAttackMap(bitboard.orthogonalSlider[enemyIndex], false); //enemy sliders
    }

    private void updateAttackMap(long board, boolean diagonal) {
        long blockers = allPieces & ~(bitboard.pieces[friendlyIndex][6]); //friendly king

        while (board != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(board);
            int startSquare = plsbResult.first;
            board = plsbResult.second;
            opponentSlidingAttackMap |= bitboard.getSliderAttacks(startSquare, blockers, diagonal);
        }
    }

    private void getKingMoves() {
        long legalMask = ~(opponentAttackMap | friendlyPieces);
        long kingMoves = bitboard.kingMoves[friendlyKingSquare] & legalMask & moveTypeMask;
        int piece = KING * friendlyColour;
        while (kingMoves != 0)
        {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(kingMoves);//popLeastSignificantBit()(ref kingMoves);
            int targetSquare = plsbResult.first;
            kingMoves = plsbResult.second;
            allMoves.add(new Move(piece, friendlyKingSquare, targetSquare, board.square[targetSquare]));
        }

        // add castling moves
        if (!inCheck && generateQuietMoves) {
            long castleBlockers = opponentAttackMap | allPieces;
            boolean[] castling = board.getCastling(friendlyColour);

            if (!castling[0] && !castling[1]) { // queenside castling
                long castleMask = friendlyColour > 0 ? whiteQueensideMask : blackQueensideMask;
                long castleBlockMask = friendlyColour > 0 ? whiteQueensideMask2 : blackQueensideMask2;
                if ((castleBlockMask & castleBlockers) == 0 && (castleMask & allPieces) == 0) {
                    int targetSquare = friendlyColour > 0 ? 58 : 2;
                    Move move = new Move(piece, friendlyKingSquare, targetSquare, board.square[targetSquare]);
                    move.castlingFlag = true;
                    move.preCastlingPosition = targetSquare - 2;
                    move.postCastlingPosition = targetSquare + 1;
                    allMoves.add(move);
                }
            }
            if (!castling[0] && !castling[2]) { // kingside castling
                long castleMask = friendlyColour > 0 ? whiteKingsideMask : blackKingsideMask;
                if ((castleMask & castleBlockers) == 0)
                {
                    int targetSquare = friendlyColour > 0 ? 62 : 6;
                    Move move = new Move(piece, friendlyKingSquare, targetSquare, board.square[targetSquare]);
                    move.castlingFlag = true;
                    move.preCastlingPosition = targetSquare + 1;
                    move.postCastlingPosition = targetSquare - 1;
                    allMoves.add(move);
                }
            }
        }
    }

    private void getSlidingMoves() {
        long moveMask = emptyOrEnemySquares & checkRayBitmask & moveTypeMask;
        long orthogonalSliders = bitboard.orthogonalSlider[friendlyIndex];
        long diagonalSliders = bitboard.diagonalSlider[friendlyIndex];

        // Pinned pieces cannot move if king is in check
        if (inCheck) {
            orthogonalSliders &= ~pinRays;
            diagonalSliders &= ~pinRays;
        }

        while (orthogonalSliders != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(orthogonalSliders);
            int startSquare = plsbResult.first;
            orthogonalSliders = plsbResult.second;
            long moveSquares = bitboard.getSliderAttacks(startSquare, allPieces, false) & moveMask;

            // If piece is pinned, it can only move along the pin ray
            if (isPiecePinned(startSquare)) {
                moveSquares &= alignMask[startSquare][friendlyKingSquare];
            }

            while (moveSquares != 0) {
                plsbResult = popLeastSignificantBit(moveSquares);
                int targetSquare = plsbResult.first;
                moveSquares = plsbResult.second;
                allMoves.add(new Move(board.square[startSquare], startSquare, targetSquare, board.square[targetSquare]));
            }
        }

        while (diagonalSliders != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(diagonalSliders);
            int startSquare = plsbResult.first;
            diagonalSliders = plsbResult.second;
            long moveSquares = bitboard.getSliderAttacks(startSquare, allPieces, true) & moveMask;

            // If piece is pinned, it can only move along the pin ray
            if (isPiecePinned(startSquare)) {
                var x = alignMask[startSquare][friendlyKingSquare];
                moveSquares &= x;
            }

            while (moveSquares != 0) {
                plsbResult = popLeastSignificantBit(moveSquares);
                int targetSquare = plsbResult.first;
                moveSquares = plsbResult.second;
                allMoves.add(new Move(board.square[startSquare], startSquare, targetSquare, board.square[targetSquare]));
            }
        }
    }

    private void getKnightMoves() {
        long knights = bitboard.pieces[friendlyIndex][KNIGHT] & notPinRays;
        long moveMask = emptyOrEnemySquares & checkRayBitmask & moveTypeMask;

        while (knights != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(knights);
            int knightSquare = plsbResult.first;
            knights = plsbResult.second;
            long moveSquares = bitboard.knightAttacks[knightSquare] & moveMask;

            while (moveSquares != 0) {
                plsbResult = popLeastSignificantBit(moveSquares);
                int targetSquare = plsbResult.first;
                moveSquares = plsbResult.second;
                allMoves.add(new Move(KNIGHT*friendlyColour, knightSquare, targetSquare, board.square[targetSquare]));
            }
        }
    }

    private void getPawnMoves() {
        int piece = PAWN * friendlyColour;
        int pushDir = friendlyColour;;
        int pushOffset = pushDir * 8;

        long pawns = bitboard.pieces[friendlyIndex][PAWN];
        long promotionRowMask = friendlyColour > 0 ? firstRow : lastRow;
        long enpassantRowMask = friendlyColour > 0 ? row4 : row5;
        long enpassantAttackingPawns = pawns & enpassantRowMask;

        long singlePush = shiftBits(pawns, pushOffset) & emptySquares;
        long pushPromotions = singlePush & promotionRowMask & checkRayBitmask;

        long captureEdgeColumnMask = friendlyColour > 0 ? notFirstColumn : notLastColumn; // left capture from piece perspective
        long captureEdgeColumnMask2 = friendlyColour > 0 ? notLastColumn : notFirstColumn; // right capture from piece perspective
        long captureA = shiftBits(pawns & captureEdgeColumnMask, pushDir * 9) & enemyPieces;
        long captureB = shiftBits(pawns & captureEdgeColumnMask2, pushDir * 7) & enemyPieces;

        long singlePushNoPromotions = singlePush & ~promotionRowMask & checkRayBitmask;

        long capturePromotionsA = captureA & promotionRowMask & checkRayBitmask;
        long capturePromotionsB = captureB & promotionRowMask & checkRayBitmask;

        captureA &= checkRayBitmask & ~promotionRowMask;
        captureB &= checkRayBitmask & ~promotionRowMask;

        // Single / double push
        if (generateQuietMoves) {
            // Generate single pawn pushes
            while (singlePushNoPromotions != 0) {
                Pair<Integer, Long> plsbResult = popLeastSignificantBit(singlePushNoPromotions);
                int targetSquare = plsbResult.first;
                singlePushNoPromotions = plsbResult.second;
                int startSquare = targetSquare + pushOffset;
                if (!isPiecePinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                    allMoves.add(new Move(piece, startSquare, targetSquare, board.square[targetSquare]));
                }
            }

            // Generate double pawn pushes
            long doublePushTargetRankMask = friendlyColour > 0 ? row5 : row4;
            long doublePush = shiftBits(singlePush, pushOffset) & emptySquares & doublePushTargetRankMask & checkRayBitmask;

            while (doublePush != 0) {
                Pair<Integer, Long> plsbResult = popLeastSignificantBit(doublePush);
                int targetSquare = plsbResult.first;
                doublePush = plsbResult.second;
                int startSquare = targetSquare + pushOffset * 2;
                if (!isPiecePinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                    Move move = new Move(piece, startSquare, targetSquare, board.square[targetSquare]);
                    move.pawnTwoSquaresMove = true;
                    allMoves.add(move);
                }
            }
        }

        // Captures
        while (captureA != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(captureA);
            int targetSquare = plsbResult.first;
            captureA = plsbResult.second;
            int startSquare = targetSquare + pushDir * 9;

            if (!isPiecePinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                allMoves.add(new Move(piece, startSquare, targetSquare, board.square[targetSquare]));
            }
        }

        while (captureB != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(captureB);
            int targetSquare = plsbResult.first;
            captureB = plsbResult.second;
            int startSquare = targetSquare + pushDir * 7;

            if (!isPiecePinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                allMoves.add(new Move(piece, startSquare, targetSquare, board.square[targetSquare]));
            }
        }

        // Promotions
        while (pushPromotions != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(pushPromotions);
            int targetSquare = plsbResult.first;
            pushPromotions = plsbResult.second;
            int startSquare = targetSquare + pushOffset;

            if (!isPiecePinned(startSquare)) {
                Move move = new Move(piece, startSquare, targetSquare, board.square[targetSquare]);
                move.promotionFlag = true;
                allMoves.add(move);
            }
        }

        while (capturePromotionsA != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(capturePromotionsA);
            int targetSquare = plsbResult.first;
            capturePromotionsA = plsbResult.second;
            int startSquare = targetSquare + pushDir * 9;

            if (!isPiecePinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]){
                Move move = new Move(piece, startSquare, targetSquare, board.square[targetSquare]);
                move.promotionFlag = true;
                allMoves.add(move);
            }
        }

        while (capturePromotionsB != 0) {
            Pair<Integer, Long> plsbResult = popLeastSignificantBit(capturePromotionsB);
            int targetSquare = plsbResult.first;
            capturePromotionsB = plsbResult.second;
            int startSquare = targetSquare + pushDir * 7;

            if (!isPiecePinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                Move move = new Move(piece, startSquare, targetSquare, board.square[targetSquare]);
                move.promotionFlag = true;
                allMoves.add(move);
            }
        }

        // En passant
        Move lastMove = board.getLastMove();
        if (lastMove.pawnTwoSquaresMove) {
            while (enpassantAttackingPawns != 0) {
                Pair<Integer, Long> plsbResult = popLeastSignificantBit(enpassantAttackingPawns);
                int startSquare = plsbResult.first;
                enpassantAttackingPawns = plsbResult.second;
                int targetSquare = startSquare - pushOffset -1;
                if (startSquare == lastMove.targetSquare + 1 && !isInCheckAfterEnpassant(startSquare, targetSquare, lastMove.targetSquare)) {
                    Move move = new Move(piece, startSquare, targetSquare, board.square[targetSquare]);
                    move.enpassantFlag = true;
                    move.enpassantPosition = lastMove.targetSquare;
                    allMoves.add(move);
                }
                targetSquare = startSquare - pushOffset + 1;
                if (startSquare == lastMove.targetSquare - 1 && !isInCheckAfterEnpassant(startSquare, targetSquare, lastMove.targetSquare)) {
                    Move move = new Move(piece, startSquare, targetSquare, board.square[targetSquare]);
                    move.enpassantFlag = true;
                    move.enpassantPosition = lastMove.targetSquare;
                    allMoves.add(move);
                }
            }
        }
    }

    private boolean isInCheckAfterEnpassant(int startSquare, int targetSquare, int captureSquare) {
        if (!isPiecePinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]){
            long opponentOrthogonalSliders = bitboard.orthogonalSlider[enemyIndex];

            if (opponentOrthogonalSliders != 0) {
                long maskedBlockers = (allPieces ^ (1L << 63 - captureSquare | 1L << 63 - startSquare | 1L << 63 - targetSquare));
                long rookAttacks = bitboard.getSliderAttacks(friendlyKingSquare, maskedBlockers, false);
                return (rookAttacks & opponentOrthogonalSliders) != 0;
            }

            return false;
        }
        return  true;
    }

    private boolean isPiecePinned(int square)
    {
        return ((pinRays >> 63 - square) & 1) != 0;
    }
}
