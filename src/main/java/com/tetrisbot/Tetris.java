package com.tetrisbot;

import java.util.*;

/**
 * This class simulates the Tetris game. Each instance can act as an
 * independent game. The constructor initializes the game states. The input()
 * method processes an input in the simulated game. The instance will also
 * save the piece and input sequence to allow replays.
 */

public class Tetris {
    static enum Piece {
        I, J, L, O, S, T, Z, EMPTY, SHADOW
    }
    static enum Input {
        HARDDROP, SOFTDROP, LEFT, RIGHT, CW, CCW, HOLD
    }
    static final String[] INPUT_EMOJIS = {"⏬", "⬇️", "⬅️", "➡️", "↩️", "↪️", "🔄"};
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

    /*
    (+1, 0)	(-1, 0)	(+2, 0)	(-1,+1)	(+2,-2)
    (0, +1)	(-1,+1)	(+2, 1)	(-1,-1)	(+2,+2)
    (-1, 0)	(+1, 0)	(-2, 0)	(+1,-1)	(-2,+2)
    (0, -1)	(+1,-1)	(-2,-1)	(+1,+1)	(-2,-2)
    (0, +1)	(-1,+1)	(+2,+1)	(-1,-1)	(+2,+2)
    (-1, 0)	(+1, 0)	(-2, 0)	(+1,-1)	(-2,+2)
    (0, -1)	(+1,-1)	(-2,-1)	(+1,+1)	(-2,-2)
    (+1, 0)	(-1, 0)	(+2, 0)	(-1,+1)	(+2,-2)
     */
    static final int[][][] I_SRS = {
        {{1, 0}, {-1, 0}, {2, 0}, {-1, 1}, {2, -2}}, //0->1
        {{0, 1}, {-1, 1}, {2, 1}, {-1, -1}, {2, 2}}, //1->2
        {{-1, 0}, {1, 0}, {-2, 0}, {1, -1}, {-2, 2}}, //2->3
        {{0, -1}, {1, -1}, {-2, -1}, {1, 1}, {-2, -2}}, //3->0
        {{0, 1}, {-1, 1}, {2, 1}, {-1, -1}, {2, 2}}, //0->3
        {{-1, 0}, {1, 0}, {-2, 0}, {1, -1}, {-2, 2}}, //1->0
        {{0, -1}, {1, -1}, {-2, -1}, {1, 1}, {-2, -2}}, //2->1
        {{1, 0}, {-1, 0}, {2, 0}, {-1, 1}, {2, -2}} //3->2
    };
    static final int[][][] SCORE_TABLE = { //Entries with -1 should never occur
        {{0, 100, 300, 500, 800}, {0, -1, -1, -1, 1200}}, //normal, b2b normal
        {{100, 200, 400, -1, -1}, {100, 300, 600, -1, -1}}, //mini spin, b2b mini spin
        {{400, 800, 1200, 1600, -1}, {400, 1200, 1800, 2400, -1}}, //spin, b2b spin
        {{-1, 900, 1500, 2300, 2800}, {-1, -1, -1, -1, 4400}} // all clear, b2b all clear
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
            case I: shape = arrayCopy2D(SHAPES[0]); break;
            case J: shape = arrayCopy2D(SHAPES[1]); break;
            case L: shape = arrayCopy2D(SHAPES[2]); break;
            case O: shape = arrayCopy2D(SHAPES[3]); break;
            case S: shape = arrayCopy2D(SHAPES[4]); break;
            case T: shape = arrayCopy2D(SHAPES[5]); break;
            case Z: shape = arrayCopy2D(SHAPES[6]); break;
            default: {}
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
    public static int getColor(Piece piece, boolean bright) {
        switch (piece) {
            case I: return bright ? 0x7fffff : 0x00ffff;
            case J: return bright ? 0x3f3fff : 0x0000ff;
            case L: return bright ? 0xffbf3f : 0xff7f00;
            case O: return bright ? 0xffff73 : 0xffff00;
            case S: return bright ? 0x3fff3f : 0x00ff00;
            case T: return bright ? 0xbf3fff : 0x7f00ff;
            case Z: return bright ? 0xff3f3f : 0xff0000;
            default: return bright ? 0x3f3f3f : 0x000000;
        }
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
    public static int[] srs(Piece[][] board, List<Piece[]> overflow, Piece piece, int[] position, int rotation, boolean clockwise) {
        if (piece == Piece.O) {
            int[] srs = {0, 0, 0};
            return srs;
        }
        
        for (int i = 0; i < 5; i++) {
            if (piece == Piece.I) {
                if (!collide(board, overflow, piece, position[0] + I_SRS[rotation + (clockwise ? 0 : 4)][i][0], position[1] + I_SRS[rotation + (clockwise ? 0 : 4)][i][1], rotation + (clockwise ? 1 : 3))) {
                    int[] srs = {I_SRS[rotation + (clockwise ? 0 : 4)][i][0], I_SRS[rotation + (clockwise ? 0 : 4)][i][1], i > 2 ? 1 : 0};
                    return srs;
                }
            }
            if (!collide(board, overflow, piece, position[0] + SRS[rotation + (clockwise ? 0 : 4)][i][0], position[1] + SRS[rotation + (clockwise ? 0 : 4)][i][1], rotation + (clockwise ? 1 : 3))) {
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
    public static boolean collide(Piece[][] board, List<Piece[]> overflow, Piece piece, int[] pos, int rotation) {
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
            if (shape[i][1] + pos[1] < 0) {
                if (overflow.size() < 0 - shape[i][1] - pos[1] || overflow.get(-1 - shape[i][1] - pos[1])[shape[i][0] + pos[0]] == Piece.EMPTY) {

                }
            }
            else {
                if (board[shape[i][1] + pos[1]][shape[i][0] + pos[0]] != Piece.EMPTY) {
                    return true;
                }
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
    public static boolean collide(Piece[][] board, List<Piece[]> overflow, Piece piece, int xPos, int yPos, int rotation) {
        int[] pos = {xPos, yPos};
        return collide(board, overflow, piece, pos, rotation);
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
    int b2b; //True if previous clear was a b2b clear
    int lines; //Level can be calculated as lines / 10 + 1
    ArrayList<Piece> fullQueue; //Contains ALL pieces that appeared and will appear in order, unaffected by holding
    LinkedList<Piece> queue; //Contains the upcoming pieces in order, affected by holding
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
    boolean alive;
    //Add instance variables here as necessary

    public Tetris() {
        score = 0;
        combo = -1;
        b2b = -1;
        lines = 190;
        fullQueue = new ArrayList<>(sevenBag());
        queue = new LinkedList<>(fullQueue);
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
        alive = true;
    }
    public Tetris(String sequence) { //Construct a Tetris game for replays
        this();
        fullQueue.clear();
        for (int i = 0; i < sequence.length(); i++) {
            fullQueue.add(Piece.valueOf(sequence.substring(i, i + 1)));
        }
        queue = new LinkedList<>(fullQueue);
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
            case HARDDROP: {
                if (validMoves.contains(Input.HARDDROP)) {
                    place();
                    inputs.add(Input.HARDDROP);
                }
            } break;
            case SOFTDROP: {
                if (validMoves.contains(Input.SOFTDROP)) {
                    position[1]++;
                    lowest = Math.max(position[1], lowest);
                    spinLevel = 0;
                    score++;
                    inputCount = 0;
                    inputs.add(Input.SOFTDROP);
                    grav20();
                }
            } break;
            case LEFT: {
                if (validMoves.contains(Input.LEFT)) {
                    position[0]--;
                    spinLevel = 0;
                    inputCount++;
                    inputs.add(Input.LEFT);
                    grav20();
                }
            } break;
            case RIGHT: {
                if (validMoves.contains(Input.RIGHT)) {
                    position[0]++;
                    spinLevel = 0;
                    inputCount++;
                    inputs.add(Input.RIGHT);
                    grav20();
                }
            } break;
            case CW: {
                if (validMoves.contains(Input.CW)) {
                    if (queue.peek() != Piece.O) {
                        int[] kick = srs(board, overflow, queue.peek(), position, rotation, true);
                        position[0] += kick[0];
                        position[1] += kick[1];
                        spinLevel = 1;
                        if (kick[2] == 1) {
                            spinLevel = 2;
                        }
                        rotation++;
                        rotation = rotation % 4;
                    }
                    inputCount++;
                    inputs.add(Input.CW);
                    grav20();
                }
            } break;
            case CCW: {
                if (validMoves.contains(Input.CCW)) {
                    if (queue.peek() != Piece.O) {
                        int[] kick = srs(board, overflow, queue.peek(), position, rotation, false);
                        position[0] += kick[0];
                        position[1] += kick[1];
                        spinLevel = 1;
                        if (kick[2] == 1) {
                            spinLevel = 2;
                        }
                        rotation += 3;
                        rotation = rotation % 4;
                    }
                    inputCount++;
                    inputs.add(Input.CCW);
                    grav20();
                }
            } break;
            case HOLD: {
                if (validMoves.contains(Input.HOLD)) {
                    canHold = false;
                    if (hold == Piece.EMPTY) {
                        hold = queue.poll();
                    }
                    else {
                        Piece temp = hold;
                        hold = queue.peek();
                        queue.set(0, temp);
                    }
                    reset();
                    inputs.add(Input.HOLD);
                }
            } break;
            default: {}
        }
        if (inputCount >= 15 && !validMoves.contains(Input.SOFTDROP)) {
            place();
        }
    }

    /**
     * Places the piece currently on the board, updating the necessary
     * variables.
     */
    public void place() {
        //Move piece down until it collides
        message = "";
        while (!collide(board, overflow, queue.get(0), position[0], position[1] + 1, rotation)) {
            position[1]++;
            spinLevel = 0;
            score += 2;
        }
        //Fill tiles where the piece will be placed
        int[][] shape = getShape(queue.get(0), rotation);
        boolean inBounds = false;
        for (int i = 0; i < 4; i++) {
            if (shape[i][1] + position[1] < 0) {
                while (overflow.size() < 0 - shape[i][1] - position[1]) {
                    Piece[] emptyRow = new Piece[10];
                    Arrays.fill(emptyRow, Piece.EMPTY);
                    overflow.add(emptyRow);
                }
                overflow.get(-1 - shape[i][1] - position[1])[shape[i][0] + position[0]] = queue.get(0);
            }
            else {
                inBounds = true;
                board[shape[i][1] + position[1]][shape[i][0] + position[0]] = queue.get(0);
            }
        }
        if (!inBounds) {
            alive = false;
        }
        //Check for any spins
        if (spinLevel > 0) {
            boolean[] corners = new boolean[4];
            corners[0] = position[0] == 0 || position[1] == 0 || board[position[1] - 1][position[0] - 1] != Piece.EMPTY;
            corners[1] = position[0] == 0 || position[1] > 18 || board[position[1] + 1][position[0] - 1] != Piece.EMPTY;
            corners[2] = position[0] > 8 || position[1] == 0 || board[position[1] - 1][position[0] + 1] != Piece.EMPTY;
            corners[3] = position[0] > 8 || position[1] > 18 || board[position[1] + 1][position[0] + 1] != Piece.EMPTY;
            if ((corners[0] ? 1 : 0) + (corners[1] ? 1 : 0) + (corners[2] ? 1 : 0) + (corners[3] ? 1 : 0) > 2) {
                if (spinLevel == 1) {
                    switch (rotation) { //Check if front corners are filled
                        case 0: {
                            if (corners[0] && corners[2]) {
                                spinLevel = 2;
                            }
                        } break;
                        case 1: {
                            if (corners[2] && corners[3]) {
                                spinLevel = 2;
                            }
                        } break;
                        case 2: {
                            if (corners[1] && corners[3]) {
                                spinLevel = 2;
                            }
                        } break;
                        case 3: {
                            if (corners[0] && corners[1]) {
                                spinLevel = 2;
                            }
                        } break;
                        default: {} //Default case should never happen
                    }
                }
            }
            else {
                spinLevel = 0;
            }
        }
        if (queue.get(0) != Piece.T) {
            spinLevel = 0;
        }
        if (spinLevel == 1) {
            message = "MINI T-SPIN ";
        }
        if (spinLevel == 2) {
            message = "T-SPIN ";
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
        switch (cleared) {
            case 1:
            message += "SINGLE";
            break;
            case 2:
            message += "DOUBLE";
            break;
            case 3:
            message += "TRIPLE";
            break;
            case 4:
            message += "TETRIS";
            break;
            default:
        }
        if (cleared > 0) {
            combo++;
            if ((queue.get(0) == Piece.T && spinLevel > 0) || cleared > 3) {
                b2b++;
            }
            else {
                b2b = -1;
            }
            if (b2b > 0) {
                message = "BACK-TO-BACK " + message;
            }
        }
        else {
            combo = -1;
        }
        if (combo > 0) {
            message += " COMBO x" + combo;
        }
        boolean allclear = true;
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 10; x++) {
                if (board[y][x] != Piece.EMPTY) {
                    allclear = false;
                }
            }
        }
        if (allclear) {
            message = "ALL CLEAR " + message;
        }
        score += (SCORE_TABLE[allclear ? 3 : spinLevel][b2b > 0 ? 1 : 0][cleared] + Math.max(0, combo) * 50) * (lines / 10 + 1);
        if (cleared > 0 || spinLevel > 0) {
            message += " (+" + (SCORE_TABLE[allclear ? 3 : spinLevel][b2b > 0 ? 1 : 0][cleared] + Math.max(0, combo) * 50) * (lines / 10 + 1) + ")";
        }
        lines += cleared;
        //Update queue and add another bag if necessary
        queue.poll();
        reset();
        canHold = true;
    }

    /**
     * Returns all the possible moves in the current state.
     * @return A list containing the numbers representing every valid input as
     * mapped in the Input enum.
     */
    public List<Input> getValidMoves() {
        List<Input> validMoves = new ArrayList<>(7);
        validMoves.add(Input.HARDDROP); //Hard drop is always valid
        //Check soft drop
        int[] softdrop = {position[0], position[1] + 1};
        if (!collide(board, overflow, queue.get(0), softdrop, rotation)) {
            validMoves.add(Input.SOFTDROP);
        }
        //Check move left
        int[] moveleft = {position[0] - 1, position[1]};
        if (!collide(board, overflow, queue.get(0), moveleft, rotation)) {
            validMoves.add(Input.LEFT);
        }
        //Check move right
        int[] moveright = {position[0] + 1, position[1]};
        if (!collide(board, overflow, queue.get(0), moveright, rotation)) {
            validMoves.add(Input.RIGHT);
        }
        //Check rotate cw
        if (srs(board, overflow, queue.get(0), position, rotation, true) != null) {
            validMoves.add(Input.CW);
        }
        //Check rotate ccw
        if (srs(board, overflow, queue.get(0), position, rotation, false) != null) {
            validMoves.add(Input.CCW);
        }
        //Check hold
        if (canHold) {
            validMoves.add(Input.HOLD);
        }
        return validMoves;
    }

    /**
     * Returns the shadow of the current piece.
     * @return An array of [x, y] denoting the tiles of the shadow.
     */
    public int[][] getShadow() {
        int[] shadow = {position[0], position[1]};
        while (!collide(board, overflow, queue.get(0), shadow[0], shadow[1] + 1, rotation)) {
            shadow[1]++;
        }
        int[][] shape = getShape(queue.get(0), rotation);
        for (int i = 0; i < 4; i++) {
            shape[i][0] += shadow[0];
            shape[i][1] += shadow[1];
        }
        return shape;
    }

    /**
     * Resets the piece state when a new piece spawns.
     */
    public void reset() {
        position[0] = 4;
        position[1] = 1;
        rotation = 0;
        inputCount = 0;
        spinLevel = 0;
        lowest = 1;
        if (collide(board, overflow, queue.get(0), position, rotation)) {
            position[1]--;
            if (collide(board, overflow, queue.get(0), position, rotation)) {
                alive = false;
            }
        }
        if (queue.size() < 5) {
            List<Piece> newBag = sevenBag();
            for (int i = 0; i < 7; i++) {
                fullQueue.add(newBag.get(i));
                queue.add(newBag.get(i));
            }
        }
        grav20();
    }
    
    /**
     * Forces pieces down if 20g is reached.
     */
    public void grav20() {
        if (lines >= 190) {
            while (!collide(board, overflow, queue.get(0), position[0], position[1] + 1, rotation)) {
                position[1]++;
            }
        }
    }
    //Add instance methods here as necessary
}