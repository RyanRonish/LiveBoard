import java.io.*;
import java.net.*;
import java.util.*;

public class LiveBoardServer {
    private static List<ObjectOutputStream> clients = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("LiveBoard Server started on port 12345");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            clients.add(out);

            new Thread(() -> {
                try {
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    Object input;
                    while ((input = in.readObject()) != null) {
                        broadcast(input);
                    }
                } catch (Exception e) {
                    System.out.println("Client disconnected.");
                }
            }).start();
        }
    }

    private static void broadcast(Object message) {
        for (ObjectOutputStream client : clients) {
            try {
                client.writeObject(message);
                client.flush();
            } catch (IOException e) {
                System.out.println("Error sending to client.");
            }
        }
    }
}