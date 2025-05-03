import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.Point;

public class LiveBoardClient extends JFrame {
    private ObjectOutputStream out;

    public LiveBoardClient(String serverIP, int port) throws IOException {
        setTitle("LiveBoard Client");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Socket socket = new Socket(serverIP, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        DrawArea drawArea = new DrawArea(out);
        add(drawArea);

        new Thread(() -> {
            try {
                Object input;
                while ((input = in.readObject()) != null) {
                    if (input instanceof Point) {
                        drawArea.addPoint((Point) input);
                    }
                }
            } catch (Exception e) {
                System.out.println("Disconnected from server.");
            }
        }).start();
    }

    public static void main(String[] args) throws IOException {
        String serverIP = JOptionPane.showInputDialog("Enter server IP (e.g. 127.0.0.1):");
        int port = Integer.parseInt(JOptionPane.showInputDialog("Enter server port (e.g. 12345):"));
        SwingUtilities.invokeLater(() -> {
            try {
                new LiveBoardClient(serverIP, port).setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

class DrawArea extends JPanel {
    private final ObjectOutputStream out;
    private final java.util.List<Point> points = new ArrayList<>();

    public DrawArea(ObjectOutputStream out) {
        this.out = out;
        setBackground(Color.WHITE);

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point point = e.getPoint();
                points.add(point);
                repaint();
                try {
                    out.writeObject(point);
                    out.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void addPoint(Point point) {
        points.add(point);
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Point p : points) {
            g.fillOval(p.x, p.y, 5, 5);
        }
    }
}