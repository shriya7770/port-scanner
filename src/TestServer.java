import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A tiny test server so you can create OPEN ports on your own laptop.
 *
 * Usage:
 *   java TestServer              -> listens on ports 8080 and 9090
 *   java TestServer 7000 7001    -> listens on the ports you give
 *
 * It binds to 127.0.0.1 ONLY (loopback), so nobody else on your network can reach it.
 * Every ServerSocket runs in its own thread, accepts connections, sends a short
 * banner message and closes the connection.
 * Press Ctrl+C to stop the server.
 */
public class TestServer {

    public static void main(String[] args) {
        int[] ports = {8080, 9090};

        if (args.length > 0) {
            ports = new int[args.length];
            for (int i = 0; i < args.length; i++) {
                try {
                    ports[i] = Integer.parseInt(args[i]);
                    if (ports[i] < 1 || ports[i] > 65535) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port: " + args[i] + " (use numbers 1-65535)");
                    return;
                }
            }
        }

        for (int port : ports) {
            Thread t = new Thread(() -> listen(port));
            t.start();
        }
        System.out.println("TestServer running. Press Ctrl+C to stop.");
    }

    private static void listen(int port) {
        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))) {
            System.out.println("Listening on 127.0.0.1:" + port);

            while (true) {
                try (Socket client = server.accept()) {
                    System.out.println("[port " + port + "] connection from "
                            + client.getRemoteSocketAddress());
                    OutputStream out = client.getOutputStream();
                    out.write(("Hello from TestServer on port " + port + "\r\n").getBytes());
                    out.flush();
                } catch (IOException e) {
                    System.out.println("[port " + port + "] client error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Could not listen on port " + port + ": " + e.getMessage()
                    + " (is it already in use?)");
        }
    }
}
