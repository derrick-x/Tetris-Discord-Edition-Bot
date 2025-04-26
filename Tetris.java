import java.util.*;

/**
 * This class simulates the Tetris game. Each instance can act as an
 * independent game. The constructor initializes the game states. The input()
 * method processes an input in the simulated game. The instance will also
 * save the piece and input sequence to allow replays.
 * Piece mapping:
    * 0 = empty
    * 1 = I
    * 2 = J
    * 3 = L
    * 4 = O
    * 5 = S
    * 6 = T
    * 7 = Z
 * Input mapping:
    * 0 = hard drop
    * 1 = soft drop
    * 2 = left
    * 3 = right
    * 4 = CW rotation
    * 5 = CCW rotation
    * 6 = hold
 */

public class Tetris {
    //Add static variables here as necessary

    int score;
    int combo;
    boolean b2b; //True if previous clear was a b2b clear
    int lines; //Level can be calculated as lines / 10 + 1
    ArrayList<Integer> queue; //Contains ALL pieces that appeared and will appear in order
    int queueIndex; //The index of the piece currently on the board
    int[][] board; //0 for empty, otherwise number that corresponds to the piece color
    int hold; //0 for empty, otherwise number that corresponds to the piece
    int xPos;
    int yPos;
    int rotation; //0 for default, 1 for CW once, 2 for CW twice, 3 for CCW once
    ArrayList<Integer> inputs; //The record of the input sequence
    //Add instance variables here as necessary

    public Tetris() {
        score = 0;
        combo = -1;
        b2b = false;
        queue = new ArrayList<Integer>();
        queueIndex = 0;
        board = new int[20][10];
        hold = 0;
        xPos = 4;
        yPos = 1;
        inputs = new ArrayList<Integer>();
    }

    /**
     * Proceesses an input given in the parameter.
     * If the input is invalid, do nothing.
     * @param input An integer representing the input as mapped in the class
     * description.
     */
    public void input(int input) {

    }

    /**
     * Returns all the possible moves in the current state.
     * @return S list containing the numbers representing every valid input as
     * mapped in the class description.
     */
    public List<Integer> getValidMoves() {
        return null;
    }

    /**
     * Returns a bag of the seven tetrominoes as mapped in the class
     * description.
     * @return A list containing a random permutation of the numbers 1-7.
     */
    public static List<Integer> sevenBag() {
        List<Integer> pieces = new ArrayList<Integer>();
        for (int i = 1; i <= 7; i++) {
            pieces.add(i);
        }
        for (int i = 6; i >= 0; i--) {
            int temp = pieces.get(i);
            int index = (int) (Math.random() * (i + 1));
            pieces.set(i, pieces.get(index));
            pieces.set(index, temp);
        }
        return pieces;
    }
}
