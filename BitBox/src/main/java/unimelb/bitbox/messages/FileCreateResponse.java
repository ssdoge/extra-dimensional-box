package unimelb.bitbox.messages;

import unimelb.bitbox.server.PeerServer;
import unimelb.bitbox.util.fs.FileDescriptor;

import java.io.IOException;

public class FileCreateResponse extends Response {
    private static final String SUCCESS = "file loader ready";
    private String pathName;
    private FileDescriptor fileDescriptor;

    private boolean successful = false;
    public boolean isSuccessful() {
        return successful;
    }

    public FileCreateResponse(String pathName, FileDescriptor fileDescriptor) {
        super("FILE_CREATE:" + pathName + ":" + fileDescriptor);
        this.pathName = pathName;
        this.fileDescriptor = fileDescriptor;

        document.append("command", FILE_CREATE_RESPONSE);
        document.append("fileDescriptor", fileDescriptor);
        document.append("pathName", pathName);
    }

    private String generateFileLoader() {
        try {
            PeerServer.fsManager().createFileLoader(pathName, fileDescriptor);
        } catch (IOException e) {
            // We possibly have a different version of this file.
            if (PeerServer.fsManager().fileNameExists(pathName)) {
                try {
                    PeerServer.fsManager().modifyFileLoader(pathName, fileDescriptor);
                } catch (IOException ignored) {
                    // We're currently transferring this file, or else our file is newer.
                    PeerServer.logWarning("failed to generate file loader for " + pathName);
                    return "error generating modify file loader: " + pathName;
                }
            } else {
                return "error generating create file loader: " + pathName;
            }
        }
        return SUCCESS;
    }

    @Override
    void onSent() {
        String reply;
        if (PeerServer.fsManager().fileNameExists(pathName, fileDescriptor.md5)) {
            reply = "file already exists locally";
        } else if (!PeerServer.fsManager().isSafePathName(pathName)) {
            reply = "unsafe pathname given: " + pathName;
        } else {
            reply = generateFileLoader();
        }

        successful = reply.equals(SUCCESS);
        document.append("message", reply);
        document.append("status", successful);
    }
}
