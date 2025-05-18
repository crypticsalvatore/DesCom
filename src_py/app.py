from flask import Flask, request, jsonify
import requests # Used to make HTTP requests to the Java service
from flask_cors import CORS # For handling Cross-Origin Resource Sharing if frontend and backend are on different "origins" during development

app = Flask(__name__)
CORS(app) # Enable CORS for all routes

# Configuration for the Java service
JAVA_SERVICE_URL = "http://localhost:7001/api/get-day" # Make sure this matches your Java service's address

@app.route('/api/add-and-get-day', methods=['GET'])
def add_and_get_day_route():
    try:
        num1_str = request.args.get('num1')
        num2_str = request.args.get('num2')

        if num1_str is None or num2_str is None:
            return jsonify({"error": "Missing 'num1' or 'num2' parameter"}), 400

        try:
            num1 = float(num1_str)
            num2 = float(num2_str)
        except ValueError:
            return jsonify({"error": "Invalid number format for 'num1' or 'num2'"}), 400

        # Add the numbers
        the_sum = num1 + num2
        app.logger.info(f"Calculated sum: {the_sum}")

        # Call the Java service to get the day of the week
        try:
            # The sum is the "user choice" for the Java service
            java_response = requests.get(f"{JAVA_SERVICE_URL}?number={the_sum}", timeout=5) # 5 second timeout
            java_response.raise_for_status()  # Raise an exception for HTTP errors (4xx or 5xx)
            
            java_data = java_response.json()
            day_from_java = java_data.get("day")

            if not day_from_java:
                app.logger.error("Java service did not return a 'day'. Response: %s", java_data)
                return jsonify({"error": "Failed to get day from Java service - missing 'day' field"}), 500
            
            app.logger.info(f"Day received from Java service: {day_from_java}")
            return jsonify({"input_sum": the_sum, "day_of_the_week": day_from_java}), 200

        except requests.exceptions.Timeout:
            app.logger.error(f"Timeout when calling Java service at {JAVA_SERVICE_URL}")
            return jsonify({"error": "Java service timed out"}), 504 # Gateway Timeout
        except requests.exceptions.ConnectionError:
            app.logger.error(f"Could not connect to Java service at {JAVA_SERVICE_URL}")
            return jsonify({"error": "Could not connect to Java service"}), 503 # Service Unavailable
        except requests.exceptions.RequestException as e:
            app.logger.error(f"Error calling Java service: {e}")
            # Try to get more specific error from Java if possible
            error_detail = str(e)
            if e.response is not None:
                try:
                    error_detail = e.response.json().get("error", str(e))
                except ValueError: # Not JSON
                    error_detail = e.response.text
            return jsonify({"error": f"Failed to get day from Java service: {error_detail}"}), 502 # Bad Gateway
        except ValueError as e: # JSON decoding error from Java response
            app.logger.error(f"Could not decode JSON response from Java service: {e}")
            return jsonify({"error": "Invalid JSON response from Java service"}), 500


    except Exception as e:
        app.logger.error(f"An unexpected error occurred: {e}")
        return jsonify({"error": "An internal server error occurred in Python service"}), 500

if __name__ == '__main__':
    # It's good practice to enable logging for development
    import logging
    logging.basicConfig(level=logging.INFO)
    app.run(host='0.0.0.0', port=5001, debug=True) # Runs on port 5001
```
**To run this Python service:**
1.  Save it as `app.py`.
2.  Make sure you have Flask and requests installed:
    `pip install Flask requests flask-cors`
3.  Run it from your terminal: `python app.