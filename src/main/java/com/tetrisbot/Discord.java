package com.tetrisbot;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Discord extends ListenerAdapter {
    public static void main(String[] args) throws LoginException {
        String token = "";
        JDABuilder.createDefault(token)
                .addEventListeners(new Discord())
                .build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (event.getMessage().getContentRaw().equalsIgnoreCase("!hi")) {
            event.getChannel().sendMessage("Hello, " + event.getAuthor().getName() + "!").queue();
        }
    }
}
