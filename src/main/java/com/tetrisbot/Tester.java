package com.tetrisbot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JFrame;

import org.json.JSONObject;

/**
 * Temporary class to test the Tetris.java code logic.
 */
public class Tester extends Canvas {
    static Tetris tetris;
    public static void main(String[] args) {
        saveReplay();
        if (true) {
            return;
        }
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
        shape = Tetris.getShape(tetris.queue.get(0), tetris.rotation);
        g.setColor(new Color(Tetris.getColor(tetris.queue.get(0))));
        for (int i = 0; i < 4; i++) {
            g.fillRect((shape[i][0] + tetris.position[0]) * 20 + 200, (shape[i][1] + tetris.position[1]) * 20 + 100, 20, 20);
        }
        for (int i = 1; i <= 3; i++) {
            shape = Tetris.getShape(tetris.queue.get(i), 0);
            g.setColor(new Color(Tetris.getColor(tetris.queue.get(i))));
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
        g.drawString("Alive: " + tetris.alive, 60, 280);
        g.drawString(tetris.message, 60, 300);
    }
    public static String saveReplay() {
        String id = "pentag" + System.currentTimeMillis();
        File gif = new File(id + "-replay.gif");
        try {
            FileImageOutputStream output = new FileImageOutputStream(gif);
            GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_RGB, 500, true);
            for (int i = 0; i < 1; i++) {
                writer.writeToSequence(new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB));
            }
            output.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        try {
            URL url = new URL("https://api.backblazeb2.com/b2api/v2/b2_authorize_account");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String credentials = Base64.getEncoder().encodeToString("2ecdb725a8f9:0043167423cf90cdc9cc155c9d076fc8557ab912d7".getBytes());
            conn.setRequestProperty("Authorization", "Basic " + credentials);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = in.lines().collect(Collectors.joining());
            JSONObject auth = new JSONObject(response);
            String apiUrl = auth.getString("apiUrl");
            String accountAuthToken = auth.getString("authorizationToken");
            URL uploadUrlReq = new URL(apiUrl + "/b2api/v2/b2_get_upload_url");
            HttpURLConnection uploadConn = (HttpURLConnection) uploadUrlReq.openConnection();
            uploadConn.setRequestMethod("POST");
            uploadConn.setRequestProperty("Authorization", accountAuthToken);
            uploadConn.setDoOutput(true);
            String bucketPayload = "{\"bucketId\":\"a2de7cbd8bc732d59a780f19\"}";
            uploadConn.getOutputStream().write(bucketPayload.getBytes());
            String uploadResp;
            try (BufferedReader up = new BufferedReader(new InputStreamReader(uploadConn.getInputStream()))) {
                uploadResp = up.lines().collect(Collectors.joining());
            }
            JSONObject uploadData = new JSONObject(uploadResp);
            String uploadUrl = uploadData.getString("uploadUrl");
            String uploadAuthToken = uploadData.getString("authorizationToken");
            byte[] data = Files.readAllBytes(gif.toPath());
            HttpURLConnection upload = (HttpURLConnection) new URL(uploadUrl).openConnection();
            upload.setRequestMethod("POST");
            upload.setRequestProperty("Authorization", uploadAuthToken);
            upload.setRequestProperty("X-Bz-File-Name", gif.getName());
            upload.setRequestProperty("Content-Type", "image/gif");
            upload.setRequestProperty("X-Bz-Content-Sha1", "do_not_verify");
            upload.setDoOutput(true);
            upload.getOutputStream().write(data);
            int responseCode = upload.getResponseCode();
            System.out.println("Upload result: " + responseCode);
            gif.delete();
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        
        return id;
    }
}