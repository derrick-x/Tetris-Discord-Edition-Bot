import java.util.*;

/**
 * Temporary class to test the Tetris.java code logic.
 */
public class Tester {
    public static void main(String[] args) {
        Tetris tetris = new Tetris();
        Scanner scan = new Scanner(System.in);
        List<Tetris.Piece> pieces = tetris.getNext(4);
        while (true) { 
            int[][] currPiece = Tetris.getShape(pieces.get(0));
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
                    else {
                        System.out.print(' ');
                    }
                }
                System.out.println('|');
            }
            System.out.println("------------");
            System.out.println("Next: " + pieces.subList(1, 4).toString());
            System.out.println("Hold: " + tetris.hold);
            System.out.print("Your move: ");
            String input = scan.nextLine();
        }
    }
}
