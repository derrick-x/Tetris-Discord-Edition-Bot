package com.tetrisbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

/**
 * This class handles the Discord API and manages all application operations.
 */

public class Bot extends ListenerAdapter {
    static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN"); //If testing on your own, use your own bot token
    static final long BOT_ID = 1377027446783610890L; //replace with your bot's ID
    static final String VERSION = 
    "# v1.2: User Profiles" +
    "\n- Player stats are now saved on your user profile! Use command `profile` to view your stats." +
    "\n- Prefixes can now be customized for each server by members with the Manage Server permission. Use command `prefix [prefix]` to configure. `!tetris ` will always work." +
    "\n- Game now displays valid moves at each frame." +
    "\n- Some bug fixes and behind the scenes work.";
    static final String HELP =
    "**Tetris Bot commands:**" + "\n" +
    "* " + "`start {code}`: Enters the start menu for a new game in a channel. To quickstart, place the code of the desired config, generated from the start menu. 0 is default." + "\n" +
    "* " + "`abort`: aborts the current game in a channel." + "\n" +
    "* " + "`[input]`: plays the input, if valid. You can also enter any unambiguous prefix of the input, such as `l` for LEFT or `ha` for HARDDROP."+ "\n" +
    "* " + "`[input] -`: repeats the input until it is no longer valid. Only applies to LEFT, RIGHT, and SOFTDROP."+ "\n" +
    "* " + "`keybind {[input] [keybind]}`: sets the list of input-keybind pairs as your custom keybinds. For example, `keybind ha hd ho c ccw z` sets HARDDROP to hd, HOLD to c, and CCW to z. Custom keybinds are case sensitive." + "\n" +
    "* " + "`keybind`: displays your current keybinds set." + "\n" +
    "* " + "`prefix [text]`: Reconfigures the server's prefix to the text specified. User must have Manage Messages permission to use command." + "\n" +
    "* " + "`profile`: displays your current keybinds set." + "\n" +
    "* " + "`leaderboard`: displays the current leaderboard." + "\n" +
    "* " + "`version`: Gets the current version of the game." + "\n" +
    "**You may send a command by using the \"!tetris\" prefix or by replying to any bot message in the same channel.**";
    static final String[][] FLAGS = { //Start menu options
        {"Off", "On"},
        {"Every input", "Every piece", "Off"},
        {"3", "4", "5", "0", "1", "2"}};
    static HashMap<Long, Long> menuChannels; //Map between channel IDs and menu IDs.
    static HashMap<Long, int[]> menus; //Map between menu IDs and start configurations.
    static HashMap<Long, Game> games; //Map between channel IDs and games.
    static HashMap<Long, HashMap<String, Tetris.Input>> keybinds; //Map between keybinds and inputs.
    static HashMap<Long, Score> leaderboard; //Map between channel IDs and scores.
    static HashMap<Long, String> prefixes; //Map between server IDs and prefixes.
    static long shutdown; //If shutdown scheduled, when it will occur.
    static HashSet<Long> broadcasted; //Which channels have received announcements.
    static String announcement; //Announcement to broadcast.
    static boolean ready; //When bot completes startup procedure.
    static HashMap<String, Player> players; //Map between player name and player.
    static class Score implements Comparable<Score> { //Class to store score information.
        int value;
        String id;
        public Score(int v, String i) {
            value = v; id = i;
        } 
        @Override
        public int compareTo(Score o) {
            if (value == o.value) {
                if (Long.parseLong(id.split("/")[1]) > Long.parseLong(o.id.split("/")[1])) {
                    return 1;
                }
                else if (Long.parseLong(id.split("/")[1]) > Long.parseLong(o.id.split("/")[1])) {
                    return -1;
                }
                else {
                    return 0;
                }
            }
            else {
                return o.value - value;
            }
        }
        public String toString() {
            return value + " by " + id;
        }
    }
    static class Player { //Class to store player information
        String id;
        int inputs;
        long points;
        long assist;
        int highScore;
        int completed;
        HashSet<String> channels;
        Integer[] clears;
        public Player() {
            this("");
        }
        public Player(String i) {
            id = i;
            inputs = 0;
            points = 0;
            assist = 0;
            highScore = -1;
            completed = 0;
            channels = new HashSet<>();
            clears = new Integer[12];
            Arrays.fill(clears, 0);
        }
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("inputs", inputs);
            map.put("points", points);
            map.put("assist", assist);
            map.put("highScore", highScore);
            map.put("completed", completed);
            map.put("channels", new ArrayList<>(channels));
            map.put("clears", Arrays.asList(clears));
            return map;
        }
        public static Player fromMap(Map<String, Object> map) {
            Player player = new Player();
            player.id = map.get("id").toString();
            player.inputs = ((Number) map.get("inputs")).intValue();
            player.points = ((Number) map.get("points")).longValue();
            player.assist = ((Number) map.get("assist")).longValue();
            player.highScore = ((Number) map.get("highScore")).intValue();
            player.completed = ((Number) map.get("completed")).intValue();
            player.channels = new HashSet<>((List<String>) map.get("channels"));
            List<Long> clears = (List<Long>) map.get("clears");
            for (int i = 0; i < 12; i++) {
                player.clears[i] = clears.get(i).intValue();
            }
            return player;
        }
        public MessageEmbed display(String name) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(name);
            embed.addField("Inputs played", inputs + "\n*Total inputs played by user across all games.*", true);
            embed.addField("Points scored", points + "\n*Total points scored by user from all inputs.*", true);
            embed.addField("Points assisted", assist + "\n*Points scored by another player's placement, multiplied by user's contribution percentage to that piece.*", true);
            embed.addField("High Score", (highScore < 0 ? "N/A" : highScore) + "\n*Highest score achieved in a game contributed to. User must have played at least half the average number of inputs per player in a game to receive that game's score.*", true);
            embed.addField("Games completed", completed + "\n*Total number of games contributed to that were completed (not aborted). User must have played at least half the average number of inputs per player in a game to receive that game's score.*", true);
            embed.addField("Channels played", channels.size() + "\n*Total number of channels where at least one input was played by user.*", true);
            embed.addField("Favorite line clear", getFavorite() + "\n*Most frequent line clear played by user.*", true);
            return embed.build();
        }
        public String getFavorite() {
            int max = 0;
            for (int i = 1; i < 12; i++) {
                if (clears[i] > clears[max]) {
                    max = i;
                }
            }
            if (clears[max] == 0) {
                return "N/A";
            }
            switch (max) {
                case 0:
                return "SINGLE";
                case 1:
                return "DOUBLE";
                case 2:
                return "TRIPLE";
                case 3:
                return "TETRIS";
                case 4:
                return "MINI T-SPIN SINGLE";
                case 5:
                return "MINI T-SPIN DOUBLE";
                case 6:
                return "MINI T-SPIN TRIPLE";
                case 7:
                return "MINI T-SPINTETRIS";
                case 8:
                return "T-SPIN SINGLE";
                case 9:
                return "T-SPIN DOUBLE";
                case 10:
                return "T-SPIN TRIPLE";
                case 11:
                return "T-SPIN TETRIS";
                default:
                return "";
            }
        }
    }
    
    public static void main(String[] args) throws LoginException {
        //Initialize variables
        ready = false;
        keybinds = new HashMap<>();
        games = new HashMap<>();
        menuChannels = new HashMap<>();
        menus = new HashMap<>();
        shutdown = -1;
        broadcasted = new HashSet<>();
        leaderboard = new HashMap<>();
        prefixes = new HashMap<>();
        players = new HashMap<>();
        //Attempt to retrieve information from GitHub and Firestore
        try {
            String[] save = GitHubAPI.read("save.txt").split("\n");
            int index = 0;
            while (index < save.length && save[index].length() > 0) {
                long channel = Long.parseLong(save[index].substring(5));
                index++;
                String owner = save[index];
                index++;
                long lastUserId = Long.parseLong(save[index]);
                index++;
                int[] flags = {save[index].charAt(0) - '0', save[index].charAt(1) - '0', (save[index].charAt(2) - '0' + 3) % 6};
                Game game = new Game(owner, flags);
                game.lastUserId = lastUserId;
                index++;
                ArrayList<Tetris.Piece> queue = new ArrayList<>();
                for (int i = 0; i < save[index].length(); i++) {
                    queue.add(Tetris.Piece.values()[save[index].charAt(i) - '0']);
                }
                game.tetris.fullQueue = queue;
                game.tetris.queue = new LinkedList<>(queue);
                game.addFrame(null, null);
                index++;
                while (index < save.length && !save[index].startsWith("Game ") && save[index].length() > 0) {
                    Tetris.Input input = Tetris.Input.values()[save[index].charAt(0) - '0'];
                    String user = save[index].substring(1);
                    if (user.contains(" ")) {
                        String player = user.substring(user.indexOf(' ') + 1);
                        game.players.put(game.frames, player);
                        user = user.substring(0, user.indexOf(' '));
                    }
                    game.tetris.input(input);
                    game.addFrame(user, input);
                    index++;
                }
                games.put(channel, game);
                if (index < save.length && save[index].length() == 0) {
                    break;
                }
            }
            index++;
            while (index < save.length && save[index].length() > 0) {
                long user = Long.parseLong(save[index].substring(8));
                HashMap<String, Tetris.Input> keybindMap = new HashMap<>();
                index++;
                while (index < save.length && save[index].length() > 0 && !save[index].startsWith("Keybind ")) {
                    keybindMap.put(save[index].substring(1), Tetris.Input.values()[save[index].charAt(0) - '0']);
                    index++;
                }
                keybinds.put(user, keybindMap);
            }
            index++;
            while (index < save.length) {
                prefixes.put(Long.valueOf(save[index].split(" ")[0]), save[index].split(" ")[1]);
                index++;
            }
            Database.init();
            Map<String, Map<String, Object>> playerData = Database.read("Players");
            for (String player : playerData.keySet()) {
                players.put(player, Player.fromMap(playerData.get(player)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.setProperty("java.awt.headless", "true");
        //Attempt to read leaderboard information
        try {
            String[] lbFile = GitHubAPI.read("leaderboard.txt").split("\n");
            for (String entry : lbFile) {
                leaderboard.put(Long.valueOf(entry.split(" ")[0]), new Score(Integer.parseInt(entry.split(" ")[1]), entry.split(" ")[2]));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        //Initialize JDA
        try {
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
            .build()
            .awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
        ready = true;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //Owner commands
        if (event.getAuthor().getIdLong() == 722202264055447643L) {
            if (event.getMessage().getContentRaw().equals("!tetris shutdown")) {
                shutdown(event);
            }
            if (event.getMessage().getContentRaw().startsWith("!tetris announce")) {
                announce(event.getMessage().getContentRaw().substring(17));
            }
        }
        //Validate message as command
        if (event.getMember() == null || event.getAuthor().isBot()) {
            return;
        }
        String[] args;
        if (event.getMessage().getReferencedMessage() != null && event.getMessage().getReferencedMessage().getAuthor().getIdLong() == 1377027446783610890L) {
            args = event.getMessage().getContentRaw().split(" ");
        } else if (event.getMessage().getContentRaw().startsWith("!tetris ")) {
            args = event.getMessage().getContentRaw().substring(8).split(" ");
        } else if (prefixes.containsKey(event.getGuild().getIdLong()) && event.getMessage().getContentRaw().startsWith(prefixes.get(event.getGuild().getIdLong()))) {
            args = event.getMessage().getContentRaw().substring(prefixes.get(event.getGuild().getIdLong()).length()).split(" ");
        }
        else {
            return;
        }
        if (!ready) {
            event.getChannel().sendMessage("Please wait a moment for the bot to finish starting up!").queue();
            return;
        }
        //Broadcast announcements
        if (shutdown > 0 && !broadcasted.contains(event.getChannel().getIdLong())) {
            long seconds = (shutdown - System.currentTimeMillis()) / 1000;
            event.getChannel().sendMessage("ATTENTION: Tetris Bot will be going offline in " + seconds + "s for a maintenance break! Service may be interrupted for a few minutes.").queue();
            broadcasted.add(event.getChannel().getIdLong());
        }
        if (announcement != null && !broadcasted.contains(event.getChannel().getIdLong())) {
            event.getChannel().sendMessage("ANNOUNCEMENT: " + announcement).queue();
            broadcasted.add(event.getChannel().getIdLong());
        }
        if (args[0].length() == 0) {
            return;
        }
        //Run command
        if (!players.containsKey(event.getAuthor().getName())) {
            players.put(event.getAuthor().getName(), new Player(event.getAuthor().getId()));
        }
        if (args[0].equals("start")) {
            start(event, ((args.length > 1 && args[1].matches("^\\d+$"))) ? Integer.parseInt(args[1]) : -1);
        }
        if (args[0].equals("abort")) {
            abort(event);
        }
        if (args[0].equals("keybind")) {
            keybind(event, Arrays.copyOfRange(args, 1, args.length));
        }
        if (args[0].equals("leaderboard")) {
            leaderboard(event);
        }
        if (args[0].equals("prefix")) {
            if (args.length > 1) {
                prefix(event, args[1]);
            }
        }
        if (args[0].equals("profile")) {
            profile(event);
        }
        if (args[0].equals("version")) {
            version(event);
        }
        if (args[0].equals("help")) {
            help(event);
        }
        Tetris.Input input = stringToInput(args[0], event.getAuthor().getIdLong());
        if (input != null) {
            input(event, input, args.length > 1 && args[1].equals("-"));
        }
    }

    /**
     * Save all information to GitHub and Firestore and elegantly terminate.
     * @param event The event where shutdown was detected.
     */
    public void shutdown(MessageReceivedEvent event) {
        broadcasted = new HashSet<>();
        shutdown = System.currentTimeMillis() + 60000;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            StringBuilder save = new StringBuilder();
            for (long channel : games.keySet()) {
                save.append("Game ").append(channel);
                Game game = games.get(channel);
                save.append("\n").append(game.owner);
                save.append("\n").append(game.lastUserId);
                save.append("\n").append(game.react ? 1 : 0).append(game.consecutive).append(game.tetris.preview).append("\n");
                for (int i = 0; i < game.tetris.fullQueue.size(); i++) {
                    save.append(game.tetris.fullQueue.get(i).ordinal());
                }
                for (int i = 0; i < game.users.size(); i++) {
                    save.append("\n").append(game.tetris.inputs.get(i).ordinal()).append(game.users.get(i));
                    if (!game.players.isEmpty() && game.players.firstKey() == i) {
                        save.append(" ").append(game.players.pollFirstEntry().getValue());
                    }
                }
                save.append("\n");
            }
            for (long user : keybinds.keySet()) {
                save.append("\n").append("Keybind ").append(user);
                for (String keybind : keybinds.get(user).keySet()) {
                    save.append("\n").append(keybinds.get(user).get(keybind).ordinal()).append(keybind);
                }
            }
            save.append("\n");
            for (long server : prefixes.keySet()) {
                save.append("\n").append(server).append(" ").append(prefixes.get(server));
            }
            try {
                GitHubAPI.write("save.txt", save.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            StringBuilder lbSave = new StringBuilder();
            for (long channel : leaderboard.keySet()) {
                lbSave.append(channel).append(" ")
                .append(leaderboard.get(channel).value).append(" ")
                .append(leaderboard.get(channel).id).append("\n");
            }
            lbSave.deleteCharAt(lbSave.length() - 1);
            try {
                GitHubAPI.write("leaderboard.txt", lbSave.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (String player : players.keySet()) {
                try {
                    Database.write("Players", player, players.get(player).toMap());
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            event.getChannel().sendMessage("Finished shutting down").queue();
            System.exit(0);
        }, 1, BOT_ID == 1387542197879963648L ? TimeUnit.SECONDS : TimeUnit.MINUTES);
    }

    public void announce(String message) {
        broadcasted = new HashSet<>();
        announcement = message;
        System.out.println(message);
    }
    
    public void start(MessageReceivedEvent event, int code) {
        Game game = games.get(event.getChannel().getIdLong());
        if (game == null) {
            if (code < 0) {
                int[] flags = new int[FLAGS.length + 1];
                event.getChannel().sendMessage(startMenu(flags)).queue(sentMessage -> {
                    menuChannels.put(event.getChannel().getIdLong(), sentMessage.getIdLong());
                    menus.put(sentMessage.getIdLong(), flags);
                    sentMessage.addReaction(Emoji.fromUnicode("🔼")).queue();
                    sentMessage.addReaction(Emoji.fromUnicode("◀️")).queue();
                    sentMessage.addReaction(Emoji.fromUnicode("▶️")).queue();
                    sentMessage.addReaction(Emoji.fromUnicode("🔽")).queue();
                    sentMessage.addReaction(Emoji.fromUnicode("🆗")).queue();
                });
            }
            else {
                int[] flags = new int[FLAGS.length];
                for (int i = 0; i < FLAGS.length; i++) {
                    flags[i] = code % FLAGS[i].length;
                    code /= FLAGS[i].length;
                }
                event.getChannel().sendMessage("Starting game by " + event.getAuthor().getName() + "...").queue();
                games.put(event.getChannel().getIdLong(), new Game(event.getAuthor().getName(), flags));
                games.get(event.getChannel().getIdLong()).addFrame(null, null);
                menus.remove(menuChannels.remove(event.getChannel().getIdLong()));
                sendTetris(event.getChannel(), games.get(event.getChannel().getIdLong()), null, null, true);
            }
        }
        else {
            event.getChannel().sendMessage("Game already in progress!").queue();
        }
    }

    public void abort(MessageReceivedEvent event) {
        Game game = games.get(event.getChannel().getIdLong());
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

    public void keybind(MessageReceivedEvent event, String[] args) {
        if (args.length % 2 != 0) {
            event.getChannel().sendMessage("Incorrect number of arguments provided! (Please provide a list of input-string pairs)").queue();
        }
        else if (args.length == 0) {
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
            for (int i = 1; i < args.length; i += 2) {
                Tetris.Input input = stringToInput(args[i - 1], event.getAuthor().getIdLong());
                if (input != null) {
                    user.put(args[i], input);
                    msg.append(input).append(" set to ").append(args[i]).append("\n");
                }
            }
            event.getChannel().sendMessage(msg.toString()).queue();
        }
    }

    public void leaderboard(MessageReceivedEvent event) {
        TreeMap<Score, String> global = new TreeMap<>();
        TreeMap<Score, String> server = new TreeMap<>();
        for (long id : leaderboard.keySet()) {
            TextChannel channel = event.getJDA().getTextChannelById(id);
            String name = "UNKNOWN CHANNEL";
            if (channel == null) {
                ThreadChannel thread = event.getJDA().getThreadChannelById(id);
                if (thread != null) {
                    name = thread.getGuild().getName() + "/" + thread.getParentChannel().getName() + "/" + thread.getName();
                }
            }
            else {
                name = channel.getGuild().getName() + "/" + channel.getName();
            }
            if (channel != null && channel.getGuild().getIdLong() == event.getGuild().getIdLong()) {
                server.put(leaderboard.get(id), name);
            }
            global.put(leaderboard.get(id), name);
        }
        StringBuilder lbDisplay = new StringBuilder();
        lbDisplay.append("*Linked replays may no longer exist.*\n**Global Leaderboard**").append("\n");
        int index = 0;
        for (Score s : global.keySet()) {
            if (index == Math.min(global.size(), 10)) {
                break;
            }
            lbDisplay.append(index + 1)
            .append(". ");
            lbDisplay.append(global.get(s))
            .append(" | [")
            .append(s.value)
            .append("](http://tetris-bot-replays.web.app/?fileId=")
            .append(s.id)
            .append(") | ")
            .append(new Date((long) Long.parseLong(s.id.split("/")[1])).toString())
            .append("\n");
        }
        lbDisplay.append("**").append(event.getGuild().getName()).append(" Leaderboard**").append("\n");
        index = 0;
        for (Score s : server.keySet()) {
            if (index == Math.min(global.size(), 10)) {
                break;
            }
            lbDisplay.append(index + 1)
            .append(". ");
            lbDisplay.append(server.get(s))
            .append(" | ")
            .append(s.value)
            .append(" | ")
            .append(new Date((long) Long.parseLong(s.id.split("/")[1])).toString())
            .append("\n");
        }
        if (leaderboard.containsKey(event.getChannel().getIdLong())) {
            lbDisplay.append("**Channel High Score: **")
            .append(leaderboard.get(event.getChannel().getIdLong()).value)
            .append(" | ")
            .append(new Date(Long.parseLong(leaderboard.get(event.getChannel().getIdLong()).id.split("/")[1])).toString())
            .append("\n");
        }
        event.getChannel().sendMessage(lbDisplay.toString()).queue();
    }

    public void prefix(MessageReceivedEvent event, String prefix) {
        if (event.getMember().getPermissions().contains(Permission.MANAGE_SERVER)) {
            if (prefix.length() > 0) {
                prefixes.put(event.getGuild().getIdLong(), prefix);
                event.getChannel().sendMessage("Prefix for server " + event.getGuild().getName() + " updated to `" + prefix + "`.").queue();
            }
            else {
                event.getChannel().sendMessage("Prefix must be at least one character!").queue();
            }
        }
        else {
            event.getChannel().sendMessage("Only members with the Manage Server permission can change the bot prefix!").queue();
        }
    }

    public void profile(MessageReceivedEvent event) {
        event.getChannel().sendMessageEmbeds(players.get(event.getAuthor().getName()).display(event.getAuthor().getName())).queue();
    }

    public void version(MessageReceivedEvent event) {
        event.getChannel().sendMessage(VERSION).queue();
    }

    public void help(MessageReceivedEvent event) {
        event.getChannel().sendMessage(HELP).queue();
    }

    public void input(MessageReceivedEvent event, Tetris.Input input, boolean repeat) {
        Game game = games.get(event.getChannel().getIdLong());
        if (game == null) {
            event.getChannel().sendMessage("No game in progress!").queue();
        }
        else if (game.tetris.getValidMoves().contains(input)) {
            if (game.consecutive == 2 || event.getAuthor().getIdLong() != game.lastUserId) {
                int prevScore = game.tetris.score;
                int prevPlace = game.tetris.lastPlaced;
                boolean placed = false;
                if (repeat && input != Tetris.Input.CW && input != Tetris.Input.CCW && input != Tetris.Input.HARDDROP) {
                    while (game.tetris.getValidMoves().contains(input)) {
                        placed |= game.tetris.input(input);
                        game.addFrame(event.getMember().getEffectiveName(), input);
                    }
                }
                else {
                    placed = game.tetris.input(input);
                    game.addFrame(event.getMember().getEffectiveName(), input);
                }
                if (placed || game.consecutive == 0) {
                    game.lastUserId = event.getAuthor().getIdLong();
                }
                sendTetris(event.getChannel(), game, event.getMember().getEffectiveName(), input, true);
                if (game.tetris.lines >= 300 || !game.tetris.alive) {
                    finishGame(event, game);
                }
                updatePlayers(event.getAuthor().getName(), event.getChannel().getId(), game, placed ? prevPlace : -2, game.tetris.score - prevScore);
            }
            else {
                event.getChannel().sendMessage(event.getMember().getEffectiveName() + ", you already played the last move!").queue();
            }
        }
        else {
            event.getChannel().sendMessage(input + " is not a valid move!").queue();
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
            boolean remove = true;
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
                event.getChannel().sendMessage("Starting game by " + event.retrieveUser().complete().getName() + "...").queue();
                games.put(event.getChannel().getIdLong(), new Game(event.retrieveUser().complete().getName(), flags));
                sendTetris(event.getChannel(), games.get(event.getChannel().getIdLong()), null, null, true);
                menus.remove(event.getMessageIdLong());
                menuChannels.remove(event.getChannel().getIdLong());
                break;
                default:
                remove = false;
            }
            if (remove) {
                try {
                    event.getReaction().removeReaction(user).complete();
                } catch (InsufficientPermissionException e) {}
                event.retrieveMessage().complete().editMessage(startMenu(flags)).queue();
            }
            return;
        }
        Game game = games.get(event.getChannel().getIdLong());
        if (game == null) {
            return;
        }
        if (event.getMessageIdLong() == game.gameMessage.getIdLong()) {
            try {
                event.getReaction().removeReaction(user).complete();
            } catch (InsufficientPermissionException e) {}
            for (int i = 0; i < 7; i++) {
                if (event.getEmoji().getName().equals(Tetris.INPUT_EMOJIS[i])) {
                    Tetris.Input input = Tetris.Input.values()[i];
                    if (game.consecutive == 2 || user.getIdLong() != game.lastUserId) {
                        if (game.tetris.getValidMoves().contains(input)) {
                            int prevScore = game.tetris.score;
                            int prevPlace = game.tetris.lastPlaced;
                            boolean placed = game.tetris.input(input);
                            game.addFrame(event.getMember().getEffectiveName(), input);
                            if (placed || game.consecutive == 0) {
                                game.lastUserId = event.getUserIdLong();
                            }
                            sendTetris(event.getChannel(), game, event.retrieveMember().complete().getEffectiveName(), input, false);
                            if (game.tetris.lines >= 300 || !game.tetris.alive) {
                                finishGame(event, game);
                            }
                            updatePlayers(user.getName(), event.getChannel().getId(), game, placed ? prevPlace : -2, game.tetris.score - prevScore);
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
     * Update player stats after an input.
     * @param player Name of player that played input.
     * @param channelId ID of channel where input occurred.
     * @param game Game instance where input was played.
     * @param place Index of last frame where a piece was placed, or -2 if no
     * piece was placed.
     * @param scoreDiff Increase in score from input.
     */
    public static void updatePlayers(String player, String channelId, Game game, int place, int scoreDiff) {
        players.get(player).inputs++;
        players.get(player).points += scoreDiff;
        players.get(player).channels.add(channelId);
        if (place > -2) {
            double total = 0;
            Map<String, Integer> contributors = new HashMap<>();
            for (String p : game.players.tailMap(place + 1).values()) {
                total++;
                if (contributors.containsKey(p)) {
                    contributors.put(p, contributors.get(p) + 1);
                }
                else {
                    contributors.put(p, 1);
                }
            }
            for (String p : contributors.keySet()) {
                if (!p.equals(player)) {
                    players.get(p).assist += (long) (scoreDiff * contributors.get(p) / total);
                }
            }
        }
        if (game.tetris.message.length() > 0) {
            int clear = -1;
            if (game.tetris.message.contains("SINGLE")) {
                clear++;
            }
            if (game.tetris.message.contains("DOUBLE")) {
                clear += 2;
            }
            if (game.tetris.message.contains("TRIPLE")) {
                clear += 3;
            }
            if (game.tetris.message.contains("TETRIS")) {
                clear += 4;
            }
            if (clear > -1) {
                if (game.tetris.message.contains("T-SPIN")) {
                    clear += 8;
                }
                if (game.tetris.message.contains("MINI")) {
                    clear -= 4;
                }
                players.get(player).clears[clear]++;
            }
        }
        game.players.put(game.frames - 1, player);
    }

    /**
     * Cleans up and updates player information after a game is finished.
     * @param event The event detecting the finished game.
     * @param game The game that is completed.
     */
    public static void finishGame(GenericMessageEvent event, Game game) {
        int inputs = game.players.size();
        HashMap<String, Integer> frequency = new HashMap<>();
        for (String player : game.players.values()) {
            if (frequency.containsKey(player)) {
                frequency.put(player, frequency.get(player) + 1);
            }
            else {
                frequency.put(player, 1);
            }
        }
        StringBuilder highScores = new StringBuilder("New high score for: ");
        for (String player : frequency.keySet()) {
            if (frequency.get(player) * 2 * frequency.size() >= inputs) {
                players.get(player).completed++;
                if (game.tetris.score > players.get(player).highScore) {
                    players.get(player).highScore = game.tetris.score;
                    if (highScores.length() > 20) {
                        highScores.append(", ");
                    }
                    highScores.append(player);
                }
            }
        }
        if (highScores.length() > 20) {
            event.getChannel().sendMessage(highScores.toString()).queue();
        }
        saveReplay(event, game);
        games.remove(event.getChannel().getIdLong());
    }

    /**
     * Sends a message using emojis to display a Tetris game.
     * @param channel The channel to send the message.
     * @param game The Tetris.java instance in to display.
     */
    public static void sendTetris(MessageChannelUnion channel, Game game, String user, Tetris.Input input, boolean sendNew) {
        EmbedBuilder gameDisp = game.displayGame(user, input);
        if (sendNew || game.gameMessage == null) {
            channel.sendMessageEmbeds(gameDisp.build()).queue(sentMessage -> {
                game.gameMessage = sentMessage;
                if (game.react) {
                    for (int i = 0; i < 7; i++) {
                        sentMessage.addReaction(Emoji.fromUnicode(Tetris.INPUT_EMOJIS[i])).queue();
                    }
                }
            });
        }
        else {
            game.gameMessage.editMessageEmbeds(gameDisp.build()).queue(sentMessage -> {
                game.gameMessage = sentMessage;
            });
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
            if (game.tetris.preview == 3 && game.consecutive < 2 && (!leaderboard.containsKey(event.getChannel().getIdLong()) || leaderboard.get(event.getChannel().getIdLong()).value < game.tetris.score)) {
                leaderboard.put(event.getChannel().getIdLong(), new Score(game.tetris.score, id));
            }
            event.getChannel().sendMessage("Created replay with id `" + id + "`\nClick [here](http://tetris-bot-replays.web.app/?fileId=" + id + ") to view your replay.").queue();
        }
    }

    /**
     * Creates the text display for the start menu with given flag values.
     * @param flags The flag values to display.
     * @return A string that can be sent to Discord representing the start
     * menu.
     */
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
        if (flags[1] < 2 && flags[2] == 0) {
            menu.append("\n*Your game is eligible for leaderboard submission.*");
        }
        else {
            menu.append("\n*Your game will not be submitted to the leaderboard.*");
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
}