package com.tetrisbot;

import java.util.HashMap;
import java.util.List;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot extends ListenerAdapter {
    static HashMap<Long, Tetris> games;
    
    public static void main(String[] args) throws LoginException {
        String token = "";
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
        if (event.getAuthor().isBot() || !event.getMessage().getContentRaw().substring(0, 8).equals("!tetris ")) {
            return;
        }
        String[] args = event.getMessage().getContentRaw().split(" ");
        Tetris game = games.get(event.getChannel().getIdLong());
        if (args[1].equals("start")) {
            if (game == null) {
                event.getChannel().sendMessage("Starting game...").queue();
                games.put(event.getChannel().getIdLong(), new Tetris());
                sendTetris(event.getChannel(), games.get(event.getChannel().getIdLong()));
            }
            else {
                event.getChannel().sendMessage("Game already in progress!").queue();
            }
        }
        if (args[1].equals("abort")) {
            if (game == null) {
                event.getChannel().sendMessage("No game in progress!").queue();
            }
            else {
                games.remove(event.getChannel().getIdLong());
                event.getChannel().sendMessage("Game aborted").queue();
            }
        }
        try {
            Tetris.Input input = Tetris.Input.valueOf(args[1].toUpperCase());
            if (game == null) {
                event.getChannel().sendMessage("No game in progress!").queue();
            }
            else if (game.getValidMoves().contains(input)) {
                game.input(input);
                event.getChannel().sendMessage(event.getAuthor().getName() + " played " + input).queue();
                sendTetris(event.getChannel(), game);
                if (game.lines >= 300 || !game.alive) {
                    games.remove(event.getChannel().getIdLong());
                }
            }
            else {
                event.getChannel().sendMessage(input + " is not a valid move!").queue();
            }
        } catch (IllegalArgumentException e) {

        }
    }

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
            message.append("\nGAME OVER!");
        }
        channel.sendMessage(message.toString()).queue();
    }
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
}
