package com.chessengine.chessengineservice;

import com.chessengine.chessengineservice.Game.GameStatus;
import com.chessengine.chessengineservice.Structures.Move;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ChessEngineController {

    ChessEngineService chessEngineService = new ChessEngineService();

    @GetMapping("/move")
    @CrossOrigin
    public Move getBestMove(@RequestParam int id, @RequestParam int colour) {
        return chessEngineService.getBestMove(id, colour);
    }

    @GetMapping("/allMoves")
    @CrossOrigin
    public @ResponseBody List<Move> getAllMoves(@RequestParam int id, @RequestParam int colour) {
        return chessEngineService.getAllMoves(id, colour);
    }

    @PostMapping("/makeMove")
    @CrossOrigin
    public int makeMove(@RequestParam int id, @RequestBody Move move) {
        return chessEngineService.makeMove(id, move);
    }

    @GetMapping("/newGame")
    @CrossOrigin
    public int startNewGame(@RequestParam int colour, @RequestParam int playerId, @RequestParam int level, @RequestParam boolean online) {
        return chessEngineService.createGameOrJoinQueue(colour, playerId, level, online);
    }

    @GetMapping("/fetchUpdate")
    @CrossOrigin
    public GameStatus fetchUpdate(@RequestParam int id) {
        return chessEngineService.getGameUpdate(id);
    }

    @PostMapping("/cancelSearch")
    @CrossOrigin
    public void cancelSearch(@RequestParam int id) {
        chessEngineService.cancelSearch(id);
    }


    @PostMapping("/reset")
    @CrossOrigin
    public void reset(@RequestParam int id) {
        if (id == 0) {
            chessEngineService.resetGame(id);
        } else {
            chessEngineService.deleteGame(id);
        }
    }

    @GetMapping("/generateId")
    @CrossOrigin
    public int generateId() {
        return chessEngineService.generatePlayerId();
    }
}
