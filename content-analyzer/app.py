from flask import Flask, request, jsonify
from flask_cors import CORS
from textblob import TextBlob
import textstat

app = Flask(__name__)
CORS(app)


@app.route("/analyze", methods=["POST"])
def analyze():
    data = request.get_json()
    text = data.get("text", "")

    if not text.strip():
        return jsonify({"error": "No text provided"}), 400
    blob = TextBlob(text)
    sentiment_polarity = round(blob.sentiment.polarity, 3)
    sentiment_subjectivity = round(blob.sentiment.subjectivity, 3)

    flesch_score = textstat.flesch_reading_ease(text)
    grade_level = textstat.flesch_kincaid_grade(text)
    word_count = len(text.split())
    difficult_words = textstat.difficult_words(text)
    
    return jsonify({
        "sentiment_polarity": sentiment_polarity,
        "sentiment_subjectivity": sentiment_subjectivity,
        "flesch_reading_ease": flesch_score,
        "grade_level": grade_level,
        "word_count": word_count,
        "difficult_words": difficult_words
    })

@app.route("/suggestions", methods=["POST"])
def suggestions():
    data = request.get_json()
    text = data.get("text", "")
    if not text.strip():
        return jsonify({"error": "No text provided"}), 400

    blob = TextBlob(text)
    sentiment_polarity = blob.sentiment.polarity
    sentiment_subjectivity = blob.sentiment.subjectivity

    flesch_score = textstat.flesch_reading_ease(text)
    grade_level = textstat.flesch_kincaid_grade(text)
    word_count = len(text.split())
    difficult_words = textstat.difficult_words(text)

    tips = []

    # Sentiment suggestions
    if sentiment_polarity < -0.2:
        tips.append("Tone: The text reads as negative — consider softening criticism and using constructive phrasing.")
    elif sentiment_polarity > 0.6:
        tips.append("Tone: The text is strongly positive — ensure balance and avoid exaggeration if objectivity is required.")
    else:
        tips.append("Tone: The sentiment is neutral to mildly charged; this is usually appropriate for informative content.")

    # Subjectivity suggestions
    if sentiment_subjectivity > 0.6:
        tips.append("Objectivity: High subjectivity detected — add facts, citations, or reduce opinionated language for technical content.")
    elif sentiment_subjectivity < 0.2:
        tips.append("Objectivity: Very objective — if making recommendations, consider adding appropriate context or caveats.")

    # Readability suggestions
    if flesch_score < 50:
        tips.append("Readability: Low (difficult to read) — shorten sentences, use simpler words, and break paragraphs.")
    elif flesch_score < 70:
        tips.append("Readability: Moderate — consider simplifying complex sentences to improve clarity for broader audiences.")
    else:
        tips.append("Readability: Good — accessible to a wide audience.")

    # Grade level
    if grade_level > 12:
        tips.append("Complexity: Writing is at a higher education level — lower the grade level for general audiences or provide summaries.")
    elif grade_level < 6:
        tips.append("Complexity: Very simple language detected — ensure it fits the intended audience and completeness of ideas.")

    # Word count suggestions
    if word_count < 50:
        tips.append("Length: Very short — expand with examples, explanations, or context if the topic requires it.")
    elif word_count > 1200:
        tips.append("Length: Very long — consider splitting into sections or separate articles and add a short summary.")

    # Difficult words
    if difficult_words > 20:
        tips.append("Vocabulary: Many difficult words — replace jargon with simpler terms or add definitions.")
    elif difficult_words > 5:
        tips.append("Vocabulary: Some difficult words present — consider simplifying key sentences.")

    return jsonify({"suggestions": tips})


if __name__ == "__main__":
    app.run(debug=True)