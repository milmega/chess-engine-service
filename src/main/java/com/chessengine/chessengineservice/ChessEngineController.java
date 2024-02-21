package com.chessengine.chessengineservice;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ChessEngineController {

    ChessEngineService chessEngineService = new ChessEngineService();

    @GetMapping("/move")
    @CrossOrigin(origins = "http://localhost:3000")
    public Move getBestMove(@RequestParam int colour) {
        return chessEngineService.getBestMove(colour);
    }

    @GetMapping("/allMoves")
    @CrossOrigin(origins = "http://localhost:3000")
    public @ResponseBody List<Move> getAllMoves(@RequestParam int colour) {
        return chessEngineService.getAllMoves(colour);
    }

    @PostMapping("/makeMove")
    @CrossOrigin(origins = "http://localhost:3000")
    public int makeMove(@RequestBody Move move) {
        chessEngineService.getBoard().makeMove(move, false);
        return chessEngineService.getBoard().getGameResult(-move.colour);
    }

    @PostMapping("/reset")
    @CrossOrigin(origins = "http://localhost:3000")
    public void reset() {
        chessEngineService.reset();
    }
}
