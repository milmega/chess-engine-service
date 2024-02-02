package com.chessengine.chessengineservice;

import org.springframework.web.bind.annotation.*;

@RestController
public class ChessEngineController {

    ChessEngineService chessEngineService = new ChessEngineService();

    @GetMapping("/move")
    @CrossOrigin(origins = "http://localhost:3000")
    public String GetNextMove(@RequestParam int start,
                              @RequestParam int destination,
                              @RequestParam int colour) {
        if (start > -1) { // if computer is white, start and destination are -1
            chessEngineService.getBoard().makeMove(new Move(start, destination, -colour), false);
        }
        return chessEngineService.GetNextMoveAsString(colour);
    }

    @PostMapping("/reset")
    @CrossOrigin(origins = "http://localhost:3000")
    public void Reset() {
        chessEngineService.getBoard().resetBoard();
    }
}
