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
 * This class handles the Discord API and runs the game simulation using inputs
 * received from Discord users.
 */

public class Bot extends ListenerAdapter {
    static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN"); //If testing on your own, use your own bot token
    static final long BOT_ID = 1377027446783610890L; //replace with your bot's ID
    static final String VERSION = 
    "# v1.1: Leaderboards" +
    "\n- Leaderboards have been added. Top 10 global scores and top 3 server-wide scores will be saved. Use command `leaderboard` to view." +
    "\n- Games are now displayed on embeds. This will save on memory and make reaction-based gameplay much more streamlined." +
    "\n- Games and keybinds now save when bot goes restart. We really should have implemented this sooner..." +
    "\n- Our replay viewing website now allows pausing and frame stepping, as well as providing a much cleaner look." +
    "\n- Some bug fixes and behind-the-scenes work." +
    "\n## v1.1.1" +
    "\n- Changed board display on embeds slightly for consistency." +
    "\n- Leaderboard entries now link to their replays." +
    "\n- Some bug fixes and behind-the-scenes work.";
    static final String HELP =
    "**Tetris Bot commands:**" + "\n" +
    "* " + "`start {code}`: Enters the start menu for a new game in a channel. To quickstart, place the code of the desired config, generated from the start menu. 0 is default." + "\n" +
    "* " + "`abort`: aborts the current game in a channel." + "\n" +
    "* " + "`[input]`: plays the input, if valid. You can also enter any unambiguous prefix of the input, such as `l` for LEFT or `ha` for HARDDROP."+ "\n" +
    "* " + "`[input] -`: repeats the input until it is no longer valid. Only applies to LEFT, RIGHT, and SOFTDROP."+ "\n" +
    "* " + "`keybind {[input] [keybind]}`: sets the list of input-keybind pairs as your custom keybinds. For example, `keybind ha hd ho c ccw z` sets HARDDROP to hd, HOLD to c, and CCW to z. Custom keybinds are case sensitive." + "\n" +
    "* " + "`keybind`: displays your current keybinds set." + "\n" +
    "* " + "`leaderboard`: displays the current leaderboard." + "\n" +
    "* " + "`version`: Gets the current version of the game." + "\n" +
    "**You may send a command by using the \"!tetris\" prefix or by replying to any bot message in the same channel.**";
    static final String[][] FLAGS = {
        {"Off", "On"},
        {"Every input", "Every piece", "Off"},
        {"3", "4", "5", "0", "1", "2"}};
    static HashMap<Long, Long> menuChannels;
    static HashMap<Long, int[]> menus;
    static HashMap<Long, Game> games;
    static HashMap<Long, HashMap<String, Tetris.Input>> keybinds;
    static HashMap<Long, Score> leaderboard;
    static long shutdown;
    static HashSet<Long> broadcasted;
    static String announcement;
    static boolean ready;
    static class Score implements Comparable<Score> {
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
    
    public static void main(String[] args) throws LoginException {
        ready = false;
        keybinds = new HashMap<>();
        games = new HashMap<>();
        menuChannels = new HashMap<>();
        menus = new HashMap<>();
        shutdown = -1;
        broadcasted = new HashSet<>();
        leaderboard = new HashMap<>();
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
            while (index < save.length) {
                long user = Long.parseLong(save[index].substring(8));
                HashMap<String, Tetris.Input> keybindMap = new HashMap<>();
                index++;
                while (index < save.length && !save[index].startsWith("Keybind ")) {
                    keybindMap.put(save[index].substring(1), Tetris.Input.values()[save[index].charAt(0) - '0']);
                    index++;
                }
                keybinds.put(user, keybindMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.setProperty("java.awt.headless", "true");
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
        try {
            String[] lbFile = GitHubAPI.read("leaderboard.txt").split("\n");
            for (String entry : lbFile) {
                leaderboard.put(Long.valueOf(entry.split(" ")[0]), new Score(Integer.parseInt(entry.split(" ")[1]), entry.split(" ")[2]));
            }
        } catch (IOException e) {
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
        } else if (event.getMessage().getContentRaw().length() > 7 && event.getMessage().getContentRaw().substring(0, 8).equals("!tetris ")) {
            args = event.getMessage().getContentRaw().substring(8).split(" ");
        } else {
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
                }
                save.append("\n");
            }
            for (long user : keybinds.keySet()) {
                save.append("\n").append("Keybind ").append(user);
                for (String keybind : keybinds.get(user).keySet()) {
                    save.append("\n").append(keybinds.get(user).get(keybind).ordinal()).append(keybind);
                }
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
            event.getChannel().sendMessage("Finished shutting down").queue();
            System.exit(0);
        }, 1, TimeUnit.MINUTES);
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
                    if (game.consecutive == 2 || event.retrieveUser().complete().getIdLong() != game.lastUserId) {
                        if (game.tetris.getValidMoves().contains(input)) {
                            boolean placed = game.tetris.input(input);
                            game.addFrame(event.getMember().getEffectiveName(), input);
                            if (placed || game.consecutive == 0) {
                                game.lastUserId = event.getUserIdLong();
                            }
                            sendTetris(event.getChannel(), game, event.retrieveMember().complete().getEffectiveName(), input, false);
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
    public void sendTetris(MessageChannelUnion channel, Game game, String user, Tetris.Input input, boolean sendNew) {
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