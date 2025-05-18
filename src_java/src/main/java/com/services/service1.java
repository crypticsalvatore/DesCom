// src/main/java/com/example/DayService.java
package com.example;

import io.javalin.Javalin;
import io.javalin.http.Context; // Import Context for type hinting
import io.javalin.http.HttpStatus;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// OOP: Create a dedicated class for request handling if logic becomes complex.
// For now, we'll keep it in DayService but use a separate DayCalculator class.

/**
 * Represents the main service application that handles HTTP requests.
 * It uses a DayCalculator instance to perform the core business logic.
 */
public class DayService {

    private static final Logger logger = LoggerFactory.getLogger(DayService.class);
    private final DayCalculator dayCalculator; // OOP: Instance of the calculator

    public DayService(DayCalculator dayCalculator) {
        this.dayCalculator = dayCalculator;
    }

    /**
     * Handles the GET request to /api/get-day.
     * It extracts the number, uses the DayCalculator to get the day name,
     * and sends the response.
     * @param ctx The Javalin context for the HTTP request/response.
     */
    public void getDayHandler(Context ctx) {
        String numberStr = ctx.queryParam("number");

        if (numberStr == null) {
            logger.warn("Missing 'number' query parameter");
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Missing 'number' query parameter"));
            return;
        }

        try {
            double receivedNumberDouble = Double.parseDouble(numberStr);
            
            // Delegate calculation to the DayCalculator instance
            String dayName = dayCalculator.calculateDayOfWeek(receivedNumberDouble);
            
            logger.info("Received number: {}, Calculated Day Name: {}", 
                        receivedNumberDouble, dayName);
            
            ctx.status(HttpStatus.OK).json(Map.of("input_number", receivedNumberDouble, "day", dayName));

        } catch (NumberFormatException e) {
            logger.warn("Invalid number format for 'number' parameter: {}", numberStr, e);
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid number format for 'number' parameter"));
        } catch (IllegalArgumentException e) { // Catch specific exception from DayCalculator
            logger.warn("Error in day calculation logic: {}", e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        } catch (Exception e) { // Catch-all for other unexpected errors
            logger.error("An unexpected error occurred in Java service", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", "An internal server error occurred in Java service"));
        }
    }

    public static void main(String[] args) {
        // OOP: Instantiate the calculator
        DayCalculator calculator = new DayCalculator();
        // OOP: Instantiate the service with its dependency
        DayService dayServiceApp = new DayService(calculator);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.requestLogger.http((ctx, ms) -> {
                logger.info("{} {} on port {} took {} ms", ctx.method(), ctx.path(), ctx.port(), ms);
            });
            // OOP: Add more configurations if needed, e.g., exception mapping
            config.router.addHttpHandler(io.javalin.http.HandlerType.GET, "/api/get-day", dayServiceApp::getDayHandler);

        }).start(7001); // Runs on port 7001

        logger.info("Java DayService (OOP version) started on port 7001");

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }
}

/**
 * OOP: Encapsulates the logic for calculating the day of the week.
 * This class is responsible for the core business logic related to day calculation.
 */
class DayCalculator {
    // This could be an instance field if the days could change per instance,
    // but static final is appropriate for a constant like this.
    private static final String[] DAYS_OF_WEEK = {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    /**
     * Calculates the day of the week based on the input number.
     * The input number is first cast to an integer, then divided by 7.
     * The result of this division (integer) modulo 7 is used as an index
     * to pick a day from the DAYS_OF_WEEK array.
     *
     * @param number The input number (sum from the Python service).
     * @return The name of the calculated day of the week.
     * @throws IllegalArgumentException if the internal day index calculation leads to an error (should be rare with modulo).
     */
    public String calculateDayOfWeek(double number) {
        // The prompt asks to "divide the number by seven into an integer type"
        // and then map that result.
        
        int userChoiceAsInt = (int) number; // Truncate if it was a float from sum
        
        // "divides the number by seven into an integer type"
        int divisionResult = userChoiceAsInt / 7; 
        
        // Use the result of the division to map to a day, ensuring it's within 0-6
        int dayIndex = divisionResult % 7; 
        
        // Handle negative results of modulo if the divisionResult could be negative
        if (dayIndex < 0) {
            dayIndex += 7; 
        }

        if (dayIndex >= 0 && dayIndex < DAYS_OF_WEEK.length) {
            return DAYS_OF_WEEK[dayIndex];
        } else {
            // This case should ideally not be reached if modulo logic is correct
            throw new IllegalArgumentException("Calculated day index is out of bounds: " + dayIndex);
        }
    }
}
