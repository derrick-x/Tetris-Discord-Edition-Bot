import java.util.ArrayList;

public class Main {
    static ArrayList<Bot> bots;
    public static void main(String[] args) {
        bots = new ArrayList<Bot>();
    }

    /**
     * An abstract representation of the bot to be deployed to each channel.
     */
    static class Bot {
        Tetris game;
        Discord api;
        public Bot() {
            game = new Tetris();
            api = new Discord();
        }
    }
}