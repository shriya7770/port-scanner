/**
 * The possible outcomes of trying to connect to one port.
 *
 * OPEN      - TCP handshake completed (SYN -> SYN/ACK -> ACK). Something is listening.
 * CLOSED    - The target answered with a TCP RST ("connection refused"). Nothing is listening.
 * TIMED_OUT - No answer at all within the timeout. Usually a firewall silently dropping packets
 *             (the port is "filtered"), or the host is down/unreachable.
 * ERROR     - Some other problem (no route to host, network unreachable, etc.).
 */
public enum PortStatus {
    OPEN,
    CLOSED,
    TIMED_OUT,
    ERROR
}
