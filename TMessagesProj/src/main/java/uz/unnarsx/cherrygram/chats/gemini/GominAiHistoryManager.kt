/**
 * This is the source code of Gomin Suite for Android.
 * It is licensed under GNU GPL v. 2 or later.
 */

package uz.unnarsx.cherrygram.chats.gemini

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessageObject
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import uz.unnarsx.cherrygram.misc.Constants
import java.io.File

object GominAiHistoryManager {
    
    data class GominMessage(
        val role: String, // "user" or "model"
        val text: String,
        val timestamp: Long
    )

    private val gson = Gson()
    private val historyFile: File
        get() = File(ApplicationLoader.applicationContext.filesDir, "gomin_ai_history.json")

    @Synchronized
    fun loadRawMessages(): ArrayList<GominMessage> {
        val file = historyFile
        if (!file.exists()) {
            return ArrayList()
        }
        return try {
            val json = file.readText()
            val type = object : TypeToken<ArrayList<GominMessage>>() {}.type
            gson.fromJson(json, type) ?: ArrayList()
        } catch (e: Exception) {
            ArrayList()
        }
    }

    @Synchronized
    fun saveRawMessages(messages: ArrayList<GominMessage>) {
        try {
            val json = gson.toJson(messages)
            historyFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun addMessage(role: String, text: String): GominMessage {
        val messages = loadRawMessages()
        val gMsg = GominMessage(role, text, System.currentTimeMillis())
        messages.add(gMsg)
        saveRawMessages(messages)
        return gMsg
    }

    @Synchronized
    fun clearHistory() {
        try {
            historyFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadMessages(currentAccount: Int): ArrayList<MessageObject> {
        val raw = loadRawMessages()
        val objects = ArrayList<MessageObject>()
        var localId = 1
        
        for (gMsg in raw) {
            val isOut = gMsg.role == "user"
            val message = TLRPC.TL_message().apply {
                id = localId++
                peer_id = TLRPC.TL_peerUser().apply { user_id = Constants.GOMIN_AI_DIALOG_ID }
                from_id = if (isOut) {
                    TLRPC.TL_peerUser().apply { user_id = UserConfig.getInstance(currentAccount).clientUserId }
                } else {
                    TLRPC.TL_peerUser().apply { user_id = Constants.GOMIN_AI_DIALOG_ID }
                }
                message = gMsg.text
                date = (gMsg.timestamp / 1000).toInt()
                out = isOut
                send_state = MessageObject.MESSAGE_SEND_STATE_SENT
            }
            
            val messageObject = MessageObject(currentAccount, message, true, true)
            objects.add(messageObject)
        }
        return objects
    }
}
