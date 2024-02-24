package com.chessengine.chessengineservice;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ChessEngineController {

    ChessEngineService chessEngineService = new ChessEngineService();

    @GetMapping("/move")
    @CrossOrigin(origins = "http://localhost:3000")
    public Move getBestMove(@RequestParam int id, @RequestParam int colour) {
        return chessEngineService.getBestMove(id, colour);
    }

    @GetMapping("/allMoves")
    @CrossOrigin(origins = "http://localhost:3000")
    public @ResponseBody List<Move> getAllMoves(@RequestParam int id, @RequestParam int colour) {
        return chessEngineService.getAllMoves(id, colour);
    }

    @PostMapping("/makeMove")
    @CrossOrigin(origins = "http://localhost:3000")
    public int makeMove(@RequestParam int id, @RequestBody Move move) {
        return chessEngineService.makeMove(id, move);
    }

    @GetMapping("/newGame")
    @CrossOrigin(origins = "http://localhost:3000")
    public int startNewGame(@RequestParam int colour, @RequestParam int playerId) {
        return chessEngineService.createGameOrJoinQueue(colour, playerId);
    }

    @GetMapping("/fetchMove")
    @CrossOrigin(origins = "http://localhost:3000")
    public Move getLastMove(@RequestParam int gameId) {
        return chessEngineService.getLastMove(gameId);
    }

    @PostMapping("/reset")
    @CrossOrigin(origins = "http://localhost:3000")
    public void reset() {
        chessEngineService.reset();
    }

    @GetMapping("/generateId")
    @CrossOrigin(origins = "http://localhost:3000")
    public int generateId() {
        return chessEngineService.generatePlayerId();
    }
}
