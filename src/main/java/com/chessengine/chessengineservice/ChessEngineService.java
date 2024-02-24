package com.chessengine.chessengineservice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChessEngineService {
    Map<Integer, Game> gameById;
    Map<Integer, Integer> gameByPlayerId;
    Queue<Integer> whiteQueue;
    Queue<Integer> blackQueue;
    static int _gameId = 0;
    static int _playerID = 0;


    public ChessEngineService() {
        gameById = new ConcurrentHashMap<>();
        gameById.put(0, new Game(0, 0, 0));
        gameByPlayerId = new ConcurrentHashMap<>();
        whiteQueue = new ConcurrentLinkedQueue<>();
        blackQueue = new ConcurrentLinkedQueue<>();
    }

    public int generatePlayerId() {
        _playerID++;
        return _playerID;
    }

    public int createGameOrJoinQueue(int colour, int playerId) {
        if(gameByPlayerId.containsKey(playerId)) {
            return gameByPlayerId.get(playerId);
        }
        if(colour == 1) {
            if (!blackQueue.isEmpty()) {
                int opponentId = blackQueue.poll();
                createGame(playerId, opponentId);
                return _gameId;
            } else if(!whiteQueue.contains(playerId)){
                whiteQueue.add(playerId);
            }
        } else if(colour == -1) {
            if (!whiteQueue.isEmpty()) {
                int opponentId = whiteQueue.poll();
                createGame(playerId, opponentId);
                return _gameId;
            } else if (!blackQueue.contains(playerId)) {
                blackQueue.add(playerId);
            }
        }
        return -1;
    }

    private void createGame(int playerId, int opponentId) {
        _gameId++;
        Game game = new Game(_gameId, playerId, opponentId);
        gameById.put(_gameId, game);
        gameByPlayerId.put(playerId, _gameId);
        gameByPlayerId.put(opponentId, _gameId);
    }

    public void deleteGame(int id) {
        if(gameById.containsKey(id)) {
            Game game = gameById.remove(id);
            gameByPlayerId.remove(game.playerId);
            gameByPlayerId.remove(game.opponentId);
        }
    }

    public Move getLastMove(int gameId) {
        if(gameById.containsKey(gameId)) {
            return gameById.get(gameId).lastMove;
        } else {
            System.out.println("Getting last move: Game with id: " + gameId + " does not exist");
            return new Move(0, 0, 0, 0);
        }

    }

    public Move getBestMove(int id, int colour) {
        if (gameById.containsKey(id)) {
           return gameById.get(id).getBestMove(colour);
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

    public void resetGame(int id) {
        if (gameById.containsKey(id)) {
            gameById.get(id).reset();
        } else {
            System.out.println("Resetting game: Game with id: " + id + " does not exist");
        }
    }
}
