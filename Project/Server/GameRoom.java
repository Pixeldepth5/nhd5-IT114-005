// UCID: nhd5
// Date: December 3, 2025
// Description: TriviaGuessGame Server – Fetches trivia questions based on player-selected categories.
// Reference: Open Trivia Database API

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class GameRoom extends Room {
    private static final String TRIVIA_API_URL = "https://opentdb.com/api.php?amount=5&category=%d&type=multiple"; // API URL for trivia questions
    private static final Map<String, Integer> CATEGORY_MAP = Map.of(
        "Music", 12,
        "Sports", 21,
        "Arts", 25,
        "Movies", 11,
        "History", 23,
        "Geography", 22
    );
    
    // Method to fetch trivia questions based on category
    private List<String> fetchQuestions(String category) {
        try {
            int categoryId = CATEGORY_MAP.getOrDefault(category, 11); // Default to "Movies"
            String apiUrl = String.format(TRIVIA_API_URL, categoryId);
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            return parseTriviaQuestions(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.singletonList("Error fetching questions.");
        }
    }

    // Parse the JSON response to extract questions
    private List<String> parseTriviaQuestions(String json) {
        List<String> questions = new ArrayList<>();
        String pattern = "\"question\":\"(.*?)\""; // Regex to extract question text
        Matcher matcher = Pattern.compile(pattern).matcher(json);
        while (matcher.find()) {
            questions.add(matcher.group(1));  // Add each question to list
        }
        return questions;
    }

    // Start the round with the selected category
    private void startRoundWithCategory(String category) {
        List<String> questions = fetchQuestions(category);
        if (questions.isEmpty()) {
            broadcast(null, "Error fetching questions.");
            return;
        }
        broadcast(null, "Round starting with category: " + category);
        for (String question : questions) {
            broadcast(null, "Question: " + question);
        }
    }
    
    // Handle when a player selects a category
    public void playerSelectedCategory(String category) {
        startRoundWithCategory(category);
    }
}
