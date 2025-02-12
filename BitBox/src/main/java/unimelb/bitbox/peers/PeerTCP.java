package unimelb.bitbox.peers;

import functional.algebraic.Maybe;
import unimelb.bitbox.messages.Message;
import unimelb.bitbox.server.PeerServer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * A TCP implementation of the {@link Peer}.
 *
 * @author Eleanor McMurtry
 */
public class PeerTCP extends Peer {
    private final Socket socket;

    public PeerTCP(String name, Socket socket, PeerType type) {
        super(name, type, socket.getInetAddress().getHostAddress(), socket.getPort(), new OutgoingConnectionTCP(socket));
        submit(this::receiveMessages);
        this.socket = socket;
    }

    @Override
    protected void closeInternal() {
        try {
            socket.close();
        } catch (IOException e) {
            PeerServer.log().severe("Error closing socket: " + e.getMessage());
        }
    }

    @Override
    void requestSent(Message request) {}

    @Override
    void responseReceived(Message response) {}

    private void receiveMessages() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while (Maybe.of(message = in.readLine()).isJust()) {
                receiveMessage(message);
            }
        } catch (IOException e) {
            if (!isClosed()) {
                PeerServer.log().severe("Error reading from socket: " + e.getMessage());
            }
        } finally {
            close();
        }
    }
}

/**
 * The outgoing part of the connection for a TCP peer.
 *
 * @author Eleanor McMurtry
 */
class OutgoingConnectionTCP extends OutgoingConnection {
    private final Socket socket;

    OutgoingConnectionTCP(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            while (!socket.isClosed()) {
                OutgoingMessage message = takeMessage();
                out.write(message.message);
                out.flush();
                message.onSent.run();
            }
        } catch (IOException e) {
            PeerServer.log().severe("Error writing to socket: " + e.getMessage());
        } catch (InterruptedException e) {
            PeerServer.log().fine("thread interrupted: " + e.getMessage());
        }
    }
}
