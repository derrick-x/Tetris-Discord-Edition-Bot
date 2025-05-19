import java.util.*;

/**
 * Temporary class to test the Tetris.java code logic.
 */
public class Tester {
    public static void main(String[] args) {
        Tetris tetris = new Tetris();
        Scanner scan = new Scanner(System.in);
        while (true) { 
            List<Tetris.Piece> pieces = tetris.getNext(4);
            int[][] currPiece = Tetris.getShape(pieces.get(0));
            int[][] shadow = tetris.getShadow();
            System.out.println("------------");
            for (int y = 0; y < 20; y++) {
                System.out.print('|');
                for (int x = 0; x < 10; x++) {
                    if (tetris.board[y][x] != Tetris.Piece.EMPTY) {
                        System.out.print('X');
                    }
                    else if (currPiece[0][0] + tetris.position[0] == x && currPiece[0][1] + tetris.position[1] == y) {
                        System.out.print('O');
                    }
                    else if (currPiece[1][0] + tetris.position[0] == x && currPiece[1][1] + tetris.position[1] == y) {
                        System.out.print('O');
                    }
                    else if (currPiece[2][0] + tetris.position[0] == x && currPiece[2][1] + tetris.position[1] == y) {
                        System.out.print('O');
                    }
                    else if (currPiece[3][0] + tetris.position[0] == x && currPiece[3][1] + tetris.position[1] == y) {
                        System.out.print('O');
                    }
                    else if (shadow[0][0] == x && shadow[0][1] == y) {
                        System.out.print('?');
                    }
                    else if (shadow[1][0] == x && shadow[1][1] == y) {
                        System.out.print('?');
                    }
                    else if (shadow[2][0] == x && shadow[2][1] == y) {
                        System.out.print('?');
                    }
                    else if (shadow[3][0] == x && shadow[3][1] == y) {
                        System.out.print('?');
                    }
                    else {
                        System.out.print(' ');
                    }
                }
                System.out.println('|');
            }
            System.out.println("------------");
            System.out.println("Next: " + pieces.subList(1, 4).toString());
            System.out.println("Hold: " + tetris.hold);
            List<Tetris.Input> inputs = tetris.getValidMoves();
            for (int i = 0; i < inputs.size(); i++) {
                System.out.println(i + ": " + inputs.get(i));
            }
            System.out.print("Your move: ");
            int input = scan.nextInt();
            tetris.input(inputs.get(input));
        }
    }
}
