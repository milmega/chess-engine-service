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
        chessEngineService.getBoard().makeMove(new Move(start, destination), false); //TODO: there is inconsistency between front-end board and backend board
        return chessEngineService.GetNextMove(colour);
    }

    @PostMapping("/reset")
    @CrossOrigin(origins = "http://localhost:3000")
    public void Reset() {
        chessEngineService.getBoard().initializeBoard();
    }
}
