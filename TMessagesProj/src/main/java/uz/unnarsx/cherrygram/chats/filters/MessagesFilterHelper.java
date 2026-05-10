package uz.unnarsx.cherrygram.chats.filters;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;

public class MessagesFilterHelper {
    public static final MessagesFilterHelper INSTANCE = new MessagesFilterHelper();
    public boolean shouldBlockMessage(MessageObject msg) { return false; }
    public ArrayList<TLRPC.MessageEntity> addSpoilerEntities(MessageObject msg) { return new ArrayList<>(); }
    public String addSpoilerEntities(String text) { return text; }
    public ArrayList<TLRPC.MessageEntity> addSpoilerEntities(MessageObject msg, ArrayList<TLRPC.MessageEntity> entities) { return entities != null ? entities : new ArrayList<>(); }
    public int getExcludedChatsCount() { return 0; }
    public ArrayList<String> getArrayList(Object list) { return new ArrayList<>(); }
    public Object getExcludedList() { return null; }
    public void saveArrayList(ArrayList<String> list, Object key) {}
}
