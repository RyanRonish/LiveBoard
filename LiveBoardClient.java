import javax.swing.*; // for gui components
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.*;

// client side that extends jframe for gui
public class LiveBoardClient extends JFrame {
    private DrawArea drawArea;  // panel for drawing

    // buttons and sliders for user interaction
    private JButton clearButton;
    private JSlider brushSizeSlider;
    private JButton colorButton;
    private DefaultListModel<String> userListModel;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Color currentColor = Color.BLACK;
    private int currentBrushSize = 5;

    public LiveBoardClient(String serverIP, int port) throws IOException {
        super("LiveBoard Client"); // window

        // connects to server
        socket = new Socket(serverIP, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        drawArea = new DrawArea();
        drawArea.setBackground(Color.WHITE);
        //features of the draw area
        clearButton = new JButton("Clear Board");
        brushSizeSlider = new JSlider(1, 20, 5);
        colorButton = new JButton("Choose Color");

        //list of users connected
        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        // tools at the top for the user to choose from
        JPanel toolsPanel = new JPanel();
        toolsPanel.add(colorButton);
        toolsPanel.add(new JLabel("Brush Size:"));
        toolsPanel.add(brushSizeSlider);
        toolsPanel.add(clearButton);
        // right panel for user list
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(new JLabel("Connected Users:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        add(drawArea, BorderLayout.CENTER);
        add(toolsPanel, BorderLayout.NORTH);
        add(rightPanel, BorderLayout.EAST);

        drawArea.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int x = e.getX(); // X position of cursor
                int y = e.getY();  // Y position of cursor
                drawArea.drawPoint(x, y, currentColor, currentBrushSize);
                sendMessage("DRAW:" + x + "," + y + "," + currentColor.getRGB() + "," + currentBrushSize);
            }
        });


        // clear button to clear the board
        clearButton.addActionListener(e -> {
            drawArea.clear();
            sendMessage("CLEAR");
        });

        // for the usser to choose a color
        colorButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(null, "Pick a Color", currentColor);
            if (chosen != null) currentColor = chosen;
        });

        brushSizeSlider.addChangeListener(e -> currentBrushSize = brushSizeSlider.getValue());


        new Thread(() -> { // thread to listen for messages from the server
            try {
                while (true) {
                    String msg = (String) in.readObject();
                    if (msg.startsWith("DRAW:")) {
                        String[] parts = msg.substring(5).split(",");
                        int x = Integer.parseInt(parts[0]);           // Parse drawing message
                        int y = Integer.parseInt(parts[1]);
                        Color color = new Color(Integer.parseInt(parts[2]));
                        int size = Integer.parseInt(parts[3]);
                        drawArea.drawPoint(x, y, color, size);
                    } else if (msg.equals("CLEAR")) {
                        drawArea.clear();
                    } else if (msg.startsWith("USERS:")) {
                        String[] users = msg.substring(7).split(";");
                        SwingUtilities.invokeLater(() -> {
                            userListModel.clear();
                            for (String user : users) userListModel.addElement(user);
                        });
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void sendMessage(String msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // class to represent the drawing area
    class DrawArea extends JPanel {
        private List<Stroke> strokes = new ArrayList<>();

        public void drawPoint(int x, int y, Color color, int size) {
            strokes.add(new Stroke(x, y, color, size));
            repaint();
        }

        public void clear() {
            strokes.clear();
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (Stroke s : strokes) {
                g.setColor(s.color);
                g.fillOval(s.x, s.y, s.size, s.size);
            }
        }
    }

    // class to represent a single point drawn on the board
    class Stroke {
        int x, y, size;
        Color color;
        Stroke(int x, int y, Color color, int size) {
            this.x = x; this.y = y; this.color = color; this.size = size;
        }
    }

    public static void main(String[] args) throws IOException {
        String ip = JOptionPane.showInputDialog("Enter Server IP Address:");
        String portStr = JOptionPane.showInputDialog("Enter Port Number:");
        int port = Integer.parseInt(portStr);
        new LiveBoardClient(ip, port);  // creates the client
    }
}