package com.tetrisbot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.security.auth.login.LoginException;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

/**
 * This class handles the Discord API and runs the game simulation using inputs
 * received from Discord users.
 */

public class Bot extends ListenerAdapter {
    static HashMap<Long, Game> games;
    static final String token = ""; //If testing on your own, use your own bot token
    static final long botId = 1377027446783610890L; //replace with your bot's ID
    static final String gitToken = ""; //use your GitHub token
    static final boolean DEBUG = false;
    static final String HELP =
    "Tetris Bot commands:" + "\n" +
    "start: starts a new game in a channel." + "\n" +
    "abort: aborts the current game in a channel." + "\n" +
    "[input]: plays the input, if valid. You can also enter any unambiguous prefix of the input, such as \"l\" for LEFT or \"ha\" for HARDDROP."+ "\n" +
    "[input] -: repeats the input until it is no longer valid. Only applies to LEFT, RIGHT, and SOFTDROP."+ "\n" +
    "You may send a command by using the \"!tetris\" prefix or by replying to any bot message in the same channel.";

    static class Game {
        Tetris tetris;
        String owner;
        long lastUserId;
        //List<String> users;
        List<BufferedImage> frames;
        public Game(String o) {
            tetris = new Tetris();
            owner = o;
            lastUserId = -1;
            frames = new ArrayList<>();
        }

        /**
         * Converts the piece and input sequence of a game into a replay and
         * stores it in GitHub.
         */
        public String saveReplay() {
            if (tetris.inputs.size() < 10) {
                return "short";
            }
            String id = owner + "-" + System.currentTimeMillis();
            File gif = new File(id + "-replay.gif");
            try {
                FileImageOutputStream output = new FileImageOutputStream(gif);
                GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_RGB, 500, true);
                for (int i = 0; i < frames.size(); i++) {
                    writer.writeToSequence(frames.get(i));
                }
                output.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            String apiURL = "https://api.github.com/repos/derrick-x/Tetris-Replays/contents/replays/" + id + "-replay.gif";
            byte[] fileBytes = new byte[(int) gif.length()];
            try (FileInputStream inputStream = new FileInputStream(gif)) {
                inputStream.read(fileBytes);
            }
            catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            String encodedContent = Base64.getEncoder().encodeToString(fileBytes);
            String jsonPayload = "{"
                + "\"message\": \"Upload Tetris replay\","
                + "\"content\": \"" + encodedContent + "\""
                + "}";
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(apiURL).openConnection();
                conn.setRequestMethod("PUT");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + gitToken);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("Content-Type", "application/json");
                OutputStream os = conn.getOutputStream();
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                int code = conn.getResponseCode();
                System.out.println("GitHub upload response: " + code);
            } catch (IOException e) {
                e.printStackTrace();
            }
            gif.delete();
            return id;
        }
    }
    
    public static void main(String[] args) throws LoginException {
        System.setProperty("java.awt.headless", "true");
        JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .enableIntents(GatewayIntent.GUILD_PRESENCES)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .addEventListeners(new Bot())
            .build();
        games = new HashMap<>();
                
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMember() == null || event.getAuthor().isBot()) {
            return;
        }
        String[] args;
        if (event.getMessage().getReferencedMessage() != null && event.getMessage().getReferencedMessage().getAuthor().getIdLong() == 1377027446783610890L) {
            args = event.getMessage().getContentRaw().split(" ");
        } else if (event.getMessage().getContentRaw().length() > 7 && event.getMessage().getContentRaw().substring(0, 8).equals("!tetris ")) {
            args = event.getMessage().getContentRaw().substring(8).split(" ");
        } else {
            return;
        }
        Game game = games.get(event.getChannel().getIdLong());
        if (args[0].length() == 0) {
            return;
        }
        if (args[0].equals("start")) {
            if (game == null) {
                event.getChannel().sendMessage("Starting game by " + event.getAuthor().getName() + "...").queue();
                games.put(event.getChannel().getIdLong(), new Game(event.getAuthor().getName()));
                sendTetris(event.getChannel(), games.get(event.getChannel().getIdLong()), null, null);
            }
            else {
                event.getChannel().sendMessage("Game already in progress!").queue();
            }
        }
        if (args[0].equals("abort")) {
            if (game == null) {
                event.getChannel().sendMessage("No game in progress!").queue();
            }
            else if (DEBUG || event.getAuthor().getName().equals(game.owner)) {
                saveReplay(event, game);
                games.remove(event.getChannel().getIdLong());
                event.getChannel().sendMessage("Game aborted").queue();
            }
            else {
                event.getChannel().sendMessage("Only " + game.owner + " can abort the game!").queue();
            }
        }
        if (args[0].equals("replay")) {
            if (args.length < 2) {
                event.getChannel().sendMessage("Please specify a replay id!").queue();
            }
            else {
                event.getChannel().sendMessage("Fetching replays containing key " + args[1] + "...").queue();
                List<String> replays = getReplays(args[1]);
                StringBuilder msg = new StringBuilder();
                msg.append(replays.size());
                msg.append(" replay");
                if (replays.size() != 1) {
                    msg.append("s");
                }
                msg.append(" found.");
                if (replays.size() > 10) {
                    msg.append(" Only displaying first 10.");
                }
                msg.append("\n");
                for (int i = 0; i < Math.min(replays.size(), 10); i++) {
                    msg.append(replays.get(i));
                    msg.append("\n");
                }
                event.getChannel().sendMessage(msg.toString()).queue();
            }
        }
        if (args[0].equals("help")) {
            event.getChannel().sendMessage(HELP).queue();
        }
        Tetris.Input input = stringToInput(args[0]);
        if (input != null) {
            if (game == null) {
                event.getChannel().sendMessage("No game in progress!").queue();
            }
            else if (game.tetris.getValidMoves().contains(input)) {
                if (DEBUG || event.getAuthor().getIdLong() != game.lastUserId) {
                    game.lastUserId = event.getAuthor().getIdLong();
                    if (args.length > 1 && input != Tetris.Input.CW && input != Tetris.Input.CCW && input != Tetris.Input.HARDDROP && args[1].equals("-")) {
                        while (game.tetris.getValidMoves().contains(input)) {
                            game.tetris.input(input);
                        }
                    }
                    else {
                        game.tetris.input(input);
                    }
                    sendTetris(event.getChannel(), game, event.getMember().getEffectiveName(), input);
                    if (game.tetris.lines >= 300 || !game.tetris.alive) {
                        saveReplay(event, game);
                    }
                }
                else {
                    event.getChannel().sendMessage(event.getMember().getEffectiveName() + ", you already played the last move!").queue();
                }
            }
            else {
                event.getChannel().sendMessage(input + " is not a valid move!").queue();
            }
        }
    }

    /**
     * Sends a message using emojis to display a Tetris game.
     * @param channel The channel to send the message.
     * @param game The Tetris.java instance in to display.
     */
    public void sendTetris(MessageChannelUnion channel, Game game, String user, Tetris.Input input) {
        BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        paintGame(image.getGraphics(), game.tetris, user, input);
        game.frames.add(image);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            MessageCreateBuilder message = new MessageCreateBuilder();
            StringBuilder text = new StringBuilder();
            if (game.tetris.alive && game.tetris.lines < 300) {
                text.append("Valid moves:");
                List<Tetris.Input> moves = game.tetris.getValidMoves();
                for (int i = 0; i < moves.size(); i++) {
                    text.append("\n");
                    text.append(Tetris.INPUT_EMOJIS[moves.get(i).ordinal()]);
                    text.append(moves.get(i));
                }
            }
            else {
                text.append("Game over!");
            }
            message.setContent(text.toString());
            message.addFiles(FileUpload.fromData(imageBytes, "tetris.jpg"));
            channel.sendMessage(message.build()).queue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveReplay(MessageReceivedEvent event, Game game) {
        String id = game.saveReplay();
        if (id == null) {
            event.getChannel().sendMessage("Error creating replay!").queue();
        }
        else if (id.equals("short")) {
            event.getChannel().sendMessage("Replay not created: at least 10 inputs must be played to create a replay.").queue();
        }
        else {
            event.getChannel().sendMessage("Creating replay with id \'" + id + "\'...").queue();
            event.getChannel().sendMessage("You may need to open the gif in your browser for it to load.\n[Click here to download your replay](https://raw.githubusercontent.com/derrick-x/Tetris-Replays/main/replays/" + id + "-replay.gif)").queue();
        }
    }
    
    public static Tetris.Input stringToInput(String input) {
        input = input.toUpperCase();
        List<Tetris.Input> inputs = new ArrayList<>(Arrays.asList(Tetris.Input.values()));
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < Tetris.ABBREVIATIONS[i].length; i++) {
                if (input.equals(Tetris.ABBREVIATIONS[i][j])) {
                    return inputs.get(i);
                }
            }
        }
        for (int i = 0; i < input.length(); i++) {
            if (inputs.isEmpty()) {
                return null;
            }
            for (int j = inputs.size() - 1; j >= 0; j--) {
                if (inputs.get(j).toString().charAt(i) != input.charAt(i)) {
                    inputs.remove(j);
                }
            }
        }
        if (inputs.size() == 1) {
            return inputs.get(0);
        }
        return null;
    }

    public static List<String> getReplays(String search) {
        List<String> replays = new ArrayList<>();
        try {
            URL url = new URL("https://api.github.com/repos/derrick-x/Tetris-Replays/contents/replays");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            JSONArray files = new JSONArray(content.toString());
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                String name = file.getString("name");
                String replayURL = file.getString("download_url");
                long time = Long.parseLong(name.split("-")[1]);
                if (name.contains(search)) {
                    replays.add("[" + name.substring(0, name.length() - 11) + "](<" + replayURL + ">): " + new Date(time).toString());
                }
            }
            return replays;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Draws the current frame of the given game onto the Graphics component.
     * @param g The Graphics component to draw on.
     * @param game The game state to draw.
     * @param user The name of the user to display.
     * @param input The last input received by the game.
     */
    public static void paintGame(Graphics g, Tetris game, String user, Tetris.Input input) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, 500, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(150, 50, 200, 400);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 10; x++) {
                g2d.setColor(new Color(Tetris.getColor(game.board[y][x], false)));
                g2d.fillRect(x * 20 + 150, y * 20 + 50, 20, 20);
            }
        }
        int[][] shape = game.getShadow();
        g2d.setColor(Color.GRAY);
        for (int i = 0; i < 4; i++) {
            g2d.fillRect(shape[i][0] * 20 + 150, shape[i][1] * 20 + 50, 20, 20);
        }
        shape = Tetris.getShape(game.queue.get(0), game.rotation);
        g2d.setColor(new Color(Tetris.getColor(game.queue.get(0), true)));
        for (int i = 0; i < 4; i++) {
            g2d.fillRect((shape[i][0] + game.position[0]) * 20 + 150, (shape[i][1] + game.position[1]) * 20 + 50, 20, 20);
        }
        for (int i = 1; i <= 3; i++) {
            shape = Tetris.getShape(game.queue.get(i), 0);
            g2d.setColor(new Color(Tetris.getColor(game.queue.get(i), false)));
            for (int j = 0; j < 4; j++) {
                g2d.fillRect(shape[j][0] * 20 + 410, shape[j][1] * 20 + 50 + 60 * i, 20, 20);
            }
        }
        g2d.setColor(Color.WHITE);
        g2d.drawRect(370, 70, 120, 200);
        g2d.setFont(new Font("Arial", Font.PLAIN, 15));
        g2d.drawString("NEXT", 370, 60);
        if (game.hold != Tetris.Piece.EMPTY) {
            shape = Tetris.getShape(game.hold, 0);
            g2d.setColor(new Color(Tetris.getColor(game.hold, false)));
            for (int i = 0; i < 4; i++) {
                g2d.fillRect(shape[i][0] * 20 + 60, shape[i][1] * 20 + 110, 20, 20);
            }
        }
        g2d.setColor(Color.WHITE);
        g2d.drawRect(20, 70, 120, 80);
        g2d.drawString("HOLD", 20, 60);
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(Color.DARK_GRAY);
        for (int y = 1; y < 20; y++) {
            g2d.drawLine(150, y * 20 + 50, 350, y * 20 + 50);
        }
        for (int x = 1; x < 10; x++) {
            g2d.drawLine(150 + x * 20, 50, 150 + x * 20, 450);
        }
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        g2d.setColor(Color.WHITE);
        if (user != null) {
            g2d.drawString(user + " played " + input, 10, 30);
        }
        g2d.drawString("Score", 10, 200);
        g2d.drawString(game.score + "", 10, 230);
        g.drawString("Level", 10, 280);
        g2d.drawString((game.lines / 10 + 1) + "", 10, 310);
        g2d.drawString("Lines", 10, 360);
        g2d.drawString(game.lines + "", 10, 390);
        g2d.setFont(new Font("Arial", Font.PLAIN, 15));
        g2d.drawString(game.message, 10, 480);
    }
}