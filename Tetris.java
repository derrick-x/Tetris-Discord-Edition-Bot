import java.util.*;

/**
 * This class simulates the Tetris game. Each instance can act as an
 * independent game. The constructor initializes the game states. The input()
 * method processes an input in the simulated game. The instance will also
 * save the piece and input sequence to allow replays.
 */

public class Tetris {
    static enum Piece {
        I, J, L, O, S, T, Z, EMPTY
    }
    static enum Input {
        HARDDROP, SOFTDROP, LEFT, RIGHT, CW, CCW, HOLD
    }
    static final int[][][] SHAPES = {
        {{-1, 0}, {0, 0}, {1, 0}, {2, 0}},
        {{-1, -1}, {-1, 0}, {0, 0}, {1, 0}},
        {{-1, 0}, {0, 0}, {1, 0}, {1, -1}},
        {{0, -1}, {0, 0}, {1, 0}, {1, -1}},
        {{-1, 0}, {0, 0}, {0, -1}, {1, -1}},
        {{-1, 0}, {0, 0}, {0, -1}, {1, 0}},
        {{-1, -1}, {0, -1}, {0, 0}, {1, 0}}
    };
    //Add static variables here as necessary

    /**
     * Returns a list of points that represent the given piece's shape.
     * @param piece The piece to get the shape of.
     * @return A 2D integer array consisting of points representing the
     * locations of the tiles relative to the piece's center.
     */
    static int[][] getShape(Piece piece) {
        return switch (piece) {
            case I -> SHAPES[0];
            case J -> SHAPES[1];
            case L -> SHAPES[2];
            case O -> SHAPES[3];
            case S -> SHAPES[4];
            case T -> SHAPES[5];
            case Z -> SHAPES[6];
            default -> null;
        };
    }

    /**
     * Returns the color of a given piece.
     * @param piece The piece to get the color of.
     * @return A haxadecimal integer representation of a 3-channel color.
     */
    public static int getColor(Piece piece) {
        return switch (piece) {
            case I -> 0x00ffff;
            case J -> 0x0000ff;
            case L -> 0x7fff00;
            case O -> 0xffff00;
            case S -> 0x00ff00;
            case T -> 0x7f00ff;
            case Z -> 0xff0000;
            default -> 0x000000;
        };
    }

    /**
     * Returns a bag of the seven tetrominoes as mapped in the class
     * description.
     * @return A list containing a random permutation of the pieces from the
     * Pieces enum.
     */
    public static List<Piece> sevenBag() {
        List<Piece> pieces = new ArrayList<Piece>();
        pieces.add(Piece.I);
        pieces.add(Piece.J);
        pieces.add(Piece.L);
        pieces.add(Piece.O);
        pieces.add(Piece.S);
        pieces.add(Piece.T);
        pieces.add(Piece.Z);
        for (int i = 6; i >= 0; i--) {
            Piece temp = pieces.get(i);
            int index = (int) (Math.random() * (i + 1));
            pieces.set(i, pieces.get(index));
            pieces.set(index, temp);
        }
        return pieces;
    }

    /**
     * Calculates the kick location of a rotation for a given piece and board
     * using the super rotation system.
     * @param board The current board state, without the rotating piece.
     * @param piece The piece to be rotated.
     * @param position The position of the center of the piece.
     * @param rotation The original rotation state of the piece.
     * @param clockwise True if the rotation is clockwise, false for
     * counterclockwise.
     * @return The position of the center of the piece after rotation. Returns
     * null if rotation is not possible with any kick.
     */
    public static int[] srs(Piece[][] board, Piece piece, int[] position, int rotation, boolean clockwise) {
        return null;
    }

    //Add static methods here as necessary

    int score;
    int combo;
    boolean b2b; //True if previous clear was a b2b clear
    int lines; //Level can be calculated as lines / 10 + 1
    ArrayList<Piece> queue; //Contains ALL pieces that appeared and will appear in order
    int queueIndex; //The index of the piece currently on the board
    Piece[][] board; //0 for empty, otherwise number that corresponds to the piece color
    Piece hold;
    int xPos;
    int yPos;
    int rotation; //0 for default, 1 for CW once, 2 for CW twice, 3 for CCW once
    ArrayList<Input> inputs; //The record of the input sequence
    //Add instance variables here as necessary

    public Tetris() {
        score = 0;
        combo = -1;
        b2b = false;
        queue = new ArrayList<>();
        queueIndex = 0;
        board = new Piece[20][10];
        hold = Piece.EMPTY;
        xPos = 4;
        yPos = 1;
        inputs = new ArrayList<>();
    }

    /**
     * Proceesses an input given in the parameter.
     * If the input is invalid, do nothing.
     * @param input An integer representing the input as mapped in the Input
     * enum.
     */
    public void input(int input) {

    }

    /**
     * Returns all the possible moves in the current state.
     * @return S list containing the numbers representing every valid input as
     * mapped in the Input enum.
     */
    public List<Integer> getValidMoves() {
        return null;
    }

    //Add instance methods here as necessary
}
