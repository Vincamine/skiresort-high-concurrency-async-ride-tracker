import beans.LiftRide;
import beans.LiftRideMessage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/*")
public class Servlet extends HttpServlet {
    private Connection connection;
    private RMQChannelPool channelPool;
    private Gson gson = new Gson();
    private JedisPool jedisPool;

    /**
     * Initialize RabbitMQ
     *
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
        try {
            // Initialize RMQ
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(Config.RABBITMQ_HOST);
            factory.setUsername(Config.RABBITMQ_USERNAME);
            factory.setPassword(Config.RABBITMQ_PASSWORD);
            connection = factory.newConnection();
            channelPool = new RMQChannelPool(Config.CHANNEL_POOL_SIZE, new RMQChannelFactory(connection));

            // Initialize Redis
            jedisPool = new JedisPool(Config.REDIS_HOST, Config.REDIS_PORT); // Replace with Redis host and port
        } catch (Exception e) {
            throw new ServletException("Failed to initialize RabbitMQ or Redis", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LiftRideMessage liftRideMessage = processPostRequest(request, response);

        if (liftRideMessage == null) {
            return;
        }

        try {
            Channel channel = channelPool.borrowObject();
            channel.queueDeclare("assignment2_queue", false, false, false, null);
            String message = gson.toJson(liftRideMessage);
            channel.basicPublish("", "assignment2_queue", null, message.getBytes());

            channelPool.returnObject(channel);
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"message\": \"Lift ride data successfully processed\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Failed to process lift ride data\"}");
        }
    }

    /**
     * Handle GET requests
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processGetRequest(request, response);
    }

    @Override
    public void destroy() {
        try {
            if (channelPool != null) channelPool.close();
            if (connection != null) connection.close();
            if (jedisPool != null) jedisPool.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LiftRideMessage processPostRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");

        //1. Check request
        String urlPath = request.getPathInfo();
        System.out.println("URL Path: " + urlPath);

        //1.1 Check null or empty
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"message\": \"Missing parameters\"}");
            return null;
        }

        //1.2 Validate URL format
        String[] urlParts = urlPath.split("/");
        if (!isPostUrlValid(urlParts)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Invalid URL format\"}");
            return null;
        }

        Integer resortID = Integer.parseInt(urlParts[2]);
        Integer seasonID = Integer.parseInt(urlParts[4]);
        Integer dayID = Integer.parseInt(urlParts[6]);
        Integer skierID = Integer.parseInt(urlParts[8]);

        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            LiftRide liftRide = gson.fromJson(sb.toString(), LiftRide.class);
            if (liftRide == null || liftRide.getTime() == null || liftRide.getLiftID() == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid lift ride data\"}");
                return null;
            }
            return new LiftRideMessage(resortID, seasonID, dayID, skierID, liftRide);
        } catch (JsonSyntaxException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Malformed JSON: " + e.getMessage() + "\"}");
            return null;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Unexpected error: " + e.getMessage() + "\"}");
            return null;
        }
    }


    private boolean isPostUrlValid(String[] urlPath) {
        // urlPath  = "skiers/12/seasons/2019/days/1/skiers/110"
        // urlParts = [skiers, 12, seasons, 2019, days, 1, skiers, 110]
        if (urlPath.length != 9) {
            return false;
        }

        if (!urlPath[1].equals("skiers") ||
                !urlPath[3].equals("seasons") ||
                !urlPath[5].equals("days") ||
                !urlPath[7].equals("skiers")) {
            return false;
        }

        try {
            Integer.parseInt(urlPath[2]); // resortID
            Integer.parseInt(urlPath[4]); // seasonID
            Integer.parseInt(urlPath[6]); // dayID
            Integer.parseInt(urlPath[8]); // skierID
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    private void processGetRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        //1. Check request
        String urlPath = request.getPathInfo();
        System.out.println("URL Path: " + urlPath);

        //1.1 Check null or empty
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"message\": \"Missing parameters\"}");
            return;
        }

        //1.2 Validate URL format
        String[] urlParts = urlPath.split("/");
        try {
            if (urlParts.length == 9) {
                // Handle GET /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
                processSkierDayVertical(request, response, urlParts);
            } else if (urlParts.length == 8) {
                //Handle GET /resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
                processResortDayVertical(request, response, urlParts);
            } else if (urlParts.length == 4) {
                // Handle GET /skiers/{skierID}/vertical
                processSkierTotalVertical(request, response, urlParts);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid URL length\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Failed to process request\"}");
        }

    }

    /**
     * Handle GET /resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
     * Get number of unique skiers at resort/season/day
     */
    private void processResortDayVertical(HttpServletRequest request, HttpServletResponse response, String[] urlParts) throws IOException {
        try {
            //Validation
            if (!urlParts[1].equals("resorts") || !urlParts[3].equals("seasons") || !urlParts[5].equals("days") || !urlParts[7].equals("skiers")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid URL format\"}");
                return;
            }

            try (Jedis jedis = jedisPool.getResource()) {
                int resortID = Integer.parseInt(urlParts[2]);
                int seasonID = Integer.parseInt(urlParts[4]);
                int dayID = Integer.parseInt(urlParts[6]);

                String key = "resort:" + resortID + ":season:" + seasonID + ":day:" + dayID + ":skiers";
                System.out.println("key:" + key);
                long uniqueSkiersCount = jedis.scard(key);

                if (uniqueSkiersCount > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"uniqueSkiers\": " + uniqueSkiersCount + "}");
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("Data not found");
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid URL numeric\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Server error occurred: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Handle GET /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
     * Get ski day vertical for a skier
     */
    private void processSkierDayVertical(HttpServletRequest request, HttpServletResponse response, String[] urlParts)
            throws IOException {
        try {
            if (!urlParts[1].equals("skiers") || !urlParts[3].equals("seasons") || !urlParts[5].equals("days") || !urlParts[7].equals("skiers")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid URL format\"}");
                return;
            }

            try (Jedis jedis = jedisPool.getResource()) {
                int resortID = Integer.parseInt(urlParts[2]);
                int seasonID = Integer.parseInt(urlParts[4]);
                int dayID = Integer.parseInt(urlParts[6]);
                int skierID = Integer.parseInt(urlParts[8]);

                String key = "resort:" + resortID + ":season:" + seasonID + ":day:" + dayID + ":skier:" + skierID;
                System.out.println("key:" + key);
                String dayVertical = jedis.hget(key, "vertical");

                if (dayVertical != null) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(dayVertical);
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("Data not found");
                }

            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid URL numeric\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Server error occurred: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Handle GET /skiers/{skierID}/vertical
     * Get the total vertical for the skier for specified seasons at the specified resort
     */
    private void processSkierTotalVertical(HttpServletRequest request, HttpServletResponse response, String[] urlParts)
            throws IOException {
        try {
            if (!urlParts[1].equals("skiers") || !urlParts[3].equals("vertical")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid URL format\"}");
                return;
            }

            int skierID;
            try {
                skierID = Integer.parseInt(urlParts[2]);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid skierID: must be a valid integer\"}");
                return;
            }

            String resort = request.getParameter("resort"); // Required
            String season = request.getParameter("season"); // Optional

            if (resort == null || resort.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid input: 'resort' is required\"}");
                return;
            }

            if (season != null && !season.matches("\\d{4}")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\": \"Invalid season format: must be a 4-digit year\"}");
                return;
            }

            String totalVerticalKey = "resort:" + resort + ":skier:" + skierID + ":vertical";
            System.out.println("key:" + totalVerticalKey);
            String totalVertical;
            try (Jedis jedis = RedisClient.getPool().getResource()) {
                totalVertical = (season == null)
                        ? jedis.hget(totalVerticalKey, "all")
                        : jedis.hget(totalVerticalKey, season);
            }

            if (totalVertical == null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\": \"Data not found\"}");
                return;
            }
            response.setStatus(HttpServletResponse.SC_OK);
            String jsonResponse = season == null
                    ? String.format("{\"resort\": \"%s\", \"totalVert\": %s}", resort, totalVertical)
                    : String.format("{\"seasonID\": \"%s\", \"totalVert\": %s}", season, totalVertical);
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Server error occurred: " + e.getMessage() + "\"}");
        }
    }
}


