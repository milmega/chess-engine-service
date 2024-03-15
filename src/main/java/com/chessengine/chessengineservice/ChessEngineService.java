package com.chessengine.chessengineservice;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class ChessEngineService {
    Map<Integer, Game> gameById;
    Map<Integer, Integer> gameByPlayerId;
    Queue<Integer> whiteQueue;
    Queue<Integer> blackQueue;
    static int _gameId = 0;
    static int _playerID = 0;


    public ChessEngineService() {
        gameById = new ConcurrentHashMap<>();
        gameByPlayerId = new ConcurrentHashMap<>();
        whiteQueue = new ConcurrentLinkedQueue<>();
        blackQueue = new ConcurrentLinkedQueue<>();
    }

    public int generatePlayerId() {
        _playerID++;
        System.out.println("Generating Id: " + _playerID);
        return _playerID;
    }

    public int createGameOrJoinQueue(int colour, int playerId, int level, boolean online) {
        if (!online) {
           createGame(playerId, generatePlayerId(), level);
           return _gameId;
        }
        if (gameByPlayerId.containsKey(playerId)) {
            return gameByPlayerId.get(playerId);
        }
        if (colour == 1) {
            if (!blackQueue.isEmpty()) {
                int opponentId = blackQueue.poll();
                createGame(playerId, opponentId, level);
                return _gameId;
            } else if (!whiteQueue.contains(playerId)){
                whiteQueue.add(playerId);
            }
        } else if (colour == -1) {
            if (!whiteQueue.isEmpty()) {
                int opponentId = whiteQueue.poll();
                createGame(playerId, opponentId, level);
                return _gameId;
            } else if (!blackQueue.contains(playerId)) {
                blackQueue.add(playerId);
            }
        }
        return -1;
    }

    private void createGame(int playerId, int opponentId, int level) {
        _gameId++;
        System.out.println("Creating game between " + playerId + " and " + opponentId);
        Game game = new Game(_gameId, playerId, opponentId, level);
        gameById.put(_gameId, game);
        gameByPlayerId.put(playerId, _gameId);
        gameByPlayerId.put(opponentId, _gameId);
        game.whiteTimer.start();
    }

    public void deleteGame(int id) {
        if (gameById.containsKey(id)) {
            System.out.println("Deleting game with id: " + id);
            Game game = gameById.remove(id);
            gameByPlayerId.remove(game.playerId);
            gameByPlayerId.remove(game.opponentId);
        }
    }

    public Move getBestMove(int id, int colour) {
        if (gameById.containsKey(id)) {
            Game game = gameById.get(id);
            Move bestMove = game.getBestMove(colour);
            if(gameById.containsKey(id)) { // double check if the game is still active after computing the move
                return bestMove;
            } else {
                return null;
            }
        } else {
            System.out.println("Getting best move: Game with id: " + id + " does not exist");
            return null;
        }
    }

    public List<Move> getAllMoves(int id, int colour) {
        if (gameById.containsKey(id)) {
            return gameById.get(id).getAllMoves(colour);
        } else {
            System.out.println("Getting all moves: Game with id: " + id + " does not exist");
            return new ArrayList<>();
        }
    }

    public int makeMove(int id, Move move) {
        if (gameById.containsKey(id)) {
            Game game = gameById.get(id);
            game.getBoard().makeMove(move, false);
            move.gameResult = getGameResult(id, -move.colour);
            game.lastMove = move;
            game.updateTimer(move.colour);
            return move.gameResult;
        } else {
            System.out.println("Making move: Game with id: " + id + " does not exist");
            return 0;
        }
    }

    public int getGameResult(int id, int colour) {
        if (gameById.containsKey(id)) {
            return gameById.get(id).getBoard().getGameResult(colour);
        } else {
            System.out.println("Getting game result: Game with id: " + id + " does not exist");
        }
        return 0;
    }

    public GameState getGameUpdate(int gameId) {
        if (gameById.containsKey(gameId)) {
            Game game = gameById.get(gameId);
            Move lastMove = game.lastMove;
            int whiteTime = game.whiteTimer.getSecondsLeft();
            int blackTime = game.blackTimer.getSecondsLeft();
            return new GameState(true, lastMove, whiteTime, blackTime);
        } else {
            System.out.println("Getting game update: Game with id: " + gameId + " does not exist");
            Move move = new Move(0, 0, 0, 0);
            return new GameState(false, move, 0, 0);
        }
    }

    public void cancelSearch(int playerId) {
        whiteQueue.remove(playerId);
        blackQueue.remove(playerId);
    }

    public void resetGame(int id) {
        if (gameById.containsKey(id)) {
            gameById.get(id).reset();
        } else {
            System.out.println("Resetting game: Game with id: " + id + " does not exist");
        }
    }
}
