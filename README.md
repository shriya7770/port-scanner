Port Scanner Using Java Sockets

This project is a simple TCP Port Scanner developed using Java. It checks a range of ports on a target IP address and identifies whether each port is open, closed, timed out, or encountered an error.

The project uses Java Sockets to test TCP connections and multithreading to scan multiple ports efficiently. Wireshark is used to capture and analyze network packets to understand how TCP connections are established and refused.

Features
Scans a user-specified range of TCP ports.
Identifies ports as OPEN, CLOSED, TIMED_OUT, or ERROR.
Uses multithreading for efficient scanning.
Measures connection response time.
Includes a test server for testing open and closed ports locally.
Supports packet analysis using Wireshark.
Technologies Used
Language: Java
Networking: Java Sockets and TCP
Concurrency: Java multithreading and ExecutorService
Packet Analysis: Wireshark
Environment: Windows, PowerShell, and batch scripts


How It Works
The user provides a target IP address, a starting port, an ending port, and a timeout value.
The scanner attempts to establish a TCP connection to each port using Java Sockets.
Successful connections are reported as open, while refused connections are reported as closed.
Ports that do not respond within the specified time may be reported as timed out.
Multithreading allows multiple ports to be checked efficiently.
Wireshark captures the packets exchanged during scanning so the TCP connection process can be analyzed.
How to Run
Prerequisites
Java Development Kit (JDK) installed.
Wireshark installed (optional, for packet analysis).
Step 1: Start the Test Server

Open PowerShell in the project folder and run:

.\run_server.bat

The test server listens on 127.0.0.1 at ports 8080 and 9090 by default. Keep this terminal open while scanning.

Step 2: Run the Port Scanner

Open another PowerShell terminal in the same project folder and run:

.\run_scanner.bat 127.0.0.1 8079 8081 2000

This example scans ports 8079 through 8081 on the local computer with a timeout of 2000 milliseconds, assuming the script uses the arguments in that order.

If the test server is running, port 8080 should normally be open, while ports 8079 and 8081 should be closed if no other service is listening on them.

Wireshark Packet Analysis

To observe the packets generated during scanning:

Open Wireshark and select Adapter for loopback traffic capture.
Start capturing packets before running the scanner.
Run the port scanner.
Apply the following display filters.

To inspect traffic on port 8080:

tcp.port == 8080

To inspect a range of ports:

tcp.port >= 8079 && tcp.port <= 8081

For an open port, the TCP three-way handshake typically consists of SYN, SYN-ACK, and ACK packets. For a closed port, a connection attempt commonly receives an RST response, indicating that the connection was refused.

Learning Outcomes
Understanding TCP connections and port numbers.
Learning Java socket programming.
Understanding multithreading for network operations.
Identifying common TCP packet types using Wireshark.
Learning basic network troubleshooting and port-state detection.
