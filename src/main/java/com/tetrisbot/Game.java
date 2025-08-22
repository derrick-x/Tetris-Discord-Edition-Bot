package com.tetrisbot;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class Game {
    static final String[] PIECE_IDS = {
        "<:I_:1402070316875776132>",
        "<:J_:1402070319404941443>",
        "<:L_:1402070321015558144>",
        "<:O_:1402069642704326748>",
        "<:S_:1402069651135004704>",
        "<:T_:1402070322907447409>",
        "<:Z_:1402069661952114771>"
    };
    static final String[] TILES = {"⏹️", "🟦", "🟧", "🟨", "🟩", "🟪", "🟥", "⬛", "🔳"};

    Tetris tetris; //Tetris instance of game.
    String owner; //User that started the game.
    long lastUserId; //ID of last user that played an input.
    Message gameMessage; //Message where game is displayed.
    boolean react; //If reaction mode is enabled.
    int consecutive; //0 = no consecutive rules, 1 = switch every piece, 2 = switch every player
    int frames; //Number of frames in replay.
    List<String> users; //List of users corresponding to each frame.
    TreeMap<Integer, String> players; //List of players that played an input.
    StringBuilder replay; //String detailing the replay.

    public Game(String o, int[] flags) {
        tetris = new Tetris((flags[2] + 3) % 6);
        owner = o;
        lastUserId = -1;
        react = flags[0] == 1;
        gameMessage = null;
        consecutive = flags[1];
        frames = -1;
        users = new ArrayList<>();
        players = new TreeMap<>();
        replay = new StringBuilder();
        replay.append(tetris.preview);
    }

    /**
     * Converts the game sequence of a game into a replay and
     * stores it in GitHub.
     */
    public String saveReplay() {
        String replayId = owner + "/" + System.currentTimeMillis();
        try {
            GitHubAPI.write("replays/" + replayId + "-replay.txt", replay.toString());
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return replayId;
    }
    
    /**
     * Adds a frame to the current game's replay file.
     * @param user The user who played the input.
     * @param input The input played.
     */
    public void addFrame(String user, Tetris.Input input) {
        replay.append("\n");
        int[][] shape = Tetris.getShape(tetris.queue.get(0), tetris.rotation);
        for (int i = 0; i < 4; i++) {
            shape[i][0] += tetris.position[0];
            shape[i][1] += tetris.position[1];
        }
        int[][] shadow = tetris.getShadow();
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 10; x += 2) {
                int tile1 = tetris.board[y][x].ordinal();
                int tile2 = tetris.board[y][x + 1].ordinal();
                for (int i = 0; i < 4; i++) {
                    if (x == shadow[i][0] && y == shadow[i][1]) {
                        tile1 = 8;
                    }
                    if (x + 1 == shadow[i][0] && y == shadow[i][1]) {
                        tile2 = 8;
                    }
                }
                for (int i = 0; i < 4; i++) {
                    if (x == shape[i][0] && y == shape[i][1]) {
                        tile1 = tetris.queue.get(0).ordinal();
                    }
                    if (x + 1 == shape[i][0] && y == shape[i][1]) {
                        tile2 = tetris.queue.get(0).ordinal();
                    }
                }
                replay.append((char) (tile1 * 9 + tile2 + 33));
            }
        }
        for (int i = 1; i <= tetris.preview; i++) {
            replay.append((char) (tetris.queue.get(i).ordinal() + 33));
        }
        replay.append((char) (tetris.hold.ordinal() + 33));
        replay.append("~").append(tetris.score).append("~").append(tetris.lines).append("~").append(tetris.message).append("~");
        if (input != null) {
            replay.append(input.ordinal());
            replay.append(user);
        }
        if (user != null) {
            users.add(user);
        }
        frames++;
    }

    /**
     * Draws the current frame of the given game onto the Graphics component.
     * @param g The Graphics component to draw on.
     * @param game The game state to draw.
     * @param user The name of the user to display.
     * @param input The last input received by the game.
     */
    public EmbedBuilder displayGame(String user, Tetris.Input input) {
        EmbedBuilder gameDisp = new EmbedBuilder();
        if (user == null) {
            gameDisp.setTitle("Game started!");
        }
        else {
            gameDisp.setTitle(user + " played " + input);
        }
        String[][] board = new String[20][10];
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 10; x++) {
                board[y][x] = TILES[tetris.board[y][x].ordinal()];
            }
        }
        int[][] shape = tetris.getShadow();
        for (int i = 0; i < 4; i++) {
            if (shape[i][1] < 0) {
                continue;
            }
            board[shape[i][1]][shape[i][0]] = TILES[8];
        }
        shape = Tetris.getShape(tetris.queue.get(0), tetris.rotation);
        for (int i = 0; i < 4; i++) {
            if (shape[i][1] + tetris.position[1] < 0) {
                continue;
            }
            board[shape[i][1] + tetris.position[1]][shape[i][0] + tetris.position[0]] = TILES[tetris.queue.get(0).ordinal()];
        }
        StringBuilder boardDisp = new StringBuilder();
        if (tetris.position[1] > 0) {
            boardDisp.append("*(Top row omitted due to character constraints)*\n");
        }
        for (int y = tetris.position[1] > 0 ? 1 : 0; y < (tetris.position[1] > 0 ? 20 : 19); y++) {
            for (int x = 0; x < 10; x++) {
                boardDisp.append(board[y][x]);
            }
            boardDisp.append("\n");
        }
        if (tetris.position[1] == 0) {
            boardDisp.append("*(Bottom row omitted due to character constraints)*\n");
        }
        gameDisp.setDescription(boardDisp.toString());
        gameDisp.addField("HOLD", tetris.hold == Tetris.Piece.EMPTY ? "" : PIECE_IDS[tetris.hold.ordinal()], true);
        StringBuilder queueDisp = new StringBuilder();
        gameDisp.addField("CURRENT", PIECE_IDS[tetris.queue.get(0).ordinal()], true);
        for (int i = 1; i <= tetris.preview; i++) {
            queueDisp.append(PIECE_IDS[tetris.queue.get(i).ordinal()]).append(" ");
        }
        gameDisp.addField("NEXT", queueDisp.toString(), true);
        gameDisp.addField("SCORE", tetris.score + "", true);
        gameDisp.addField("LEVEL", Math.min(30, (tetris.lines / 10 + 1)) + "", true);
        gameDisp.addField("LINES", tetris.lines + "", true);
        List<Tetris.Input> validMoves = tetris.getValidMoves();
        StringBuilder validDisp = new StringBuilder();
        for (Tetris.Input i : validMoves) {
            validDisp.append(Tetris.INPUT_EMOJIS[i.ordinal()]);
        }
        gameDisp.addField("Valid moves", validDisp.toString(), false);
        if (tetris.message.length() > 0) {
            gameDisp.addField(tetris.message, "", false);
        }
        if (!tetris.alive) {
            gameDisp.addField("GAME OVER!", "", false);
            gameDisp.setColor(Color.RED);
        }
        else if (tetris.lines >= 300) {
            gameDisp.addField("GAME COMPLETE!", "", false);
            gameDisp.setColor(Color.GREEN);
        }
        return gameDisp;
    }
}
