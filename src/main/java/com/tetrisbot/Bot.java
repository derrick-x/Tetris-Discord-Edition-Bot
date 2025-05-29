package com.tetrisbot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import javax.imageio.stream.FileImageOutputStream;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;

/**
 * This class handles the Discord API and runs the game simulation using inputs
 * received from Discord users.
 */

public class Bot extends ListenerAdapter {
    static HashMap<Long, Game> games;
    static String gitToken = ""; //use your own GitHub token when testing
    static final Tetris.Input[] INPUT_INDEX = {
        Tetris.Input.HARDDROP,
        Tetris.Input.SOFTDROP,
        Tetris.Input.LEFT,
        Tetris.Input.RIGHT,
        Tetris.Input.CW,
        Tetris.Input.CCW,
        Tetris.Input.HOLD
    };

    static class Game {
        Tetris tetris;
        String owner;
        List<String> users;
        public Game(String o) {
            tetris = new Tetris();
            owner = o;
            users = new ArrayList<>();
        }

        /**
         * Converts the piece and input sequence of a game into a replay and
         * stores it in GitHub.
         */
        public String saveReplay() {
            String id = owner + System.currentTimeMillis();
            String apiURL = "https://api.github.com/repos/derrick-x/Tetris-Replays/contents/replays/" + id + ".txt";
            StringBuilder replay = new StringBuilder();
            for (int i = 0; i < tetris.fullQueue.size(); i++) {
                replay.append(tetris.fullQueue.get(i));
            }
            for (int i = 0; i < tetris.inputs.size(); i++) {
                switch(tetris.inputs.get(i)) {
                    case HARDDROP:
                    replay.append("\n0"); break;
                    case SOFTDROP:
                    replay.append("\n1"); break;
                    case LEFT:
                    replay.append("\n2"); break;
                    case RIGHT:
                    replay.append("\n3"); break;
                    case CW:
                    replay.append("\n4"); break;
                    case CCW:
                    replay.append("\n5"); break;
                    case HOLD:
                    replay.append("\n6"); break;
                    default: //default case should never occur
                }
                replay.append(users.get(i));
            }
            replay.append("\n");
            String encodedContent = Base64.getEncoder().encodeToString(replay.toString().getBytes(StandardCharsets.UTF_8));
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
            return id;
        }
    }
    
    public static void main(String[] args) throws LoginException {
        System.setProperty("java.awt.headless", "true");
        String token = ""; //use your own bot token when testing
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
        if (event.getMember() == null || event.getAuthor().isBot() || event.getMessage().getContentRaw().length() < 8 || !event.getMessage().getContentRaw().substring(0, 8).equals("!tetris ")) {
            return;
        }
        String[] args = event.getMessage().getContentRaw().split(" ");
        Game game = games.get(event.getChannel().getIdLong());
        if (args[1].equals("start")) {
            if (game == null) {
                event.getChannel().sendMessage("Starting game by " + event.getAuthor().getName() + "...").queue();
                games.put(event.getChannel().getIdLong(), new Game(event.getAuthor().getName()));
                sendTetris(event.getChannel(), games.get(event.getChannel().getIdLong()).tetris);
            }
            else {
                event.getChannel().sendMessage("Game already in progress!").queue();
            }
        }
        if (args[1].equals("abort")) {
            if (game == null) {
                event.getChannel().sendMessage("No game in progress!").queue();
            }
            else if (event.getAuthor().getName().equals(game.owner)) {
                event.getChannel().sendMessage("Saved replay with id \'" + game.saveReplay() + "\'...").queue();
                games.remove(event.getChannel().getIdLong());
                event.getChannel().sendMessage("Game aborted").queue();
            }
            else {
                event.getChannel().sendMessage("Only " + game.owner + " can abort the game!").queue();
            }
        }
        if (args[1].equals("replay")) {
            if (args.length < 3) {
                event.getChannel().sendMessage("Please specify a replay id!").queue();
            }
            else {
                event.getChannel().sendMessage("Creating replay...").queue();
                File gif = createReplay(args[2]);
                if (gif == null) {
                    event.getChannel().sendMessage("Error occured when creating replay! (Possible the replay with specified id does not exist?)").queue();
                }
                else {
                    event.getChannel().sendMessage("Replay attached to this message.").addFiles(FileUpload.fromData(gif)).queue(success -> {
                        gif.delete();
                    });
                }
            }
        }
        try {
            Tetris.Input input = Tetris.Input.valueOf(args[1].toUpperCase());
            if (game == null) {
                event.getChannel().sendMessage("No game in progress!").queue();
            }
            else if (game.tetris.getValidMoves().contains(input)) {
                if (game.users.isEmpty() || !event.getMember().getEffectiveName().equals(game.users.get(game.users.size() - 1))) {
                    game.users.add(event.getMember().getEffectiveName());
                    game.tetris.input(input);
                    event.getChannel().sendMessage(event.getMember().getEffectiveName() + " played " + input).queue();
                    sendTetris(event.getChannel(), game.tetris);
                    if (game.tetris.lines >= 300 || !game.tetris.alive) {
                        event.getChannel().sendMessage("Saved replay with id \'" + game.saveReplay() + "\'").queue();
                        games.remove(event.getChannel().getIdLong());
                    }
                }
                else {
                    event.getChannel().sendMessage(event.getMember().getEffectiveName() + ", you already played the last move!").queue();
                }
            }
            else {
                event.getChannel().sendMessage(input + " is not a valid move!").queue();
            }
        } catch (IllegalArgumentException e) {

        }
    }

    /**
     * Sends a message using emojis to display a Tetris game.
     * @param channel The channel to send the message.
     * @param game The Tetris.java instance in to display.
     */
    public void sendTetris(MessageChannelUnion channel, Tetris game) {
        StringBuilder message = new StringBuilder();
        int[][] shadow = game.getShadow();
        int[][] shape = Tetris.getShape(game.queue.get(0), game.rotation);
        message.append(game.message);
        for (int y = 0; y < 12; y++) {
            message.append("\n");
            for (int x = 0; x < 10; x++) {
                Tetris.Piece tile = game.board[y][x];
                for (int i = 0; i < 4; i++) {
                    if (x == shadow[i][0] && y == shadow[i][1]) {
                        tile = null;
                    }
                }
                for (int i = 0; i < 4; i++) {
                    if (x == shape[i][0] + game.position[0] && y == shape[i][1] + game.position[1]) {
                        tile = game.queue.get(0);
                    }
                }
                if (tile == null) {
                    message.append("🔳");
                }
                else {
                    switch(tile) {
                        case I:
                        message.append("⬜");
                        break;
                        case J:
                        message.append("🟦");
                        break;
                        case L:
                        message.append("🟧");
                        break;
                        case O:
                        message.append("🟨");
                        break;
                        case S:
                        message.append("🟩");
                        break;
                        case T:
                        message.append("🟪");
                        break;
                        case Z:
                        message.append("🟥");
                        break;
                        default:
                        message.append("⬛");
                    }
                }
            }
        }
        channel.sendMessage(message.toString()).queue();
        message = new StringBuilder();
        for (int y = 12; y < 20; y++) {
            for (int x = 0; x < 10; x++) {
                Tetris.Piece tile = game.board[y][x];
                for (int i = 0; i < 4; i++) {
                    if (x == shadow[i][0] && y == shadow[i][1]) {
                        tile = null;
                    }
                }
                for (int i = 0; i < 4; i++) {
                    if (x == shape[i][0] + game.position[0] && y == shape[i][1] + game.position[1]) {
                        tile = game.queue.get(0);
                    }
                }
                if (tile == null) {
                    message.append("🔳");
                }
                else {
                    switch(tile) {
                        case I:
                        message.append("⬜");
                        break;
                        case J:
                        message.append("🟦");
                        break;
                        case L:
                        message.append("🟧");
                        break;
                        case O:
                        message.append("🟨");
                        break;
                        case S:
                        message.append("🟩");
                        break;
                        case T:
                        message.append("🟪");
                        break;
                        case Z:
                        message.append("🟥");
                        break;
                        default:
                        message.append("⬛");
                    }
                }
            }
            message.append("\n");
        }
        message.append("HOLD:\n");
        message.append(pieceToString(game.hold));
        message.append("NEXT:\n");
        for (int i = 1; i < 4; i++) {
            message.append(pieceToString(game.queue.get(i)));
        }
        message.append("SCORE: ");
        message.append(game.score);
        message.append("\nLEVEL: ");
        message.append(1 + game.lines / 10);
        message.append("\nLINES: ");
        message.append(game.lines);
        if (game.alive && game.lines < 300) {
            message.append("\nVALID MOVES:");
            List<Tetris.Input> moves = game.getValidMoves();
            for (int i = 0; i < moves.size(); i++) {
                message.append("\n");
                message.append(moves.get(i));
            }
        }
        else {
            message.append("\nGame over!");
        }
        channel.sendMessage(message.toString()).queue();
    }

    /**
     * Returns a string representation of a Tetris piece that can be placed in
     * a message.
     * @param piece The Tetris piece to convert into a string.
     * @return A string of emojis representing the specified Tetris piece.
     */
    public static String pieceToString(Tetris.Piece piece) {
        switch(piece) {
            case I:
            return "⬛⬛⬛⬛⬛⬛\n⬛⬜⬜⬜⬜⬛\n⬛⬛⬛⬛⬛⬛\n";
            case J:
            return "⬛⬛⬛⬛⬛\n⬛🟦⬛⬛⬛\n⬛🟦🟦🟦⬛\n⬛⬛⬛⬛⬛\n";
            case L:
            return "⬛⬛⬛⬛⬛\n⬛⬛⬛🟧⬛\n⬛🟧🟧🟧⬛\n⬛⬛⬛⬛⬛\n";
            case O:
            return "⬛⬛⬛⬛\n⬛🟨🟨⬛\n⬛🟨🟨⬛\n⬛⬛⬛⬛\n";
            case S:
            return "⬛⬛⬛⬛⬛\n⬛⬛🟩🟩⬛\n⬛🟩🟩⬛⬛\n⬛⬛⬛⬛⬛\n";
            case T:
            return "⬛⬛⬛⬛⬛\n⬛⬛🟪⬛⬛\n⬛🟪🟪🟪⬛\n⬛⬛⬛⬛⬛\n";
            case Z:
            return "⬛⬛⬛⬛⬛\n⬛🟥🟥⬛⬛\n⬛⬛🟥🟥⬛\n⬛⬛⬛⬛⬛\n";
            default:
            return "⬛\n";
        }
    }

    /**
     * Creates a replay of a Tetris game in gif format.
     * @param id The id of the replay to search for.
     * @return A file representing the gif.
     */
    public static File createReplay(String id) {
        try {
            String apiURL = "https://api.github.com/repos/derrick-x/Tetris-Replays/contents/replays/" + id + ".txt";
            HttpURLConnection conn = (HttpURLConnection) new URL(apiURL).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + gitToken);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            String json = response.toString();
            String encode = json.split("\"content\":\"")[1].split("\"")[0].replace("\\n", "");
            String[] decode = new String(Base64.getDecoder().decode(encode)).split("\n");
            File gif = new File(id + "-replay.gif");
            try {
                FileImageOutputStream output = new FileImageOutputStream(gif);
                GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_RGB, 500, true);
                Tetris game = new Tetris(decode[0]);
                BufferedImage initial = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
                paintGame(initial.getGraphics(), game, null, 0);
                writer.writeToSequence(initial);
                for (int i = 1; i < decode.length; i++) {
                    int input = decode[i].charAt(0) - '0';
                    game.input(INPUT_INDEX[input]);
                    BufferedImage frame = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
                    paintGame(frame.getGraphics(), game, decode[i].substring(1), input);
                    writer.writeToSequence(frame);
                }
                output.close();
                return gif;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Draws the current frame of the given game onto the Graphics component.
     * @param g The Graphics component to draw on.
     * @param game The game state to draw.
     * @param user The name of the user to display.
     * @param input The last input received by the game.
     */
    public static void paintGame(Graphics g, Tetris game, String user, int input) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 600, 600);
        g.setColor(Color.WHITE);
        g.drawRect(200, 100, 200, 400);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 10; x++) {
                g.setColor(new Color(Tetris.getColor(game.board[y][x])));
                g.fillRect(x * 20 + 200, y * 20 + 100, 20, 20);
            }
        }
        int[][] shape = game.getShadow();
        g.setColor(Color.GRAY);
        for (int i = 0; i < 4; i++) {
            g.fillRect(shape[i][0] * 20 + 200, shape[i][1] * 20 + 100, 20, 20);
        }
        shape = Tetris.getShape(game.queue.get(0), game.rotation);
        g.setColor(new Color(Tetris.getColor(game.queue.get(0))));
        for (int i = 0; i < 4; i++) {
            g.fillRect((shape[i][0] + game.position[0]) * 20 + 200, (shape[i][1] + game.position[1]) * 20 + 100, 20, 20);
        }
        for (int i = 1; i <= 3; i++) {
            shape = Tetris.getShape(game.queue.get(i), 0);
            g.setColor(new Color(Tetris.getColor(game.queue.get(i))));
            for (int j = 0; j < 4; j++) {
                g.fillRect(shape[j][0] * 20 + 460, shape[j][1] * 20 + 100 + 60 * i, 20, 20);
            }
        }
        if (game.hold != Tetris.Piece.EMPTY) {
            shape = Tetris.getShape(game.hold, 0);
            g.setColor(new Color(Tetris.getColor(game.hold)));
            for (int i = 0; i < 4; i++) {
                g.fillRect(shape[i][0] * 20 + 60, shape[i][1] * 20 + 100, 20, 20);
            }
        }
        g.setColor(Color.WHITE);
        if (user != null) {
            g.drawString(user + " played " + INPUT_INDEX[input], 60, 50);
        }
        g.drawString("Score: " + game.score, 60, 200);
        g.drawString("Lines: " + game.lines, 60, 220);
        g.drawString(game.message, 60, 550);
    }
}