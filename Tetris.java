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
    static final int[][][] SRS = {
        {{0, 0}, {-1, 0}, {-1, -1}, {0, 2}, {-1, 2}}, //0->1
        {{0, 0}, {1, 0}, {1, 1}, {0, -2}, {1, -2}}, //1->2
        {{0, 0}, {1, 0}, {1, -1}, {0, 2}, {1, 2}}, //2->3
        {{0, 0}, {-1, 0}, {-1, 1}, {0, -2}, {-1, -2}}, //3->0
        {{0, 0}, {1, 0}, {1, -1}, {0, 2}, {1, 2}}, //0->3
        {{0, 0}, {1, 0}, {1, 1}, {0, -2}, {1, -2}},  //1->0
        {{0, 0}, {-1, 0}, {-1, -1}, {0, 2}, {-1, 2}}, //2->1
        {{0, 0}, {-1, 0}, {-1, 1}, {0, -2}, {-1, -2}} //3->2
    };
    static final int[][] I_SRS = {

    };
    //Add static variables here as necessary


    /**
     * Returns a copy of the given array. (Mainly just to be a helper method
     * for getShape())
     * @param array The original array.
     * @return A non-aliased copy of the original array.
     */
    static int[][] arrayCopy2D(int[][] array) {
        int[][] copy = new int[array.length][];
        for (int i = 0; i < array.length; i++) {
            copy[i] = new int[array[i].length];
            System.arraycopy(array[i], 0, copy[i], 0, array[i].length);
        }
        return copy;
    }
    
    /**
     * Returns a list of points that represent the given piece's shape.
     * @param piece The piece to get the shape of.
     * @return A 2D integer array consisting of points representing the
     * locations of the tiles relative to the piece's center.
     */
    static int[][] getShape(Piece piece, int rotation) {
        int[][] shape = null;
        switch (piece) {
            case I -> shape = arrayCopy2D(SHAPES[0]);
            case J -> shape = arrayCopy2D(SHAPES[1]);
            case L -> shape = arrayCopy2D(SHAPES[2]);
            case O -> shape = arrayCopy2D(SHAPES[3]);
            case S -> shape = arrayCopy2D(SHAPES[4]);
            case T -> shape = arrayCopy2D(SHAPES[5]);
            case Z -> shape = arrayCopy2D(SHAPES[6]);
            default -> {}
        }
        if (shape == null) { //This should never be true
            return shape;
        }
        shape = rotate(shape, rotation);
        return shape;
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
            case L -> 0xff7f00;
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
        //Fisher-Yates shuffle
        for (int i = 6; i >= 0; i--) {
            Piece temp = pieces.get(i);
            int index = (int) (Math.random() * (i + 1));
            pieces.set(i, pieces.get(index));
            pieces.set(index, temp);
        }
        return pieces;
    }

    /** TODO
     * Calculates the kick location of a rotation for a given piece and board
     * using the super rotation system.
     * @param board The current board state, without the rotating piece.
     * @param piece The piece to be rotated.
     * @param position The position of the center of the piece.
     * @param rotation The original rotation state of the piece.
     * @param clockwise True if the rotation is clockwise, false for
     * counterclockwise.
     * @return An array where the first two entries represent the position of
     * the center of the piece after rotation. The third entry is 1 if the kick
     * forces a full spin (level 3 or 4 kick), 0 otherwise. Returns null if
     * rotation is not possible with any kick.
     */
    public static int[] srs(Piece[][] board, Piece piece, int[] position, int rotation, boolean clockwise) {
        if (piece == Piece.O) {
            int[] srs = {0, 0, 0};
            return srs;
        }
        
        for (int i = 0; i < 5; i++) {
            if (piece == Piece.I) {
                //Eventually we need to handle I tetromino logic separately
            }
            if (!collide(board, piece, position[0] + SRS[rotation + (clockwise ? 0 : 4)][i][0], position[1] + SRS[rotation + (clockwise ? 0 : 4)][i][1], rotation + (clockwise ? 1 : 3))) {
                int[] srs = {SRS[rotation + (clockwise ? 0 : 4)][i][0], SRS[rotation + (clockwise ? 0 : 4)][i][1], i > 2 ? 1 : 0};
                return srs;
            }
        }
        return null;
    }

    /**
     * Checks if a given piece, position, and rotation will collide with a
     * filled tile in a given board.
     * @param board The current board state, without the rotating piece.
     * @param piece The piece to be rotated.
     * @param pos The position of the center of the piece.
     * @param rotation The rotation state of the piece.
     * @return True if the piece will collide with a filled tile in the board.
     */
    public static boolean collide(Piece[][] board, Piece piece, int[] pos, int rotation) {
        int[][] shape = getShape(piece, rotation);
        for (int i = 0; i < 4; i++) {
            //Out of bounds checks
            if (shape[i][0] + pos[0] < 0) {
                return true;
            }
            if (shape[i][0] + pos[0] > 9) {
                return true;
            }
            if (shape[i][1] + pos[1] > 19) {
                return true;
            }
            //Tile collision check
            if (shape[i][1] + pos[1] >= 0 && board[shape[i][1] + pos[1]][shape[i][0] + pos[0]] != Piece.EMPTY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a given piece, x position, y position, and rotation will collide with a
     * filled tile in a given board.
     * @param board The current board state, without the rotating piece.
     * @param piece The piece to be rotated.
     * @param xPos The x position of the center of the piece.
     * @param yPos The y position of the center of the piece.
     * @param rotation The rotation state of the piece.
     * @return True if the piece will collide with a filled tile in the board.
     */
    public static boolean collide(Piece[][] board, Piece piece, int xPos, int yPos, int rotation) {
        int[] pos = {xPos, yPos};
        return collide(board, piece, pos, rotation);
    }

    /**
     * Rotates an array of points (represented as arrays of length 2).
     * @param original The original array of points.
     * @param direction Direction of rotation: 1 = clockwise, 3 =
     * counterclockwise.
     * @return A copy of the array with the rotation applied.
     */
    static final int[][] rotate(int[][] original, int direction) {
        int[][] rotated = new int[original.length][2];
        for (int i = 0; i < original.length; i++) {
            rotated[i][0] = original[i][0];
            rotated[i][1] = original[i][1];
        }
        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < direction; j++) {
                int temp = rotated[i][0];
                rotated[i][0] = rotated[i][1] * -1;
                rotated[i][1] = temp;
            }
        }
        return rotated;
    }

    //Add static methods here as necessary

    int score;
    int combo;
    boolean b2b; //True if previous clear was a b2b clear
    int lines; //Level can be calculated as lines / 10 + 1
    ArrayList<Piece> queue; //Contains ALL pieces that appeared and will appear in order
    int queueIndex; //The index of the piece currently on the board
    Piece[][] board; //0 for empty, otherwise number that corresponds to the piece color, board[y][x]
    List<Piece[]> overflow; //Tiles that go over the top of the board
    Piece hold;
    boolean canHold;
    int[] position; //position = {x, y}
    int rotation; //0 for default, 1 for CW once, 2 for CW twice, 3 for CCW once
    int inputCount; //Total number of inputs since last downward movement - 15 consecutive inputs results in instant lock
    ArrayList<Input> inputs; //The record of the input sequence
    String message; //Any message to display, such as the type of line cleared
    int spinLevel; //Whether the last successful move was a rotation
    int lowest; //Lowest y position ever reached, used for instant lock condition
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
        overflow = new ArrayList<>();
        hold = Piece.EMPTY;
        canHold = true;
        position = new int[2];
        position[0] = 4;
        position[1] = 1;
        rotation = 0;
        inputCount = 0;
        inputs = new ArrayList<>();
        message = "";
        spinLevel = 0;
        lowest = 1;
    }

    /**
     * Processes an input given in the parameter. If the input is illegal, do
     * nothing.
     * @param input An integer representing the input as mapped in the Input
     * enum.
     */
    public void input(Input input) {
        List<Input> validMoves = getValidMoves();
        switch (input) {
            case HARDDROP -> {
                if (validMoves.contains(Input.HARDDROP)) {
                    place();
                    inputs.add(Input.HARDDROP);
                }
            }
            case SOFTDROP -> {
                if (validMoves.contains(Input.SOFTDROP)) {
                    position[1]++;
                    lowest = Math.max(position[1], lowest);
                    spinLevel = 0;
                    score++;
                    inputs.add(Input.SOFTDROP);
                }
            }
            case LEFT -> {
                if (validMoves.contains(Input.LEFT)) {
                    position[0]--;
                    inputCount++;
                    spinLevel = 0;
                    inputs.add(Input.LEFT);
                }
            }
            case RIGHT -> {
                if (validMoves.contains(Input.RIGHT)) {
                    position[0]++;
                    inputCount++;
                    spinLevel = 0;
                    inputs.add(Input.RIGHT);
                }
            }
            case CW -> {
                if (validMoves.contains(Input.CW)) {
                    int[] kick = srs(board, queue.get(queueIndex), position, rotation, true);
                    position[0] += kick[0];
                    position[1] += kick[1];
                    spinLevel = 1;
                    if (kick[2] == 1) {
                        spinLevel = 2;
                    }
                    rotation++;
                    rotation = rotation % 4;
                }
            }
            case CCW -> {
                if (validMoves.contains(Input.CCW)) {
                    int[] kick = srs(board, queue.get(queueIndex), position, rotation, false);
                    position[0] += kick[0];
                    position[1] += kick[1];
                    spinLevel = 1;
                    if (kick[2] == 1) {
                        spinLevel = 2;
                    }
                    rotation += 3;
                    rotation = rotation % 4;
                }
            }
            case HOLD -> {
                if (validMoves.contains(Input.HOLD)) {
                    canHold = false;
                    if (hold == Piece.EMPTY) {
                        hold = queue.remove(queueIndex);
                    }
                    else {
                        Piece temp = hold;
                        hold = queue.get(queueIndex);
                        queue.set(queueIndex, temp);
                    }
                    position[0] = 4;
                    position[1] = 1;
                    rotation = 0;
                    inputCount = 0;
                    spinLevel = 0;
                    lowest = 1;
                }
            }
            default -> {}
        }
        if (inputCount > 15) {
            place();
        }
    }

    /**
     * Places the piece currently on the board, updating the necessary
     * variables.
     */
    public void place() {
        //Move piece down until it collides
        while (!collide(board, queue.get(queueIndex), position, rotation)) {
            position[1]++;
            spinLevel = 0;
            score += 2;
        }
        position[1]--;
        score -= 2;
        //Fill tiles where the piece will be placed
        int[][] shape = getShape(queue.get(queueIndex), rotation);
        for (int i = 0; i < 4; i++) {
            if (shape[i][1] + position[1] < 0) {
                while (overflow.size() < 0 - shape[i][1] - position[1]) {
                    Piece[] emptyRow = new Piece[10];
                    Arrays.fill(emptyRow, Piece.EMPTY);
                    overflow.add(emptyRow);
                }
                overflow.get(-1 - shape[i][1] - position[1])[shape[i][0] + position[0]] = queue.get(queueIndex);
            }
            else {
                board[shape[i][1] + position[1]][shape[i][0] + position[0]] = queue.get(queueIndex);
            }
        }
        //Check for any spins (incomplete)
        if (spinLevel > 0) {
            int corners = 0;
            corners += (position[0] == 0 || position[1] == 0 || board[position[1] - 1][position[0] - 1] != Piece.EMPTY) ? 1 : 0;
            corners += (position[0] < 9 || position[1] == 0 || board[position[1] + 1][position[0] - 1] != Piece.EMPTY) ? 1 : 0;
            corners += (position[0] == 0 || position[1] < 19 || board[position[1] - 1][position[0] + 1] != Piece.EMPTY) ? 1 : 0;
            corners += (position[0] < 9 || position[1] < 19 || board[position[1] + 1][position[0] + 1] != Piece.EMPTY) ? 1 : 0;
            if (corners > 2) {
                spinLevel = 2;
            }
        }
        //Detect and clear filled lines
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
                    System.arraycopy(board[y2 - 1], 0, board[y2], 0, 10);
                }
                if (!overflow.isEmpty()) {
                    board[0] = Arrays.copyOf(overflow.remove(0), 10);
                }
                else {
                    Arrays.fill(board[0], Piece.EMPTY);
                }
                y++;
            }
        }
        //TODO: Add score for clearing lines
        //Update queue and add another bag if necessary
        queueIndex++;
        if (queueIndex + 5 > queue.size()) {
            queue.addAll(sevenBag());
        }
        //Reset piece state
        position[0] = 4;
        position[1] = 1;
        rotation = 0;
        canHold = true;
        inputCount = 0;
        spinLevel = 0;
        lowest = 1;
    }

    /**
     * Returns all the possible moves in the current state.
     * @return A list containing the numbers representing every valid input as
     * mapped in the Input enum.
     */
    public List<Input> getValidMoves() {
        List<Input> validMoves = new ArrayList<Input>(7);
        validMoves.add(Input.HARDDROP); //Hard drop is always valid
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
        return validMoves;
    }

    /**
     * Returns the upcoming pieces in the queue.
     * @param amount The amount of pieces to return in the queue.
     * @return A list containing the specified number of upcoming pieces in the
     * queue, if it does not exceed the amount of pieces added to the queue,
     * otherwise it returns as many pieces as it can.
     */
    public List<Piece> getNext(int amount) {
        return queue.subList(queueIndex, Math.min(queue.size(), queueIndex + amount));
    }

    /**
     * Returns the shadow of the current piece.
     * @return An array of [x, y] denoting the tiles of the shadow.
     */
    public int[][] getShadow() {
        int[] shadow = {position[0], position[1]};
        while (!collide(board, queue.get(queueIndex), shadow[0], shadow[1] + 1, rotation)) {
            shadow[1]++;
        }
        int[][] shape = getShape(queue.get(queueIndex), rotation);
        for (int i = 0; i < 4; i++) {
            shape[i][0] += shadow[0];
            shape[i][1] += shadow[1];
        }
        return shape;
    }

    //Add instance methods here as necessary
}