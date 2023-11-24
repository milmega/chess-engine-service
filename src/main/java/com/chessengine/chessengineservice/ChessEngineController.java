package com.chessengine.chessengineservice;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChessEngineController {

    ChessEngineService chessEngineService = new ChessEngineService();

    @GetMapping("/move")
    @CrossOrigin(origins = "http://localhost:3000")
    public String GetNextMove(@RequestParam String board,
                              @RequestParam int colour,
                              @RequestParam int whiteKing,
                              @RequestParam int blackKing,
                              @RequestParam String whiteCastling,
                              @RequestParam String blackCastling) {
        GameEngine.setKingPosition(1, whiteKing / 8, whiteKing % 8);
        GameEngine.setKingPosition(-1, blackKing / 8, blackKing % 8);
        GameEngine.setCastling(1, whiteCastling);
        GameEngine.setCastling(-1, blackCastling);
        int[][] board2D = new int[8][8];
        int index = 0;
        for (int i = 0; i < board.length(); i++) {
            String character = board.substring(i, i+1);
            if(character.equals("-")) {
                i++;
                character = character.concat(board.substring(i, i+1));
            }
            board2D[index/8][index%8] = Integer.parseInt(character);
            index++;
        }
        return chessEngineService.GetNextMove(colour, board2D);
    }
}
