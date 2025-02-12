package unimelb.bitbox.messages;

import unimelb.bitbox.peers.Peer;
import unimelb.bitbox.server.PeerServer;
import unimelb.bitbox.util.network.HostPort;

/**
 * HANDSHAKE_RESPONSE message.
 *
 * @author Eleanor McMurtry
 */
public class HandshakeResponse extends Response {
    private HostPort hostPort;

    public HandshakeResponse(Peer peer, HostPort hostPort) {
        super("HANDSHAKE", peer);
        this.hostPort = hostPort;

        document.append("command", MessageType.HANDSHAKE_RESPONSE);
        document.append("hostPort", PeerServer.hostPort().toJSON());
    }

    @Override
    void onSent() {
        peer.activate(hostPort);
    }
}
