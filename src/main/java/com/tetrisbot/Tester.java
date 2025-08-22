package com.tetrisbot;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;

/**
 * Temporary class to test stuff.
 */
public class Tester extends Canvas {
    static Tetris tetris;
    public static void main(String[] args) throws Exception {
        Database.init();
        Bot.Player player = new Bot.Player("");
        Map<String, Object> map = player.toMap();
        Database.write("Players", "player_name", map);
        Map<String, Object> read = Database.read("Players", "player_name");
        Bot.Player test = Bot.Player.fromMap(read);
        System.exit(0);
        new Scanner(System.in).nextLine();
        JFrame frame = new JFrame();
        frame.setSize(500, 520);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Canvas canvas = new Tester();
        frame.add(canvas);
        canvas.repaint();
        frame.setVisible(true);
        tetris = new Tetris(3);

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
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(3));
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, 500, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(150, 50, 200, 400);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 10; x++) {
                g2d.setColor(new Color(Tetris.getColor(tetris.board[y][x], false)));
                g2d.fillRect(x * 20 + 150, y * 20 + 50, 20, 20);
            }
        }
        int[][] shape = tetris.getShadow();
        g2d.setColor(Color.GRAY);
        for (int i = 0; i < 4; i++) {
            g2d.fillRect(shape[i][0] * 20 + 150, shape[i][1] * 20 + 50, 20, 20);
        }
        shape = Tetris.getShape(tetris.queue.get(0), tetris.rotation);
        g2d.setColor(new Color(Tetris.getColor(tetris.queue.get(0), true)));
        for (int i = 0; i < 4; i++) {
            g2d.fillRect((shape[i][0] + tetris.position[0]) * 20 + 150, (shape[i][1] + tetris.position[1]) * 20 + 50, 20, 20);
        }
        for (int i = 1; i <= 3; i++) {
            shape = Tetris.getShape(tetris.queue.get(i), 0);
            g2d.setColor(new Color(Tetris.getColor(tetris.queue.get(i), false)));
            for (int j = 0; j < 4; j++) {
                g2d.fillRect(shape[j][0] * 20 + 410, shape[j][1] * 20 + 50 + 60 * i, 20, 20);
            }
        }
        if (tetris.hold != Tetris.Piece.EMPTY) {
            shape = Tetris.getShape(tetris.hold, 0);
            g2d.setColor(new Color(Tetris.getColor(tetris.hold, false)));
            for (int i = 0; i < 4; i++) {
                g2d.fillRect(shape[i][0] * 20 + 60, shape[i][1] * 20 + 100, 20, 20);
            }
        }
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(Color.DARK_GRAY);
        for (int y = 1; y < 20; y++) {
            g2d.drawLine(150, y * 20 + 50, 350, y * 20 + 50);
        }
        for (int x = 1; x < 10; x++) {
            g2d.drawLine(150 + x * 20, 50, 150 + x * 20, 450);
        }
        g2d.setColor(Color.WHITE);
        /*
        if (user != null) {
            g2d.drawString(user + " played " + input, 10, 30);
        }
        */
        g2d.drawString("Score", 10, 200);
        g2d.drawString(tetris.score + "", 10, 230);
        g.drawString("Level", 10, 280);
        g2d.drawString((tetris.lines / 10 + 1) + "", 10, 310);
        g2d.drawString("Lines", 10, 360);
        g2d.drawString(tetris.lines + "", 10, 390);
        g2d.setFont(new Font("Arial", Font.PLAIN, 15));
        g2d.drawString(tetris.message, 10, 480);
    }
}