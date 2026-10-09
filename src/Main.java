import java.util.List;
import java.util.Scanner;

/**
 * Port Scanner using Java Sockets - main program (console menu / prompts).
 *
 * Run it with no arguments and it asks you for everything:
 *     java Main
 * Or pass the values directly (handy for quick tests):
 *     java Main 127.0.0.1 1 1000 500
 *              <ip>      <start> <end> <timeoutMs>
 *
 * ETHICS: only scan localhost or machines you own / have written permission to test.
 */
public class Main {

    // Windows retries a refused connection for ~1-2 s before reporting "refused".
    // A timeout shorter than that can make CLOSED ports look like TIMED_OUT, so we
    // default to 2000 ms to keep the classification accurate.
    private static final int DEFAULT_TIMEOUT_MS = 2000;
    private static final int THREADS = 100;

    public static void main(String[] args) {
        printBanner();
        Scanner in = new Scanner(System.in);

        String host;
        int startPort;
        int endPort;
        int timeout;

        if (args.length == 4) {
            // ----- Non-interactive mode: validate the command-line values -----
            host = args[0].trim();
            if (!isValidHost(host)) {
                System.out.println("Error: '" + host + "' is not a valid IPv4 address.");
                return;
            }
            try {
                startPort = Integer.parseInt(args[1]);
                endPort = Integer.parseInt(args[2]);
                timeout = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.out.println("Error: ports and timeout must be whole numbers.");
                return;
            }
            if (!isValidPort(startPort) || !isValidPort(endPort) || startPort > endPort) {
                System.out.println("Error: ports must be 1-65535 and start <= end.");
                return;
            }
            if (timeout < 50 || timeout > 10000) {
                System.out.println("Error: timeout must be between 50 and 10000 ms.");
                return;
            }
        } else if (args.length == 0) {
            // ----- Interactive mode: keep asking until the input is valid -----
            host = askHost(in);
            startPort = askPort(in, "Enter START port (1-65535): ", 1);
            endPort = askPort(in, "Enter END port   (1-65535): ", startPort);
            timeout = askTimeout(in);
        } else {
            System.out.println("Usage: java Main            (interactive)");
            System.out.println("   or: java Main <ip> <startPort> <endPort> <timeoutMs>");
            return;
        }

        // ----- Safety check for non-local targets -----
        if (!isLoopback(host)) {
            System.out.println();
            System.out.println("WARNING: " + host + " is NOT localhost.");
            System.out.println("Scanning systems without permission may be illegal.");
            System.out.print("Do you own this system or have written permission to scan it? (yes/no): ");
            String answer = in.hasNextLine() ? in.nextLine().trim().toLowerCase() : "no";
            if (!answer.equals("yes")) {
                System.out.println("Scan cancelled.");
                return;
            }
        }

        runScan(host, startPort, endPort, timeout);
    }

    // ------------------------------------------------------------------
    //  The scan itself + summary
    // ------------------------------------------------------------------
    private static void runScan(String host, int startPort, int endPort, int timeout) {
        int total = endPort - startPort + 1;
        System.out.println();
        System.out.println("Scanning " + host + "  ports " + startPort + "-" + endPort
                + "  (timeout " + timeout + " ms, " + THREADS + " threads)");
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win") && timeout < 2000) {
            System.out.println("NOTE: On Windows a refused (closed) port can take 1-2 s to be reported.");
            System.out.println("      With a timeout under 2000 ms, closed ports may show as TIMED_OUT.");
        }
        System.out.println("Please wait...");
        System.out.println("------------------------------------------------------------");

        PortScanner scanner = new PortScanner(host, timeout, THREADS);
        long begin = System.currentTimeMillis();
        List<ScanResult> results;
        try {
            results = scanner.scanRange(startPort, endPort);
        } catch (InterruptedException e) {
            System.out.println("Scan interrupted.");
            Thread.currentThread().interrupt();
            return;
        }
        long totalTime = System.currentTimeMillis() - begin;

        int open = 0, closed = 0, timeoutOrUnknown = 0;
        for (ScanResult r : results) {
            switch (r.getStatus()) {
                case OPEN:      open++;     break;
                case CLOSED:    closed++;   break;
                default:        timeoutOrUnknown++; break;
            }
            String status = r.getStatus() == PortStatus.OPEN ? "OPEN"
                    : r.getStatus() == PortStatus.CLOSED ? "CLOSED" : "TIMEOUT/UNKNOWN";
            System.out.printf("Port %-5d %-15s %5d ms   %s%n",
                    r.getPort(), status, r.getTimeMs(), r.getDetail());
        }

        System.out.println("------------------------------------------------------------");
        System.out.println("SCAN SUMMARY");
        System.out.println("  Target          : " + host);
        System.out.println("  Port range      : " + startPort + " - " + endPort);
        System.out.println("  Ports scanned   : " + total);
        System.out.println("  Open            : " + open);
        System.out.println("  Closed          : " + closed);
        System.out.println("  Timeout/unknown : " + timeoutOrUnknown);
        System.out.println("  Total time      : " + totalTime + " ms");
        if (open > 0) {
            StringBuilder sb = new StringBuilder();
            for (ScanResult r : results) {
                if (r.getStatus() == PortStatus.OPEN) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(r.getPort());
                }
            }
            System.out.println("  Open port list  : " + sb);
        }
    }

    // ------------------------------------------------------------------
    //  Input helpers (each one loops until the user types something valid)
    // ------------------------------------------------------------------
    private static String askHost(Scanner in) {
        while (true) {
            System.out.print("Enter target IPv4 address (e.g. 127.0.0.1): ");
            if (!in.hasNextLine()) {
                System.out.println("No input. Exiting.");
                System.exit(0);
            }
            String text = in.nextLine().trim();
            if (isValidHost(text)) {
                return text;
            }
            System.out.println("  Invalid IP address. Use four numbers 0-255 separated by dots.");
        }
    }

    private static int askPort(Scanner in, String prompt, int minAllowed) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextLine()) {
                System.out.println("No input. Exiting.");
                System.exit(0);
            }
            String text = in.nextLine().trim();
            try {
                int port = Integer.parseInt(text);
                if (!isValidPort(port)) {
                    System.out.println("  Port must be between 1 and 65535.");
                } else if (port < minAllowed) {
                    System.out.println("  End port cannot be smaller than start port (" + minAllowed + ").");
                } else {
                    return port;
                }
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a whole number.");
            }
        }
    }

    private static int askTimeout(Scanner in) {
        while (true) {
            System.out.print("Enter timeout in ms (50-10000, press Enter for " + DEFAULT_TIMEOUT_MS + "): ");
            if (!in.hasNextLine()) {
                return DEFAULT_TIMEOUT_MS;
            }
            String text = in.nextLine().trim();
            if (text.isEmpty()) {
                return DEFAULT_TIMEOUT_MS;
            }
            try {
                int t = Integer.parseInt(text);
                if (t >= 50 && t <= 10000) {
                    return t;
                }
                System.out.println("  Timeout must be between 50 and 10000 ms.");
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a whole number.");
            }
        }
    }

    // ------------------------------------------------------------------
    //  Validation helpers
    // ------------------------------------------------------------------
    static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    /** Accepts a dotted IPv4 address such as 192.168.1.10 (or the word "localhost"). */
    static boolean isValidHost(String text) {
        if (text.equalsIgnoreCase("localhost")) {
            return true;
        }
        String[] parts = text.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String p : parts) {
            if (p.isEmpty() || p.length() > 3) {
                return false;
            }
            for (char c : p.toCharArray()) {
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            if (Integer.parseInt(p) > 255) {
                return false;
            }
        }
        return true;
    }

    static boolean isLoopback(String host) {
        return host.equalsIgnoreCase("localhost") || host.startsWith("127.");
    }

    private static void printBanner() {
        System.out.println("============================================================");
        System.out.println("        PORT SCANNER USING JAVA SOCKETS  (TCP connect scan)");
        System.out.println("  Scan only localhost or systems you have permission to test");
        System.out.println("============================================================");
    }
}
