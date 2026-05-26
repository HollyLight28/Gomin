/**
 * This is the source code of Gomin Suite for Android.
 * It is licensed under GNU GPL v. 2 or later.
 */

package uz.unnarsx.cherrygram.chats.gemini

import android.content.DialogInterface
import android.text.InputType
import android.text.TextUtils
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.java.GenerativeModelFutures
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.LocaleController
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ChatActivity
import org.telegram.ui.LaunchActivity
import uz.unnarsx.cherrygram.core.configs.CherrygramMessagesConfig
import uz.unnarsx.cherrygram.misc.Constants
import java.util.ArrayList

object GominAiChatHelper {

    // Context for Dmitry / manipulation chat mode
    var activeShieldContext: String? = null
    var activeShieldHistory: String? = null
    var activeShieldPartnerName: String = "Співрозмовник"

    // Custom clinical prompt to use for Shield analysis and follow-up discussion
    val shieldSystemPrompt = """
Ти — експерт з аналізу міжособистісної комунікації, профайлер поведінкових патернів, спеціаліст з розпізнавання маніпуляцій, психологічного тиску, прихованої агресії, аб’юзивних динамік, коерсивного контролю та токсичних сценаріїв спілкування.

Твоя головна задача — аналізувати надані користувачем діалоги, повідомлення, конфлікти, ситуації, листування, репліки, поведінку або опис взаємодії та виявляти:
- маніпулятивні техніки;
- прихований психологічний підтекст;
- емоційний ефект на адресата;
- ризики для психіки, кордонів, самооцінки та безпеки;
- найбільш ефективні способи захисту й деескалації;
- конкретні фрази, якими користувач може відповісти без виправдань, без втягування в чужу гру і без самознецінення.

Працюй як аналітик, а не як “добрий співрозмовник”, який всіх намагається виправдати. Твій пріоритет — точність, ясність, викриття динаміки впливу та захист користувача. Не пом’якшуй суть, якщо бачиш явні ознаки тиску, аб’юзу, приниження, прихованого контролю чи нарцисичних патернів.

Водночас будь інтелектуально чесним:
- не вигадуй факти, яких немає;
- не став клінічних діагнозів;
- не називай людину “нарцисом” як медичний факт без підстав;
- якщо бачиш лише патерни, формулюй так: “є ознаки нарцисичної поведінки / нарцисичної динаміки / контролюючого стилю”;
- якщо даних недостатньо, прямо кажи, що висновок попередній;
- чітко відрізняй факти, інтерпретації та гіпотези.

ОСНОВНИЙ ПРИНЦИП:
Ти не просто “пояснюєш конфлікт”, а розкладаєш взаємодію на механіки впливу: хто що робить, навіщо, яким інструментом, на які емоції тисне і як це зупинити.

ТВОЄ ЗАВДАННЯ В КОЖНОМУ АНАЛІЗІ:
1. Витягти суть ситуації.
2. Виявити конкретні маніпулятивні техніки.
3. Пояснити прихований намір маніпулятора.
4. Показати, який емоційний стан це має викликати в адресата.
5. Оцінити рівень токсичності й ризику.
6. Дати чітку стратегію захисту.
7. Дати готові фрази-відповіді.
8. Якщо треба — вказати, коли краще не пояснювати, не сперечатись, а дистанціюватись.
9. Якщо є ознаки ескалації небезпеки — прямо вказати на це.
10. Якщо контекст неоднозначний — дати 2–3 можливі трактування з рівнем впевненості.

ТЕХНІКИ, ЯКІ ТРЕБА ВМІТИ ВИЯВЛЯТИ:
- газлайтинг;
- знецінення;
- пасивна агресія;
- прихована агресія;
- маніпуляція почуттям провини;
- навішування сорому;
- емоційний шантаж;
- жалість як інструмент контролю;
- роль жертви як спосіб уникнути відповідальності;
- трикутник Карпмана (рятівник — жертва — переслідувач);
- перекручування фактів;
- подвійні послання;
- “покарання мовчанням”;
- ревнощі як контроль;
- ізоляція від інших;
- фінансовий контроль;
- сексуальний тиск;
- знецінення меж;
- переведення фокусу з проблеми на реакцію жертви;
- DARVO (заперечення, атака, перевертання ролей жертви й агресора);
- blame shifting;
- love bombing;
- intermittent reinforcement (то тепло, то холодно);
- future faking;
- guilt tripping;
- baiting (провокація на емоційну реакцію);
- triangulation;
- smear campaign;
- silent treatment;
- boundary testing;
- coercive control;
- moral superiority;
- псевдологіка для придушення сумнівів адресата;
- інфантилізація;
- патерн “ти все не так зрозумів”;
- патерн “після всього, що я для тебе зробив”;
- патерн “якщо ти мене любиш, ти мусиш...”;
- патерн “я такий через тебе”.
""".trimIndent()

    /**
     * Self-healing алгоритм чергування ролей для Gemini Multi-turn Chat.
     * Запобігає крашам SDK через послідовні однакові ролі, зливаючи сусідні повідомлення в одне.
     */
    fun prepareTurns(
        systemPrompt: String,
        history: List<GominAiHistoryManager.GominMessage>,
        newQuery: String,
        isShieldMode: Boolean,
        shieldPartner: String,
        shieldContext: String?
    ): List<Content> {
        val mergedHistory = ArrayList<GominAiHistoryManager.GominMessage>()

        // 1. Зливаємо сусідні повідомлення з однаковими ролями в історії
        for (msg in history) {
            val textToUse = msg.text.trim()
            if (TextUtils.isEmpty(textToUse)) continue
            
            if (mergedHistory.isNotEmpty() && mergedHistory.last().role == msg.role) {
                val last = mergedHistory.removeAt(mergedHistory.size - 1)
                mergedHistory.add(GominAiHistoryManager.GominMessage(
                    last.role,
                    last.text + "\n\n" + textToUse,
                    msg.timestamp
                ))
            } else {
                mergedHistory.add(GominAiHistoryManager.GominMessage(msg.role, textToUse, msg.timestamp))
            }
        }

        // 2. Додаємо новий запит користувача (зливаємо, якщо останній теж був user)
        val queryToUse = newQuery.trim()
        if (mergedHistory.isNotEmpty() && mergedHistory.last().role == "user") {
            val last = mergedHistory.removeAt(mergedHistory.size - 1)
            mergedHistory.add(GominAiHistoryManager.GominMessage(
                "user",
                last.text + "\n\n" + queryToUse,
                System.currentTimeMillis()
            ))
        } else {
            mergedHistory.add(GominAiHistoryManager.GominMessage(
                "user",
                queryToUse,
                System.currentTimeMillis()
            ))
        }

        val contents = ArrayList<Content>()

        // 3. Формуємо стартову чергу (System Prompt -> Model Ack)
        if (isShieldMode && shieldContext != null) {
            contents.add(Content.Builder().apply {
                role = "user"
                text("Системна інструкція: $shieldSystemPrompt\n\nОсь повна історія чату з $shieldPartner для аналізу:\n$activeShieldHistory")
            }.build())

            val firstTurn = mergedHistory.firstOrNull()
            if (firstTurn != null && firstTurn.role == "model") {
                contents.add(Content.Builder().apply {
                    role = "model"
                    text("Зрозумів. Я детально проаналізував діалог з $shieldPartner. Ось мій аналіз:\n$shieldContext\n\n${firstTurn.text}")
                }.build())
                mergedHistory.removeAt(0)
            } else {
                contents.add(Content.Builder().apply {
                    role = "model"
                    text("Зрозумів. Я детально проаналізував діалог з $shieldPartner. Ось мій аналіз:\n$shieldContext")
                }.build())
            }
        } else {
            val prompt = if (TextUtils.isEmpty(systemPrompt)) "Ти - корисний АІ помічник." else systemPrompt
            contents.add(Content.Builder().apply {
                role = "user"
                text("Системна інструкція: $prompt")
            }.build())

            val firstTurn = mergedHistory.firstOrNull()
            if (firstTurn != null && firstTurn.role == "model") {
                contents.add(Content.Builder().apply {
                    role = "model"
                    text("Зрозумів інструкцію. Чим я можу допомогти тобі сьогодні, бро?\n\n${firstTurn.text}")
                }.build())
                mergedHistory.removeAt(0)
            } else {
                contents.add(Content.Builder().apply {
                    role = "model"
                    text("Зрозумів інструкцію. Чим я можу допомогти тобі сьогодні, бро?")
                }.build())
            }
        }

        // 4. Додаємо решту чергованих повідомлень
        for (msg in mergedHistory) {
            contents.add(Content.Builder().apply {
                role = msg.role
                text(msg.text)
            }.build())
        }

        return contents
    }

    fun queryAi(currentAccount: Int, newUserText: String) {
        val apiKey = CherrygramMessagesConfig.geminiApiKey
        val modelName = CherrygramMessagesConfig.geminiModelName

        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(modelName)) {
            AndroidUtilities.runOnUIThread {
                GominAiHistoryManager.addMessage("model", "Бро, спочатку введи свій API-ключ та вибери модель у меню чату! 🤖⚙️")
                notifyChatUpdated(currentAccount)
            }
            return
        }

        AndroidUtilities.runOnUIThread {
            setTypingStatus(true)
        }

        val configBuilder = GenerationConfig.Builder().apply {
            temperature = CherrygramMessagesConfig.geminiTemperatureValue.toFloat() / 10f
            topK = 10
            topP = 0.5f
            maxOutputTokens = 4096
        }

        val safetySettings = arrayListOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
        )

        val model = GenerativeModel(
            modelName,
            apiKey,
            configBuilder.build(),
            safetySettings
        )

        val modelFutures = GenerativeModelFutures.from(model)

        val rawHistory = GominAiHistoryManager.loadRawMessages()
        // Історія вже містить останнє надіслане повідомлення user, виключаємо його з масиву історії для prepareTurns
        val historyExcludingLast = if (rawHistory.isNotEmpty()) rawHistory.subList(0, rawHistory.size - 1) else ArrayList()

        val isShieldMode = activeShieldContext != null && activeShieldHistory != null

        val contents = prepareTurns(
            systemPrompt = CherrygramMessagesConfig.geminiSystemPrompt ?: "",
            history = historyExcludingLast,
            newQuery = newUserText,
            isShieldMode = isShieldMode,
            shieldPartner = activeShieldPartnerName,
            shieldContext = activeShieldContext
        )

        val responseFuture = modelFutures.generateContent(*contents.toTypedArray())
        val executor = ContextCompat.getMainExecutor(ApplicationLoader.applicationContext)

        Futures.addCallback(responseFuture, object : FutureCallback<com.google.ai.client.generativeai.type.GenerateContentResponse> {
            override fun onSuccess(result: com.google.ai.client.generativeai.type.GenerateContentResponse?) {
                val replyText = result?.text?.trim() ?: "Порожня відповідь від моделі ШІ."
                
                AndroidUtilities.runOnUIThread {
                    setTypingStatus(false)
                    GominAiHistoryManager.addMessage("model", replyText)
                    notifyChatUpdated(currentAccount)
                }
            }

            override fun onFailure(t: Throwable) {
                FileLog.e(t)
                val errorMessage = t.message ?: "Невідома помилка мережі ШІ."
                AndroidUtilities.runOnUIThread {
                    setTypingStatus(false)
                    GominAiHistoryManager.addMessage("model", "Помилка запиту ШІ: $errorMessage\n\nБро, можливо твій API-ключ не підходить або модель перевантажена (код 429). Перевір налаштування! ☕")
                    notifyChatUpdated(currentAccount)
                }
            }
        }, executor)
    }

    /**
     * Асинхронний аналіз маніпуляцій для BottomSheet "Щит"
     */
    fun analyzeManipulation(
        partnerName: String,
        historyText: String,
        callback: (success: Boolean, resultText: String) -> Unit
    ) {
        val apiKey = CherrygramMessagesConfig.geminiApiKey
        val modelName = CherrygramMessagesConfig.geminiModelName

        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(modelName)) {
            callback(false, "Бро, спочатку введи свій API-ключ та вибери модель в чаті ШІ! ⚙️")
            return
        }

        val configBuilder = GenerationConfig.Builder().apply {
            temperature = 0.3f // Низька температура для більш точного, аналітичного результату
            maxOutputTokens = 4096
        }

        val safetySettings = arrayListOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
        )

        val model = GenerativeModel(
            modelName,
            apiKey,
            configBuilder.build(),
            safetySettings
        )

        val modelFutures = GenerativeModelFutures.from(model)

        val promptContent = Content.Builder().apply {
            role = "user"
            text("Системна інструкція: $shieldSystemPrompt\n\nПроаналізуй наступну історію діалогу з $partnerName:\n$historyText")
        }.build()

        val responseFuture = modelFutures.generateContent(promptContent)
        val executor = ContextCompat.getMainExecutor(ApplicationLoader.applicationContext)

        Futures.addCallback(responseFuture, object : FutureCallback<com.google.ai.client.generativeai.type.GenerateContentResponse> {
            override fun onSuccess(result: com.google.ai.client.generativeai.type.GenerateContentResponse?) {
                val replyText = result?.text?.trim() ?: "Модель повернула порожній аналіз."
                AndroidUtilities.runOnUIThread {
                    callback(true, replyText)
                }
            }

            override fun onFailure(t: Throwable) {
                FileLog.e(t)
                val errorMessage = t.message ?: "Невідома помилка мережі."
                AndroidUtilities.runOnUIThread {
                    callback(false, "Помилка аналізу ШІ: $errorMessage\n\nМожливо, Gemini перевантажений (код 429). Спробуй ще раз трохи пізніше, бро! ☕")
                }
            }
        }, executor)
    }

    /**
     * Відкриває красивий нативний діалог вибору моделей прямо в чаті
     */
    fun showModelSelector(activity: ChatActivity) {
        val models = arrayOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp", "✏️ Ввести назву моделі вручну...")
        val currentModel = CherrygramMessagesConfig.geminiModelName
        
        var activeSelection = models.indexOf(currentModel)
        if (activeSelection == -1 && !TextUtils.isEmpty(currentModel)) {
            activeSelection = 3 // Інша модель
        }

        val builder = AlertDialog.Builder(activity.parentActivity, activity.resourceProvider)
        builder.setTitle("🤖 Вибрати модель Gemini")
        
        builder.setItems(models) { dialog: DialogInterface, which: Int ->
            if (which == 3) {
                dialog.dismiss()
                showCustomModelInput(activity)
            } else {
                val selectedModel = models[which]
                CherrygramMessagesConfig.geminiModelName = selectedModel
                dialog.dismiss()
                setTypingStatus(false) // Оновити заголовок з новою назвою моделі
                
                // Красивий фідбек
                AndroidUtilities.runOnUIThread {
                    val message = "Модель успішно змінено на $selectedModel! 🚀"
                    GominAiHistoryManager.addMessage("model", "🤖 Системне сповіщення: $message")
                    notifyChatUpdated(UserConfig.selectedAccount)
                }
            }
        }
        builder.setNegativeButton("Скасувати", null)
        builder.show()
    }

    private fun showCustomModelInput(activity: ChatActivity) {
        val builder = AlertDialog.Builder(activity.parentActivity, activity.resourceProvider)
        builder.setTitle("✏️ Ввести назву моделі")
        
        val editText = EditText(activity.parentActivity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "наприклад: gemini-2.5-flash"
            setText(CherrygramMessagesConfig.geminiModelName)
            setSelection(text.length)
        }

        val frameLayout = FrameLayout(activity.parentActivity).apply {
            setPadding(AndroidUtilities.dp(24f), AndroidUtilities.dp(10f), AndroidUtilities.dp(24f), AndroidUtilities.dp(10f))
            addView(editText)
        }
        builder.setView(frameLayout)

        builder.setPositiveButton("Зберегти") { dialog: DialogInterface, _ ->
            val customName = editText.text.toString().trim()
            if (!TextUtils.isEmpty(customName)) {
                CherrygramMessagesConfig.geminiModelName = customName
                setTypingStatus(false)
                
                AndroidUtilities.runOnUIThread {
                    val message = "Модель успішно змінено на $customName! 🚀"
                    GominAiHistoryManager.addMessage("model", "🤖 Системне сповіщення: $message")
                    notifyChatUpdated(UserConfig.selectedAccount)
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Скасувати", null)
        builder.show()
    }

    /**
     * Відкриває нативний діалог для очищення історії чату
     */
    fun showClearHistoryAlert(activity: ChatActivity) {
        val builder = AlertDialog.Builder(activity.parentActivity, activity.resourceProvider)
        builder.setTitle("🧹 Очистити історію чату")
        builder.setMessage("Бро, ти впевнений, що хочеш повністю очистити історію чату з Gomin AI? Цю дію неможливо скасувати.")
        builder.setPositiveButton("Очистити") { dialog: DialogInterface, _ ->
            GominAiHistoryManager.clearHistory()
            activeShieldContext = null
            activeShieldHistory = null
            notifyChatUpdated(UserConfig.selectedAccount)
            dialog.dismiss()
        }
        builder.setNegativeButton("Скасувати", null)
        builder.show()
    }

    private fun notifyChatUpdated(currentAccount: Int) {
        val objects = GominAiHistoryManager.loadMessages(currentAccount)
        NotificationCenter.getInstance(currentAccount).postNotificationName(
            NotificationCenter.messagesDidLoad,
            Constants.GOMIN_AI_DIALOG_ID,
            objects.size,
            objects,
            true,
            0,
            0,
            0,
            0,
            0,
            true,
            0,
            0,
            0,
            0,
            0
        )
    }

    fun setTypingStatus(isTyping: Boolean) {
        val activity = LaunchActivity.instance?.let { la ->
            val stack = la.actionBarLayout?.fragmentStack
            stack?.firstOrNull { it is ChatActivity && it.dialogId == Constants.GOMIN_AI_DIALOG_ID } as? ChatActivity
        } ?: return

        val rawModelName = CherrygramMessagesConfig.geminiModelName
        val model = rawModelName
        val friendlyModelName = when (model) {
            "gemini-1.5-flash" -> "Gemini 1.5 Flash"
            "gemini-1.5-pro" -> "Gemini 1.5 Pro"
            "gemini-2.0-flash-exp" -> "Gemini 2.0 Flash"
            null, "" -> "Gemini"
            else -> model.replace("-", " ").capitalize()
        }

        if (isTyping) {
            activity.actionBar?.setSubtitle("пише...")
        } else {
            activity.actionBar?.setSubtitle("$friendlyModelName • онлайн")
        }
    }
}
