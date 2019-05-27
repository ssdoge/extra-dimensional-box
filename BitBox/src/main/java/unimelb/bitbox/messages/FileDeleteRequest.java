package unimelb.bitbox.messages;

import unimelb.bitbox.util.network.JSONDocument;

/**
 * @Auther Benjamin(Jingyi Li) Li
 * @Email jili@student.unimelb.edu.au
 * @ID 961543
 * @Date 2019-04-19 17:25
 */
public class FileDeleteRequest extends Message{
    public FileDeleteRequest(JSONDocument fileDescriptor, String pathName){
        super("FILE_DELETE:" + pathName + ":" + fileDescriptor);
        document.append("command", FILE_DELETE_REQUEST);
        document.append("fileDescriptor", fileDescriptor);
        document.append("pathName", pathName);
    }
}
