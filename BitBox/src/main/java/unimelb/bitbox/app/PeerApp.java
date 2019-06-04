package unimelb.bitbox.app;

import unimelb.bitbox.server.PeerServer;

import java.util.logging.Logger;

/**
 * Class to initialise the peer-to-peer application.
 */
public class PeerApp
{
	private static Logger log = Logger.getLogger(PeerApp.class.getName());
    public static void main (String[] args) {
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        
        try {
            PeerServer.initialise();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}