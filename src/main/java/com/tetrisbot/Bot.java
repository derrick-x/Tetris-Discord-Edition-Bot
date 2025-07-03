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
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.security.auth.login.LoginException;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

/**
 * This class handles the Discord API and runs the game simulation using inputs
 * received from Discord users.
 */

public class Bot extends ListenerAdapter {
    static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN"); //If testing on your own, use your own bot token
    static final long BOT_ID = 1377027446783610890L; //replace with your bot's ID
    static final String GIT_TOKEN = System.getenv("GIT_TOKEN"); //use your GitHub token
    static final String VERSION = 
    "# v1.0.0: Tetris Discord Edition Bot: Full Release" + "\n" +
    "- TBA";
    static final String HELP =
    "**Tetris Bot commands:**" + "\n" +
    "* " + "`start {code}`: Enters the start menu for a new game in a channel. To quickstart, place the code of the desired config, generated from the start menu. 0 is default." + "\n" +
    "* " + "`abort`: aborts the current game in a channel." + "\n" +
    "* " + "`react`: if reaction-based input mode is enabled, sends a new reaction panel in front of all messages. Useful if discussions pushed the reaction panel far away." + "\n" +
    "* " + "`[input]`: plays the input, if valid. You can also enter any unambiguous prefix of the input, such as `l` for LEFT or `ha` for HARDDROP."+ "\n" +
    "* " + "`[input] -`: repeats the input until it is no longer valid. Only applies to LEFT, RIGHT, and SOFTDROP."+ "\n" +
    "* " + "`keybind {[input] [keybind]}`: sets the list of input-keybind pairs as your custom keybinds. For example, `keybind ha hd ho c ccw z` sets HARDDROP to hd, HOLD to c, and CCW to z. Custom keybinds are case sensitive." + "\n" +
    "* " + "`keybind {[input] [keybind]}`: displays your current keybinds set." + "\n" +
    "* " + "`version`: Gets the current version of the game." + "\n" +
    "**You may send a command by using the \"!tetris\" prefix or by replying to any bot message in the same channel.**";
    static final String[][] FLAGS = {
        {"Off", "On"},
        {"Every input", "Every piece", "Off"},
        {"3", "4", "5", "0", "1", "2"}};
    static HashMap<Long, int[]> menus;
    static HashMap<Long, Game> games;
    static HashMap<Long, HashMap<String, Tetris.Input>> keybinds;

    static class Game {
        Tetris tetris;
        String owner;
        long lastUserId;
        long inputPanelId;
        Message gameMessage;
        int consecutive;
        List<File> gifs;
        String gifId;
        List<GifSequenceWriter> gifWriters;
        int frames;
        public Game(String o, int[] flags) {
            tetris = new Tetris((flags[2] + 3) % 6);
            owner = o;
            lastUserId = -1;
            if (flags[0] == 1){
                inputPanelId = 0;
            }
            else {
                inputPanelId = -1;
            }
            gifs = new ArrayList<>();
            gifWriters = new ArrayList<>();
            gameMessage = null;
            consecutive = flags[1];
            gifId = owner + "-" + System.currentTimeMillis();
            frames = 0;
        }

        /**
         * Converts the piece and input sequence of a game into a replay and
         * stores it in GitHub.
         */
        public String saveReplay() {
            for (int i = 0; i < gifs.size(); i++) {
                String apiURL = "https://api.github.com/repos/derrick-x/Tetris-Replays/contents/replays/" + gifId + "/pt" + (i + 1) + "-replay.gif";
                byte[] fileBytes = new byte[(int) gifs.get(i).length()];
                try (FileInputStream inputStream = new FileInputStream(gifs.get(i))) {
                    inputStream.read(fileBytes);
                    gifWriters.get(i).close();
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
                    conn.setRequestProperty("Authorization", "Bearer " + GIT_TOKEN);
                    conn.setRequestProperty("Accept", "application/vnd.github+json");
                    conn.setRequestProperty("Content-Type", "application/json");
                    OutputStream os = conn.getOutputStream();
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                    int code = conn.getResponseCode();
                    System.out.println("GitHub upload response: " + code);
                    if (code != 201) {
                        return null;
                    }
                    gifs.get(i).delete();
                    gifs.set(i, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            gifs.clear();
            return gifId;
        }
        
        public void addFrame(BufferedImage image) throws IOException {
            if (frames % 1000 == 0) {
                File gif = new File("pt" + (frames / 1000 + 1) + "-replay.gif");
                gifs.add(gif);
                FileImageOutputStream output = new FileImageOutputStream(gif);
                gifWriters.add(new GifSequenceWriter(output, BufferedImage.TYPE_INT_RGB, 500, false));
            }
            gifWriters.get(gifWriters.size() - 1).writeToSequence(image);
            frames++;
        }
    }
    
    public static void main(String[] args) throws LoginException {
        keybinds = new HashMap<>();
        System.setProperty("java.awt.headless", "true");
        JDABuilder.createDefault(DISCORD_TOKEN)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .setMemberCachePolicy(MemberCachePolicy.NONE)
            .disableCache(
                CacheFlag.CLIENT_STATUS,
                CacheFlag.ACTIVITY,
                CacheFlag.EMOJI,
                CacheFlag.VOICE_STATE,
                CacheFlag.ONLINE_STATUS
            )
            .addEventListeners(new Bot())
            .build();
        games = new HashMap<>();
        menus = new HashMap<>();
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
                if (args.length < 2 || !args[1].matches("-?\\d+(\\.\\d+)?")) {
                    int[] flags = new int[FLAGS.length + 1];
                    event.getChannel().sendMessage(startMenu(flags)).queue(sentMessage -> {
                        menus.put(sentMessage.getIdLong(), flags);
                        sentMessage.addReaction(Emoji.fromUnicode("🔼")).queue();
                        sentMessage.addReaction(Emoji.fromUnicode("◀️")).queue();
                        sentMessage.addReaction(Emoji.fromUnicode("▶️")).queue();
                        sentMessage.addReaction(Emoji.fromUnicode("🔽")).queue();
                        sentMessage.addReaction(Emoji.fromUnicode("🆗")).queue();
                    });
                }
                else {
                    int code = Integer.parseInt(args[1]);
                    int[] flags = new int[FLAGS.length];
                    for (int i = 0; i < FLAGS.length; i++) {
                        flags[i] = code % FLAGS[i].length;
                        code /= FLAGS[i].length;
                    }
                    event.getChannel().sendMessage("Starting game by " + event.getAuthor().getName() + "...").queue();
                    games.put(event.getChannel().getIdLong(), new Game(event.getAuthor().getName(), flags));
                    sendTetris(event.getChannel(), games.get(event.getChannel().getIdLong()), null, null);
                }
            }
            else {
                event.getChannel().sendMessage("Game already in progress!").queue();
            }
        }
        if (args[0].equals("abort")) {
            if (game == null) {
                event.getChannel().sendMessage("No game in progress!").queue();
            }
            else if (event.getAuthor().getName().equals(game.owner) || (event.getMember() != null && event.getMember().hasPermission(Permission.MESSAGE_MANAGE))) {
                event.getChannel().sendMessage("Game aborted").queue();
                saveReplay(event, game);
                games.remove(event.getChannel().getIdLong());
            }
            else {
                event.getChannel().sendMessage("Only " + game.owner + " and users with the Manage Messages permission can abort the game!").queue();
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
        if (args[0].equals("keybind")) {
            if (args.length % 2 != 1) {
                event.getChannel().sendMessage("Incorrect number of arguments provided! (Please provide a list of input-string pairs)").queue();
            }
            else if (args.length == 1) {
                HashMap<String, Tetris.Input> user = keybinds.get(event.getAuthor().getIdLong());
                if (user == null) {
                    event.getChannel().sendMessage(event.getAuthor().getEffectiveName() + ", you have no custom keybinds set!").queue();
                }
                else {
                    StringBuilder msg = new StringBuilder(event.getAuthor().getEffectiveName() + " keybinds:\n");
                    for (String key : user.keySet()) {
                        msg.append(key).append(": ").append(user.get(key)).append("\n");
                    }
                    event.getChannel().sendMessage(msg.toString()).queue();
                }
            }
            else {
                StringBuilder msg = new StringBuilder(event.getAuthor().getEffectiveName() + " updated keybinds:\n");
                HashMap<String, Tetris.Input> user = keybinds.get(event.getAuthor().getIdLong());
                if (user == null) {
                    user = new HashMap<>();
                    keybinds.put(event.getAuthor().getIdLong(), user);
                }
                for (int i = 2; i < args.length; i += 2) {
                    Tetris.Input input = stringToInput(args[i - 1], event.getAuthor().getIdLong());
                    if (input != null) {
                        user.put(args[i], input);
                        msg.append(input).append(" set to ").append(args[i]).append("\n");
                    }
                }
                event.getChannel().sendMessage(msg.toString()).queue();
            }
        }
        if (args[0].equals("react")){
            if (game == null) {
                event.getChannel().sendMessage("No game in progress!").queue();
            }
            else {
                if (game.inputPanelId < 0) {
                    event.getChannel().sendMessage("Current game is not using reaction inputs!").queue();
                }
                else {
                    event.getChannel().retrieveMessageById(game.inputPanelId).complete().delete().complete();
                    event.getChannel().sendMessage("Use the reactions below to play inputs.").queue(sentMessage -> {
                        game.inputPanelId = sentMessage.getIdLong();
                        for (int i = 0; i < 7; i++) {
                            sentMessage.addReaction(Emoji.fromUnicode(Tetris.INPUT_EMOJIS[i])).queue();
                        }
                    });
                }
            }
        }
        if (args[0].equals("version")) {
            event.getChannel().sendMessage(VERSION).queue();
        }
        if (args[0].equals("help")) {
            event.getChannel().sendMessage(HELP).queue();
        }
        Tetris.Input input = stringToInput(args[0], event.getAuthor().getIdLong());
        if (input != null) {
            if (game == null) {
                event.getChannel().sendMessage("No game in progress!").queue();
            }
            else if (game.tetris.getValidMoves().contains(input)) {
                if (game.consecutive == 2 || event.getAuthor().getIdLong() != game.lastUserId) {
                    boolean placed = false;
                    if (args.length > 1 && input != Tetris.Input.CW && input != Tetris.Input.CCW && input != Tetris.Input.HARDDROP && args[1].equals("-")) {
                        while (game.tetris.getValidMoves().contains(input)) {
                            placed |= game.tetris.input(input);
                        }
                    }
                    else {
                        placed |= game.tetris.input(input);
                    }
                    if (placed || game.consecutive == 0) {
                        game.lastUserId = event.getAuthor().getIdLong();
                    }
                    sendTetris(event.getChannel(), game, event.getMember().getEffectiveName(), input);
                    if (game.tetris.lines >= 300 || !game.tetris.alive) {
                        saveReplay(event, game);
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
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        User user = event.retrieveUser().complete();
        if (user.getIdLong() == BOT_ID) {
            return;
        }
        int[] flags = menus.get(event.getMessageIdLong());
        if (flags != null) {
            switch (event.getEmoji().getName()) {
                case "🔼":
                flags[FLAGS.length] += FLAGS.length - 1;
                if (flags[FLAGS.length] >= FLAGS.length) {
                    flags[FLAGS.length] %= FLAGS.length;
                }
                break;
                case "◀️":
                flags[flags[FLAGS.length]] += FLAGS[flags[FLAGS.length]].length - 1;
                if (flags[flags[FLAGS.length]] >= FLAGS[flags[FLAGS.length]].length) {
                    flags[flags[FLAGS.length]] %= FLAGS[flags[FLAGS.length]].length;
                }
                break;
                case "▶️":
                flags[flags[FLAGS.length]]++;
                if (flags[flags[FLAGS.length]] >= FLAGS[flags[FLAGS.length]].length) {
                    flags[flags[FLAGS.length]] %= FLAGS[flags[FLAGS.length]].length;
                }
                break;
                case "🔽":
                flags[FLAGS.length]++;
                if (flags[FLAGS.length] >= FLAGS.length) {
                    flags[FLAGS.length] %= FLAGS.length;
                }
                break;
                case "🆗":
                if (games.get(event.getChannel().getIdLong()) == null) {
                    event.getChannel().sendMessage("Starting game by " + event.retrieveUser().complete().getName() + "...").queue();
                    games.put(event.getChannel().getIdLong(), new Game(event.retrieveUser().complete().getName(), flags));
                    sendTetris(event.getChannel(), games.get(event.getChannel().getIdLong()), null, null);
                }
                else {
                    event.getChannel().sendMessage("Game already in progress!").queue();
                }
                menus.remove(event.getMessageIdLong());
                break;
                default:
            }
            event.retrieveMessage().complete().editMessage(startMenu(flags)).queue();
            return;
        }
        Game game = games.get(event.getChannel().getIdLong());
        if (game == null) {
            return;
        }
        if (event.getMessageIdLong() == game.inputPanelId) {
            try {
                event.getReaction().removeReaction(user).complete();
            } catch (InsufficientPermissionException e) {}
            for (int i = 0; i < 7; i++) {
                if (event.getEmoji().getName().equals(Tetris.INPUT_EMOJIS[i])) {
                    Tetris.Input input = Tetris.Input.values()[i];
                    if (game.consecutive == 2 || event.retrieveUser().complete().getIdLong() != game.lastUserId) {
                        if (game.tetris.getValidMoves().contains(input)) {
                            game.lastUserId = event.retrieveUser().complete().getIdLong();
                            game.tetris.input(input);
                            sendTetris(event.getChannel(), game, event.retrieveMember().complete().getEffectiveName(), input);
                            if (game.tetris.lines >= 300 || !game.tetris.alive) {
                                saveReplay(event, game);
                                games.remove(event.getChannel().getIdLong());
                            }
                        }
                        else {
                            event.getChannel().sendMessage(input + " is not a valid move!").queue(sentMessage -> {
                                sentMessage.delete().submitAfter(5, TimeUnit.SECONDS);
                            });
                        }
                    }
                    else {
                        event.getChannel().sendMessage(event.getMember().getEffectiveName() + ", you already played the last move!").queue(sentMessage -> {
                            sentMessage.delete().submitAfter(5, TimeUnit.SECONDS);
                        });
                    }
                }
            }
        }
    }

    /**
     * Sends a message using emojis to display a Tetris game.
     * @param channel The channel to send the message.
     * @param game The Tetris.java instance in to display.
     */
    public void sendTetris(MessageChannelUnion channel, Game game, String user, Tetris.Input input) {
        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        paintGame(g, game.tetris, user, input);
        g.dispose();
        try {
            game.addFrame(image);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            image.flush();
            image = null;
            byte[] imageBytes = baos.toByteArray();
            baos.flush();
            baos.close();
            baos = null;
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
            imageBytes = null;
            System.gc();
            if (game.inputPanelId < 0) {
                channel.sendMessage(message.build()).queue();
            }
            else {
                if (game.gameMessage != null) {
                    game.gameMessage.delete().queue();
                }
                else {
                    channel.sendMessage("Use the reactions below to play inputs.").queue(sentMessage -> {
                        game.inputPanelId = sentMessage.getIdLong();
                        for (int i = 0; i < 7; i++) {
                            sentMessage.addReaction(Emoji.fromUnicode(Tetris.INPUT_EMOJIS[i])).queue();
                        }
                    });
                }
                channel.sendMessage(message.build()).queue(sentMessage -> {
                    game.gameMessage = sentMessage;
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a replay of the finished game, uploads it to GitHub, and sends
     * the link to the channel.
     * @param event The MessageReceivedEvent where a replay is to be created.
     * @param game The finished game to create a replay of.
     */
    public static void saveReplay(GenericMessageEvent event, Game game) {
        if (game.frames < 10) {
            event.getChannel().sendMessage("Replay not created - at least 10 inputs must be played to create a replay.").queue();
            return;
        }
        event.getChannel().sendMessage("Creating replay, please wait...").queue();
        String id = game.saveReplay();
        if (id == null) {
            event.getChannel().sendMessage("Error creating replay!").queue();
        }
        else {
            event.getChannel().sendMessage("Created replay with id \'" + id + "\nYou may need to open the gif in your browser for it to load.\n[Click here to download your replay](<https://github.com/derrick-x/Tetris-Replays/tree/main/replays/" + id + ">)").queue();
        }
    }

    public static String startMenu(int[] flags) {
        StringBuilder menu = new StringBuilder();
        menu.append("**START MENU**");
        menu.append("\nReaction mode: ").append(FLAGS[0][flags[0]]);
        if (flags[FLAGS.length] == 0) {
            menu.append("◀️▶️");
        }
        menu.append("\nSwitch players: ").append(FLAGS[1][flags[1]]);
        if (flags[FLAGS.length] == 1) {
            menu.append("◀️▶️");
        }
        menu.append("\nPreview size: ").append(FLAGS[2][flags[2]]);
        if (flags[FLAGS.length] == 2) {
            menu.append("◀️▶️");
        }
        int code = 0;
        for (int i = flags.length - 2; i >= 0; i--) {
            code *= FLAGS[i].length;
            code += flags[i];
        }
        menu.append("\n**Press 🆗 to start**\nPro tip: use command `start ").append(code).append("` to directly start a game with the current configuration.");
        return menu.toString();
    }
    
    /**
     * Converts a String to a Tetris.Input, checking the user's keybinds and
     * default keybinds.
     * @param input The input String received by the bot.
     * @param userId The id of the user sending an input string.
     * @return The corresponding Tetris.Input, or null if no matching input
     * found.
     */
    public static Tetris.Input stringToInput(String input, long userId) {
        HashMap<String, Tetris.Input> user = keybinds.get(userId);
        if (user != null) {
            for (String key : user.keySet()) {
                if (input.equals(key)) {
                    return user.get(key);
                }
            }
        }
        input = input.toUpperCase();
        List<Tetris.Input> inputs = new ArrayList<>(Arrays.asList(Tetris.Input.values()));
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

    /**
     * Finds all replays on GitHub that match the search query.
     * @param search The string to search for in replay ids.
     * @return A list of replay links containing the search string.
     */
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
                if (name.contains(search)) {
                    try {
                        long time = Long.parseLong(name.split("-")[1]);
                        replays.add("[" + name.substring(0, name.length() - 11) + "](<" + replayURL + ">): " + new Date(time).toString());
                    } catch (IllegalArgumentException e) {
                        replays.add("[" + name.substring(0, name.length() - 11) + "](<" + replayURL + ">): [No timestamp]");
                    }
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
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, 300, 300);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(90, 30, 120, 240);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 10; x++) {
                g2d.setColor(new Color(Tetris.getColor(game.board[y][x], false)));
                g2d.fillRect(x * 12 + 90, y * 12 + 30, 12, 12);
            }
        }
        int[][] shape = game.getShadow();
        g2d.setColor(Color.GRAY);
        for (int i = 0; i < 4; i++) {
            g2d.fillRect(shape[i][0] * 12 + 90, shape[i][1] * 12 + 30, 12, 12);
        }
        shape = Tetris.getShape(game.queue.get(0), game.rotation);
        g2d.setColor(new Color(Tetris.getColor(game.queue.get(0), true)));
        for (int i = 0; i < 4; i++) {
            g2d.fillRect((shape[i][0] + game.position[0]) * 12 + 90, (shape[i][1] + game.position[1]) * 12 + 30, 12, 12);
        }
        for (int i = 1; i <= game.preview; i++) {
            shape = Tetris.getShape(game.queue.get(i), 0);
            g2d.setColor(new Color(Tetris.getColor(game.queue.get(i), false)));
            for (int j = 0; j < 4; j++) {
                g2d.fillRect(shape[j][0] * 12 + 246, shape[j][1] * 12 + 30 + 36 * i, 12, 12);
            }
        }
        g2d.setColor(Color.WHITE);
        g2d.drawRect(222, 42, 72, 12 + game.preview * 36);
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        g2d.drawString("NEXT", 222, 36);
        if (game.hold != Tetris.Piece.EMPTY) {
            shape = Tetris.getShape(game.hold, 0);
            g2d.setColor(new Color(Tetris.getColor(game.hold, false)));
            for (int i = 0; i < 4; i++) {
                g2d.fillRect(shape[i][0] * 12 + 36, shape[i][1] * 12 + 66, 12, 12);
            }
        }
        g2d.setColor(Color.WHITE);
        g2d.drawRect(12, 42, 72, 48);
        g2d.drawString("HOLD", 12, 36);
        g2d.setStroke(new BasicStroke(0.5f));
        g2d.setColor(Color.DARK_GRAY);
        for (int y = 1; y < 20; y++) {
            g2d.drawLine(90, y * 12 + 30, 210, y * 12 + 30);
        }
        for (int x = 1; x < 10; x++) {
            g2d.drawLine(90 + x * 12, 30, 90 + x * 12, 270);
        }
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.setColor(Color.WHITE);
        if (user != null) {
            g2d.drawString(user + " played " + input, 6, 18);
        }
        g2d.drawString("Score", 6, 120);
        g2d.drawString(game.score + "", 6, 138);
        g.drawString("Level", 6, 168);
        g2d.drawString(Math.min(30, (game.lines / 10 + 1)) + "", 6, 186);
        g2d.drawString("Lines", 6, 216);
        g2d.drawString(game.lines + "", 6, 234);
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        g2d.drawString(game.message, 6, 288);
    }
}