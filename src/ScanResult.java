/**
 * Holds the result of scanning ONE port: which port, what happened,
 * how long it took, and a short human-readable detail message.
 */
public class ScanResult {

    private final int port;
    private final PortStatus status;
    private final long timeMs;
    private final String detail;

    public ScanResult(int port, PortStatus status, long timeMs, String detail) {
        this.port = port;
        this.status = status;
        this.timeMs = timeMs;
        this.detail = detail;
    }

    public int getPort() {
        return port;
    }

    public PortStatus getStatus() {
        return status;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return String.format("Port %-5d %-10s %5d ms   %s", port, status, timeMs, detail);
    }
}
