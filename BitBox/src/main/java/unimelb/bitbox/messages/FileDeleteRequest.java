package unimelb.bitbox.messages;

import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.JsonDocument;

/**
 * @Auther Benjamin(Jingyi Li) Li
 * @Email jili@student.unimelb.edu.au
 * @ID 961543
 * @Date 2019-04-19 17:25
 */
public class FileDeleteRequest extends Message{
    public FileDeleteRequest(FileSystemManager.FileDescriptor fileDescriptor, String pathName){
        document.append("command", FILE_DELETE_REQUEST);

        JsonDocument jsonFileDescriptor = new JsonDocument();
        jsonFileDescriptor.append("md5", fileDescriptor.md5);
        jsonFileDescriptor.append("lastModified", fileDescriptor.lastModified);
        jsonFileDescriptor.append("fileSize", fileDescriptor.fileSize);
        document.append("fileDescriptor", jsonFileDescriptor);

        document.append("pathName", pathName);
    }
}
