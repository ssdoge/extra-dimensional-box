package unimelb.bitbox.messages;

import unimelb.bitbox.server.PeerServer;
import unimelb.bitbox.util.fs.FileSystemException;

public class DirectoryCreateResponse extends Response {
    private static final String SUCCESS = "directory created";
    private final String pathName;

    public DirectoryCreateResponse(String pathName) {
        super("DIRECTORY_CREATE:" + pathName);

        this.pathName = pathName;
        document.append("command", DIRECTORY_CREATE_RESPONSE);
        document.append("pathName", pathName);
    }

    @Override
    void onSent() {
        String reply = SUCCESS;
        if (!PeerServer.fsManager().isSafePathName(pathName)) {
            reply = "unsafe pathname given";
        } else if (PeerServer.fsManager().dirNameExists(pathName)) {
            reply = "pathname already exists";
        } else {
            try {
                PeerServer.fsManager().makeDirectory(pathName);
            } catch (FileSystemException e) {
                reply = "there was a problem creating the directory";
            }
        }

        document.append("message", reply);
        document.append("status", reply.equals(SUCCESS));
    }
}