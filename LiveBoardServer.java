
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class LiveBoardServer {
    private static final List<ObjectOutputStream> clients = Collections.synchronizedList(new ArrayList<>());
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("LiveBoard Server started on port 12345");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            clients.add(out);

            pool.execute(() -> handleClient(clientSocket, out));
        }
    }

    private static void handleClient(Socket clientSocket, ObjectOutputStream out) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            Object input;
            while ((input = in.readObject()) != null) {
                broadcast(input);
            }
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
            clients.remove(out);
        }
    }

    private static void broadcast(Object input) {
        synchronized (clients) {
            Iterator<ObjectOutputStream> it = clients.iterator();
            while (it.hasNext()) {
                try {
                    it.next().writeObject(input);
                } catch (IOException e) {
                    it.remove(); // remove disconnected client
                }
            }
        }
    }
}
