package com.tetrisbot;

import java.util.HashMap;

import javax.security.auth.login.LoginException;

public class Main {
    static HashMap<Long, Bot> bots;
    public static void main(String[] args) throws LoginException {
        bots = new HashMap<>();
        Discord.init();
    }

    /**
     * An abstract representation of the bot to be deployed to each channel.
     */
    static class Bot {
        Tetris game;
        public Bot() {
            game = new Tetris();
        }
    }
}