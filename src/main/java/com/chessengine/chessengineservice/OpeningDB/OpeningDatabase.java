package com.chessengine.chessengineservice.OpeningDB;

import com.chessengine.chessengineservice.Helpers.PGNHelper;
import com.chessengine.chessengineservice.Structures.Move;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpeningDatabase {
    String[] openingBook;
    public OpeningDatabase() {
        this.openingBook = new String[30];
        loadFromFile();
        shuffleOpeningBook();
    }

    public Move getNextMove(String pgn, List<Move> allMoves) {
        for (String opening : openingBook) {
            if (opening.startsWith(pgn)) {
                String nextMove = opening.substring(pgn.length()).split(" ")[0];
                return PGNHelper.convertPGNToMove(nextMove, allMoves);
            }
        }
        return null;
    }

    public void shuffleOpeningBook() {
        List<String> openingBookList = Arrays.asList(openingBook);
        Collections.shuffle(openingBookList);
        openingBook = openingBookList.toArray(new String[0]);
    }

    private void loadFromFile() {
        try {
            ClassLoader classLoader = OpeningDatabase.class.getClassLoader();

            String resourcePath = "OpeningsBook.txt";
            InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                Scanner scanner = new Scanner(inputStream);
                int i = 0;
                while (scanner.hasNextLine()) {
                    openingBook[i] = "";
                    String gamePGN = scanner.nextLine();
                    Pattern pattern = Pattern.compile("\\d+\\.\\s(.*?)(?=\\d+\\.|$)");
                    Matcher matcher = pattern.matcher(gamePGN);
                    while (matcher.find()) {
                        openingBook[i] += matcher.group(1);
                    }
                    i++;
                }
                scanner.close();
            } else {
                throw new Exception("Resource not found");
            }
        } catch (Exception e) {
            System.out.println("Error while loading opening book database: " + e);
        }
    }
}
