import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import javax.swing.JFrame;

/**
 * Temporary class to test the Tetris.java code logic.
 */
public class Tester extends Canvas {
    static Tetris tetris;
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Canvas canvas = new Tester();
        frame.add(canvas);
        canvas.repaint();
        frame.setVisible(true);
        tetris = new Tetris();

        frame.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 32) {
                    tetris.input(Tetris.Input.HARDDROP);
                }
                if (e.getKeyCode() == 40) {
                    tetris.input(Tetris.Input.SOFTDROP);
                }
                if (e.getKeyCode() == 37) {
                    tetris.input(Tetris.Input.LEFT);
                }
                if (e.getKeyCode() == 39) {
                    tetris.input(Tetris.Input.RIGHT);
                }
                if (e.getKeyCode() == 38) {
                    tetris.input(Tetris.Input.CW);
                }
                if (e.getKeyCode() == 90) {
                    tetris.input(Tetris.Input.CCW);
                }
                if (e.getKeyCode() == 67) {
                    tetris.input(Tetris.Input.HOLD);
                }
                canvas.paint(canvas.getGraphics());
            }

            @Override
            public void keyReleased(KeyEvent e) {}
            
        });
    }

    @Override
    public void paint(Graphics g) {
        List<Tetris.Piece> next = tetris.getNext(4);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 600, 600);
        g.setColor(Color.WHITE);
        g.drawRect(200, 100, 200, 400);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 10; x++) {
                g.setColor(new Color(Tetris.getColor(tetris.board[y][x])));
                g.fillRect(x * 20 + 200, y * 20 + 100, 20, 20);
            }
        }
        int[][] shape = tetris.getShadow();
        g.setColor(Color.GRAY);
        for (int i = 0; i < 4; i++) {
            g.fillRect(shape[i][0] * 20 + 200, shape[i][1] * 20 + 100, 20, 20);
        }
        shape = Tetris.getShape(next.get(0), tetris.rotation);
        g.setColor(new Color(Tetris.getColor(next.get(0))));
        for (int i = 0; i < 4; i++) {
            g.fillRect((shape[i][0] + tetris.position[0]) * 20 + 200, (shape[i][1] + tetris.position[1]) * 20 + 100, 20, 20);
        }
        for (int i = 1; i <= 3; i++) {
            shape = Tetris.getShape(next.get(i), 0);
            g.setColor(new Color(Tetris.getColor(next.get(i))));
            for (int j = 0; j < 4; j++) {
                g.fillRect(shape[j][0] * 20 + 460, shape[j][1] * 20 + 100 + 60 * i, 20, 20);
            }
        }
        if (tetris.hold != Tetris.Piece.EMPTY) {
            shape = Tetris.getShape(tetris.hold, 0);
            g.setColor(new Color(Tetris.getColor(tetris.hold)));
            for (int i = 0; i < 4; i++) {
                g.fillRect(shape[i][0] * 20 + 60, shape[i][1] * 20 + 100, 20, 20);
            }
        }
        g.setColor(Color.WHITE);
        g.drawString("Score: " + tetris.score, 60, 200);
        g.drawString("Lines: " + tetris.lines, 60, 220);
        g.drawString("Combo: " + tetris.combo, 60, 240);
        g.drawString("Back-to-back: " + tetris.b2b, 60, 260);
    }
}