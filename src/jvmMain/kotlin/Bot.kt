import com.sun.javaws.Globals
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL
import javax.xml.bind.JAXBElement
import kotlin.concurrent.thread
import kotlin.math.max

class Bot(var token: String) {
    val commands = mutableMapOf<String, suspend (Message, String?) -> Unit>()

    var last = 0

    val myThread = thread {
        val url = "https://api.telegram.org/bot" + token
        while (true) {
            val input = URL(url + "/getUpdates" + "?offset=" + last.toString()).readText()
            val update = Json.decodeFromString<Request>(input)
            println(update.result.size)
            update.result.forEach { upd ->
                last = max(last, upd.update_id+1)
                if (upd.message == null)
                    return@forEach
                if (upd.message!!.text != null) {
                    commands.forEach { (command, functor) ->
                        if (command in upd.message!!.text!!)
                            GlobalScope.launch { functor.invoke(upd.message, null)}
                    }
                }
            }
            Thread.sleep(1000)
        }
    }

    fun finish() = myThread.stop()

    fun sendMessage(
        chat: Int, text: String, parse_mode: String? = null, entities: List<MessageEntity>? = null,
        disable_web_page_preview: Boolean? = null, disable_notification: Boolean? = null,
        reply_to_message_id: Int? = null, allow_sending_without_reply: Boolean? = null,
        reply_markup: InlineKeyboardMarkup? = null
    ) {
        val url = "https://api.telegram.org/bot" + token
        URL(
            url + "/sendMessage?chat_id=" + chat + "&text=" + text
                    + if (parse_mode != null) "&parse_mode=" + parse_mode else ""
                    + if (entities != null) "&entities=" + entities else ""
                    + if (disable_web_page_preview != null) "&disable_web_page_preview=" + disable_web_page_preview else ""
                    + if (disable_notification != null) "&disable_notification=" + disable_notification else ""
                    + if (reply_to_message_id != null) "&reply_to_message_id=" + reply_to_message_id else ""
                    + if (allow_sending_without_reply != null)
                        "&allow_sending_without_reply=" + allow_sending_without_reply else ""
                    + if (reply_markup != null) "&reply_markup=" + reply_markup else ""
        ).readText()
    }

    fun onCommand(command: String, functor: suspend (Message, String?) -> Unit){
        commands += Pair(command, functor)
    }

}