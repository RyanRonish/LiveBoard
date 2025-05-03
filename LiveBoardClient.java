
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
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
                        Object finalInput = input;
                        SwingUtilities.invokeLater(() -> drawArea.addPoint((Point) finalInput));
                    }
                }
            } catch (Exception e) {
                System.err.println("Connection closed or error: " + e.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String serverIP = JOptionPane.showInputDialog("Enter Server IP Address:");
                String portStr = JOptionPane.showInputDialog("Enter Server Port:");
                int port = Integer.parseInt(portStr.trim());

                LiveBoardClient client = new LiveBoardClient(serverIP, port);
                client.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Failed to connect: " + e.getMessage(),
                                              "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

class DrawArea extends JPanel {
    private final List<Point> points = new ArrayList<>();
    private final ObjectOutputStream out;

    public DrawArea(ObjectOutputStream out) {
        this.out = out;
        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point p = e.getPoint();
                points.add(p);
                repaint();
                try {
                    out.writeObject(p);
                } catch (IOException ex) {
                    System.err.println("Failed to send point: " + ex.getMessage());
                }
            }
        });
    }

    public void addPoint(Point p) {
        points.add(p);
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Point p : points) {
            g.fillOval(p.x, p.y, 5, 5);
        }
    }
}
