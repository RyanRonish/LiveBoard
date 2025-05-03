import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class LiveBoardServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("LiveBoard Server started on port " + PORT);
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                pool.execute(handler);
                broadcastUserList();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void broadcast(String msg, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude) {
                client.send(msg);
            }
        }
    }

    public static void broadcastUserList() {
        StringBuilder sb = new StringBuilder("USERS:");
        for (ClientHandler c : clients) {
            sb.append(c.getClientName()).append(";");
        }
        String userMsg = sb.toString();
        for (ClientHandler c : clients) {
            c.send(userMsg);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientName = socket.getInetAddress().getHostAddress();
        }

        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                System.out.println("Client connected: " + clientName);

                while (true) {
                    Object obj = in.readObject();
                    if (!(obj instanceof String)) continue;
                    String msg = (String) obj;

                    if (msg.equals("CLEAR")) {
                        broadcast("CLEAR", null);
                    } else if (msg.startsWith("DRAW:")) {
                        broadcast(msg, this);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client disconnected or error: " + clientName);
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException e) {}
                clients.remove(this);
                broadcastUserList();
            }
        }

        public void send(String msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                System.out.println("Error sending to client: " + clientName);
                e.printStackTrace();
                clients.remove(this);
            }
        }

        public String getClientName() {
            return clientName;
        }
    }
}
