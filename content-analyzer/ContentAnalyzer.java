import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class ContentAnalyzer {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Smart Content Analyzer (Java Client) ===");

        System.out.println("Choose an option:\n[1] Analyze Text\n[2] Get Suggestions");
        System.out.print("Enter choice (1 or 2): ");
        String choice = scanner.nextLine().trim();
        if (!choice.equals("1") && !choice.equals("2")) {
            System.out.println("Invalid choice. Exiting.");
            scanner.close();
            return;
        }

        System.out.println("\nEnter text (press Enter twice to submit):");
        StringBuilder textBuilder = new StringBuilder();
        String line;
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (line.isEmpty()) { break; }
            textBuilder.append(line).append(" ");
        }

        String text = textBuilder.toString().trim();
        if (text.isEmpty()) {
            System.out.println("No text provided. Exiting.");
            scanner.close();
            return;
        }

        String endpoint = choice.equals("1") ? "/analyze" : "/suggestions";

        String jsonBody = "{\"text\": \"" + text.replace("\"", "\\\"") + "\"}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:5000" + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            System.out.println();

            if (endpoint.equals("/analyze")) {
                System.out.println("=== Analysis Results ===");
                System.out.println();

                String polarity = extractValue(body, "sentiment_polarity");
                String subjectivity = extractValue(body, "sentiment_subjectivity");
                String readingEase = extractValue(body, "flesch_reading_ease");
                String gradeLevel = extractValue(body, "grade_level");
                String wordCount = extractValue(body, "word_count");
                String difficultWords = extractValue(body, "difficult_words");

                double polarityNum = 0.0;
                try {
                    polarityNum = Double.parseDouble(polarity);
                } catch (NumberFormatException e) {
                    // keep polarityNum as 0.0
                }

                String sentimentLabel;
                if (polarityNum > 0.1) { sentimentLabel = "Positive"; }
                else if (polarityNum < -0.1) { sentimentLabel = "Negative"; }
                else { sentimentLabel = "Neutral"; }

                System.out.println("Sentiment:        " + sentimentLabel + " (" + polarity + ")");
                System.out.println("Subjectivity:     " + subjectivity);
                System.out.println("Reading Ease:     " + readingEase + " / 100");
                System.out.println("Grade Level:      " + gradeLevel);
                System.out.println("Word Count:       " + wordCount);
                System.out.println("Difficult Words:  " + difficultWords);
            } else {
                System.out.println("=== Suggestions ===");
                String[] tips = extractArray(body, "suggestions");
                if (tips == null || tips.length == 0) {
                    System.out.println("No suggestions returned.");
                } else {
                    for (int i = 0; i < tips.length; i++) {
                        System.out.println((i + 1) + ". " + tips[i]);
                    }
                }
            }
        } else {
            System.out.println("Error: Server returned status " + response.statusCode());
            System.out.println(response.body());
        }

        scanner.close();
    }

    private static String extractValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return "N/A";
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "N/A";
        int endIndex = json.indexOf(",", colonIndex);
        if (endIndex == -1) { endIndex = json.indexOf("}", colonIndex); }
        if (endIndex == -1) endIndex = json.length();
        String value = json.substring(colonIndex + 1, endIndex).trim();
        // Strip surrounding quotes if present
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String[] extractArray(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return new String[0];
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return new String[0];
        int startBracket = json.indexOf("[", colonIndex);
        int endBracket = json.indexOf("]", startBracket);
        if (startBracket == -1 || endBracket == -1) return new String[0];
        String arrayBody = json.substring(startBracket + 1, endBracket).trim();
        if (arrayBody.isEmpty()) return new String[0];
        java.util.List<String> items = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (c == '"') { inQuotes = !inQuotes; continue; }
            if (c == ',' && !inQuotes) {
                String item = cur.toString().trim();
                if (item.startsWith("\"") && item.endsWith("\"")) item = item.substring(1, item.length()-1);
                items.add(item);
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        String last = cur.toString().trim();
        if (!last.isEmpty()) {
            if (last.startsWith("\"") && last.endsWith("\"")) last = last.substring(1, last.length()-1);
            items.add(last);
        }
        return items.toArray(new String[0]);
    }
}