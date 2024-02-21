package com.chessengine.chessengineservice.Helpers;

import com.chessengine.chessengineservice.Move;

import java.util.List;

import static com.chessengine.chessengineservice.Piece.*;
import static java.lang.Math.abs;

public class PGNHelper {

    public static String convertMoveToPGN(Move move) {
        String pgn = "";
        pgn += abs(move.piece) == PAWN ? "" : getPieceLetter(move.piece);
        pgn += (char)(move.toY + 'a');
        pgn += String.valueOf(8 - move.toX);
        return pgn + " ";
    }

    public static Move convertPGNToMove(String pgn, List<Move> moves) {
        int piece = pgn.length() == 2 ? PAWN : getPieceFromLetter(pgn.substring(0, 1));
        if(pgn.length() == 2) {
            pgn = "0" + pgn;
        }
        int targetX = 8 - (pgn.charAt(2) - '0');
        int targetY = pgn.charAt(1) - 'a';
        for (Move move : moves) {
            if(abs(move.piece) == piece && move.toX == targetX && move.toY == targetY) {
                return move;
            }
        }
        return null;
    }

    private static char getPieceLetter(int piece) {
        piece = abs(piece);
        if(piece == KNIGHT) {
            return 'N';
        } else if(piece == BISHOP) {
            return 'B';
        } else if (piece == QUEEN) {
            return 'Q';
        } else if (piece == KING) {
            return 'K';
        }
        return 0;
    }

    private static int getPieceFromLetter(String letter) {
        return switch (letter) {
            case "N" -> KNIGHT;
            case "B" -> BISHOP;
            case "Q" -> QUEEN;
            case "K" -> KING;
            default -> 0;
        };
    }
}
