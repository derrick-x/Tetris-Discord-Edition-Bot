import java.awt.*;
import javax.swing.JFrame;

/**
 * Temporary class to test the Tetris.java code logic.
 */
public class Tester extends Canvas {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Canvas canvas = new Tester();
        frame.add(canvas);
        canvas.repaint();
        frame.setVisible(true);
        Tetris tetris = new Tetris();
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 600, 600);
    }
}
