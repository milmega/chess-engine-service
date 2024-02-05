package com.chessengine.chessengineservice.MoveGenerator;

import com.chessengine.chessengineservice.*;

import java.util.ArrayList;
import java.util.List;

import static com.chessengine.chessengineservice.Bitboard.*;
import static com.chessengine.chessengineservice.Helpers.BoardHelper.*;
import static com.chessengine.chessengineservice.MoveGenerator.PrecomputedMoveData.*;
import static com.chessengine.chessengineservice.Piece.*;

//TODO: implement move ordering
//TODO: implement opening tree
//TODO: implement endgaame
//TODO implement transposition table - convert board to unique fen key
//TODO: refactor Pair to be Moves and use move flags

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
                if (startSquare == lastMove.targetSquare + 1 && !isInCheckAfterEnpassant(startSquare, targetSquare, lastMove.targetSquare)) { //TODO: check if friendly king is not in check after enpassant
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

    public List<Move> generateAllMoves(int colour, Board board) {
        List<Move> allMoves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (!isSameColour(colour, board.square[i])) {
                continue;
            }
            List<Pair<Integer, Integer>> validMoves = getValidMoves(i, board);
            int finalI = i;
            validMoves.forEach(move -> {
                int targetSquare = finalI+coorsToPos(move.first, move.second);
                allMoves.add(new Move(board.square[finalI], finalI, targetSquare, board.square[targetSquare]));
            });
        }
        return allMoves;
    }

    //filters list of valid moves to prevent checking the king and enables moves that defend from checking
    public List<Pair<Integer, Integer>> getValidMoves(int pos, Board board) {
        /*int piece = board.square[pos];
        int colour = piece > 0 ? 1 : -1;
        List<Pair<Integer, Integer>> moves = getMoves(pos, board);
        List<Pair<Integer, Integer>> validMoves = new ArrayList<>(moves.stream().filter(move -> {
            int newPos = pos+coorsToPos(move.first, move.second);
            board.makeMove(new Move(pos, newPos), true);
            boolean isKingUnderCheck = board.isInCheck(colour);
            board.unmakeMove(new Move(pos, newPos));
            return !isKingUnderCheck;
        }).toList());

        boolean[] castling = board.getCastling(colour);
        if (abs(piece) == KING && !castling[0] && (!castling[1] || !castling[2])) {
            List<Pair<Integer, Integer>> castlingMoves = getCastlingMoves(colour, board);
            validMoves.addAll(castlingMoves);
        }
        return validMoves;*/
        return new ArrayList<>();
    }

    /*private List<Pair<Integer, Integer>> getMoves(int pos, Board board) {
        int piece = board.square[pos];
        int colour = piece > 0 ? 1 : -1;
        int x = posToX(pos);
        int y = posToX(pos);
        List<Pair<Integer, Integer>> movesForFigure = getMovesForPiece(colour, pos, board);
        Move lastMove = board.getLastMove();
        int lastMovePiece = lastMove.startSquare == -1 ? 0 : board.square[lastMove.targetSquare];

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
                return !isSameColour(board.square[coorsToPos(newX, newY)], piece); //ignore if both are the same colour
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
    */
    //returns list of attack moves for a figure to show a circle around potential prey or a list of all squares under attack that king cannot go to
    public List<Move> getAttackMoves(int pos, Board board) {
        return new ArrayList<>();
        /*int colour = board.square[pos] > 0 ? 1 : -1;
        List<Pair<Integer, Integer>> moves = getMoves(pos, board); // I don't need to call getValidMoves because it only checks if opposite king is in check
        return moves.stream()
                .filter(move -> board.square[pos+coorsToPos(move.first, move.second)] != 0 &&
                        !isSameColour(board.square[pos+coorsToPos(move.first, move.second)], colour))
                .map(move -> new Move(pos, pos + move.first*8 + move.second)).toList();*/
    }
    /*
    public List<Pair<Integer, Integer>> getMovesForPiece(int colour, int pos, Board board) {
        List<Pair<Integer, Integer>> pieceMoves = new ArrayList<>();
        int piece = abs(board.square[pos]);
        int x = posToX(pos);
        int y = posToX(pos);
        if (piece == PAWN) {
            List<Pair<Integer, Integer>> pawnMoves = new ArrayList<>();
            if (colour > 0) {
                if (x > 0 && board.square[coorsToPos(x-1, y)] == 0){
                    pawnMoves.add(new Pair<>(-1, 0));
                }
                if (x == 6 && board.square[40+y] == 0 && board.square[32+y] == 0) {
                    pawnMoves.add(new Pair<>(-2, 0));
                }
                if (x > 0 && y > 0 && board.square[coorsToPos(x-1, y-1)] < 0) { //value smaller than 0 it's black
                    pawnMoves.add(new Pair<>(-1, -1));
                }
                if (x > 0 && y < 7 && board.square[coorsToPos(x-1, y+1)] < 0) { //value smaller than 0 it's black
                    pawnMoves.add(new Pair<>(-1, 1));
                }
            } else {
                if (x < 7 && board.square[coorsToPos(x+1, y)] == 0){
                    pawnMoves.add(new Pair<>(1, 0));
                }
                if (x == 1 && board.square[16+y] == 0 && board.square[24+y] == 0) {
                    pawnMoves.add(new Pair<>(2, 0));
                }
                if (x < 7 && y > 0 && board.square[coorsToPos(x+1, y-1)] > 0) { //value bigger than 0 it's white
                    pawnMoves.add(new Pair<>(1, -1));
                }
                if (x < 7 && y < 7 && board.square[coorsToPos(x+1, y+1)] > 0) { //value bigger than 0 it's white
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
        int lastMoveFromX = posToX(lastMove.startSquare);
        int lastMoveToX = posToX(lastMove.targetSquare);
        int lastMoveToY = posToX(lastMove.targetSquare);

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
            int finalI = posToX(i);
            int finalJ = posToY(i);
            leftCastlingEnabled = moves.stream().noneMatch(move -> move.first + finalI  == row && move.second + finalJ < 5); //TODO: change the evaluation
            rightCastlingEnabled = moves.stream().noneMatch(move -> move.first + finalI == row && move.second + finalJ > 3);
        }
        if (!castling[1] && leftCastlingEnabled && board.square[coorsToPos(row, 1)] == 0 && board.square[coorsToPos(row, 2)] == 0 && board.square[coorsToPos(row,3)] == 0) { //if left rook hasn't moved
            castlingMoves.add(new Pair<>(0, -2));
        }
        if (!castling[2] && rightCastlingEnabled && board.square[coorsToPos(row, 5)] == 0 && board.square[coorsToPos(row, 6)] == 0) { //if right rook hasn't moved
            castlingMoves.add(new Pair<>(0, 2));
        }
        return castlingMoves;
    }*/
}
