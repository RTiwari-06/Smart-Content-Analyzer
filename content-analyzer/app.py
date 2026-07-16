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

if __name__ == "__main__":
    app.run(debug=True)