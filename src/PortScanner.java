import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The scanning engine. It does a "TCP connect scan": for every port it tries to
 * open a normal TCP connection using java.net.Socket.
 *
 * What happens on the wire (this is what you will see in Wireshark):
 *
 *   OPEN port    : you send SYN  ->  target replies SYN,ACK  ->  you send ACK
 *                  (three-way handshake completes; we then close with FIN or RST)
 *   CLOSED port  : you send SYN  ->  target replies RST,ACK  (connection refused)
 *   FILTERED port: you send SYN  ->  nothing comes back (firewall drops it).
 *                  Java waits until our timeout expires -> TIMED_OUT.
 */
public class PortScanner {

    private final String host;
    private final int timeoutMs;
    private final int threadCount;

    public PortScanner(String host, int timeoutMs, int threadCount) {
        this.host = host;
        this.timeoutMs = timeoutMs;
        this.threadCount = threadCount;
    }

    /**
     * Scans a single port and classifies the outcome.
     * This method never throws: every problem is turned into a ScanResult.
     */
    public ScanResult scanPort(int port) {
        long start = System.currentTimeMillis();

        // try-with-resources closes the socket automatically, even on errors.
        try (Socket socket = new Socket()) {

            // InetSocketAddress = IP address + port number.
            InetSocketAddress target = new InetSocketAddress(host, port);

            // If the host name/IP could not be resolved, the address is "unresolved".
            if (target.isUnresolved()) {
                return new ScanResult(port, PortStatus.ERROR, elapsed(start),
                        "Cannot resolve host '" + host + "'");
            }

            // connect(address, timeout) is the key call. It blocks until:
            //   - handshake succeeds          -> returns normally  (OPEN)
            //   - RST received                -> ConnectException  (CLOSED)
            //   - no reply within timeoutMs   -> SocketTimeoutException (TIMED_OUT)
            socket.connect(target, timeoutMs);
            return new ScanResult(port, PortStatus.OPEN, elapsed(start),
                    "TCP handshake completed");

        } catch (SocketTimeoutException e) {
            return new ScanResult(port, PortStatus.TIMED_OUT, elapsed(start),
                    "No response within " + timeoutMs + " ms (filtered or host down)");

        } catch (NoRouteToHostException e) {
            return new ScanResult(port, PortStatus.ERROR, elapsed(start),
                    "No route to host");

        } catch (UnknownHostException e) {
            return new ScanResult(port, PortStatus.ERROR, elapsed(start),
                    "Unknown host");

        } catch (ConnectException e) {
            // ConnectException covers "connection refused" (closed port) but also
            // "network is unreachable". We look at the message to tell them apart.
            String msg = String.valueOf(e.getMessage()).toLowerCase();
            if (msg.contains("refused")) {
                return new ScanResult(port, PortStatus.CLOSED, elapsed(start),
                        "Connection refused (RST received)");
            }
            if (msg.contains("timed out")) {
                return new ScanResult(port, PortStatus.TIMED_OUT, elapsed(start),
                        "Connection timed out");
            }
            return new ScanResult(port, PortStatus.ERROR, elapsed(start),
                    "Connect failed: " + e.getMessage());

        } catch (IOException e) {
            return new ScanResult(port, PortStatus.ERROR, elapsed(start),
                    "I/O error: " + e.getMessage());

        } catch (SecurityException e) {
            return new ScanResult(port, PortStatus.ERROR, elapsed(start),
                    "Blocked by security manager");
        }
    }

    /**
     * Scans every port from startPort to endPort (inclusive) using a pool of
     * worker threads so that slow/timed-out ports do not make the scan crawl.
     * Results are returned in port order.
     */
    public List<ScanResult> scanRange(int startPort, int endPort) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<ScanResult>> futures = new ArrayList<>();

        for (int port = startPort; port <= endPort; port++) {
            final int p = port;
            futures.add(pool.submit(() -> scanPort(p)));
        }
        pool.shutdown(); // no new tasks; existing ones keep running

        List<ScanResult> results = new ArrayList<>();
        for (Future<ScanResult> f : futures) {
            try {
                results.add(f.get()); // waits for that port's result
            } catch (java.util.concurrent.ExecutionException e) {
                // scanPort() never throws, so this is just a safety net.
                results.add(new ScanResult(-1, PortStatus.ERROR, 0, "Worker failed: " + e.getMessage()));
            }
        }
        return results;
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}
