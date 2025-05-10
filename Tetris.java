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
     * @return A hexadecimal integer representation of a 3-channel color.
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
        //TODO
        return null;
    }

    /**
     * Checks if a given piece, position, and rotation will collide with a
     * filled tile in a given board.
     * @param board The current board state, without the rotating piece.
     * @param piece The piece to be rotated.
     * @param position The position of the center of the piece.
     * @param rotation The rotation state of the piece.
     * @return True if the piece will collide with a filled tile in the board.
     */
    public static boolean collide(Piece[][] board, Piece piece, int[] position, int rotation) {
        int[][] shape = getShape(piece);
        for (int i = 0; i < 4; i++) {
            if (shape[i][0] + position[0] < 0) {
                return true;
            }
            if (shape[i][0] + position[0] > 9) {
                return true;
            }
            if (shape[i][1] + position[1] < 0) {
                return true;
            }
            if (shape[i][1] + position[1] > 19) {
                return true;
            }
            if (board[shape[i][1] + position[1]][shape[i][0] + position[0]] != Piece.EMPTY) {
                return true;
            }
        }
        return false;
    }

    //Add static methods here as necessary

    int score;
    int combo;
    boolean b2b; //True if previous clear was a b2b clear
    int lines; //Level can be calculated as lines / 10 + 1
    ArrayList<Piece> queue; //Contains ALL pieces that appeared and will appear in order
    int queueIndex; //The index of the piece currently on the board
    Piece[][] board; //0 for empty, otherwise number that corresponds to the piece color, board[y][x]
    Piece hold;
    boolean canHold;
    int[] position; //position[0] = x, position[1] = y
    int rotation; //0 for default, 1 for CW once, 2 for CW twice, 3 for CCW once
    int inputCount; //Total number of inputs since last downward movement - 15 consecutive inputs results in instant lock
    ArrayList<Input> inputs; //The record of the input sequence
    String message; //Any message to display, such as the type of line cleared
    boolean isMini;
    //Add instance variables here as necessary

    public Tetris() {
        score = 0;
        combo = -1;
        b2b = false;
        queue = new ArrayList<>(sevenBag());
        queueIndex = 0;
        board = new Piece[20][10];
        for (int i = 0; i < 20; i++) {
            Arrays.fill(board[i], Piece.EMPTY);
        }
        hold = Piece.EMPTY;
        canHold = true;
        position = new int[2];
        position[0] = 4;
        position[1] = 1;
        inputCount = 0;
        inputs = new ArrayList<>();
        message = "";
        isMini = false;
    }

    /**
     * Processes an input given in the parameter. If the input is illegal, do
     * nothing.
     * @param input An integer representing the input as mapped in the Input
     * enum.
     */
    public void input(Input input) {
        List<Input> validMoves = getValidMoves();
        //TODO
        switch (input) {
            case HARDDROP:
            if (validMoves.contains(Input.HARDDROP)) {

            }
            break;
            case SOFTDROP:
            if (validMoves.contains(Input.SOFTDROP)) {
                
            }
            break;
            case LEFT:
            if (validMoves.contains(Input.LEFT)) {
                
            }
            break;
            case RIGHT:
            if (validMoves.contains(Input.RIGHT)) {
                
            }
            break;
            case CW:
            if (validMoves.contains(Input.CW)) {
                
            }
            break;
            case CCW:
            if (validMoves.contains(Input.CCW)) {
                
            }
            break;
            case HOLD:
            if (validMoves.contains(Input.HOLD)) {
                
            }
            break;
            default:
        }
    }

    /**
     * Places the piece currently on the board, updating the necessary
     * variables.
     */
    public void place() {
        while (!collide(board, queue.get(queueIndex), position, rotation)) {
            position[1]++;
        }
        position[1]--;
        int[][] shape = getShape(queue.get(queueIndex));
        for (int i = 0; i < 4; i++) {
            board[shape[i][1] + position[1]][shape[i][0] + position[0]] = queue.get(queueIndex);
        }
        //TODO: detect t-spin before clearing lines
        boolean spin = false;
        if (inputs.get(inputs.size() - 1) == Input.CW || inputs.get(inputs.size() - 1) == Input.CCW) {
            int corners = 0;
            corners += (position[0] == 0 || position[1] == 0 || board[position[1] - 1][position[0] - 1] != Piece.EMPTY) ? 1 : 0;
            corners += (position[0] < 9 || position[1] == 0 || board[position[1] + 1][position[0] - 1] != Piece.EMPTY) ? 1 : 0;
            corners += (position[0] == 0 || position[1] < 19 || board[position[1] - 1][position[0] + 1] != Piece.EMPTY) ? 1 : 0;
            corners += (position[0] < 9 || position[1] < 19 || board[position[1] + 1][position[0] + 1] != Piece.EMPTY) ? 1 : 0;
            if (corners > 2) {
                spin = true;
            }
        }
        int cleared = 0;
        for (int y = 19; y >= 0; y--) {
            boolean filled = true;
            for (int x = 0; x < 10; x++) {
                if (board[y][x] == Piece.EMPTY) {
                    filled = false;
                    break;
                }
            }
            if (filled) {
                cleared++;
                for (int y2 = y; y2 > 0; y2--) {
                    System.arraycopy(board[y - 1], 0, board[y], 0, 10);
                }
                Arrays.fill(board[0], Piece.EMPTY);
            }
        }
        //TODO
        canHold = true;
        queueIndex++;
        if (queueIndex + 5 > queue.size());
        List<Piece> newBag = sevenBag();
        for (int i = 0; i < 7; i++) {
            queue.add(newBag.get(i));
        }
    }

    /**
     * Returns all the possible moves in the current state.
     * @return S list containing the numbers representing every valid input as
     * mapped in the Input enum.
     */
    public List<Input> getValidMoves() {
        List<Input> validMoves = new ArrayList<Input>(7);
        validMoves.add(Input.HARDDROP); //Hard drop is always valid
        //TODO
        //Check soft drop
        int[] softdrop = {position[0], position[1] + 1};
        if (!collide(board, queue.get(queueIndex), softdrop, rotation)) {
            validMoves.add(Input.SOFTDROP);
        }
        //Check move left
        int[] moveleft = {position[0] - 1, position[1]};
        if (!collide(board, queue.get(queueIndex), moveleft, rotation)) {
            validMoves.add(Input.LEFT);
        }
        //Check move right
        int[] moveright = {position[0] + 1, position[1]};
        if (!collide(board, queue.get(queueIndex), moveright, rotation)) {
            validMoves.add(Input.RIGHT);
        }
        //Check rotate cw
        if (srs(board, queue.get(queueIndex), position, rotation, true) != null) {
            validMoves.add(Input.CW);
        }
        //Check rotate ccw
        if (srs(board, queue.get(queueIndex), position, rotation, false) != null) {
            validMoves.add(Input.CCW);
        }
        //Check hold
        if (canHold) {
            validMoves.add(Input.HOLD);
        }
        return null;
    }

    //Add instance methods here as necessary
}
