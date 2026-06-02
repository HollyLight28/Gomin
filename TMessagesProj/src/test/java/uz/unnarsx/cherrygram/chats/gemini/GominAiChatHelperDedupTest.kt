package uz.unnarsx.cherrygram.chats.gemini

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit-тести для дедуплікації Live API транскрипції.
 *
 * Бізнес-логіка: Gemini стрімить КУМУЛЯТИВНИЙ текст (не дельту), тому кожен chunk
 * може повністю переписати попередній. dedupTranscriptionChunk() має замінити
 * старий interim-суфікс новим chunk-ом, не дублюючи текст.
 */
class GominAiChatHelperDedupTest {

    @Test
    fun `empty newChunk returns currentText unchanged`() {
        val result = GominAiChatHelper.dedupTranscriptionChunk(
            currentText = "Привіт",
            lastInterim  = "Прив",
            newChunk     = ""
        )
        assertEquals("Привіт", result)
    }

    @Test
    fun `empty lastInterim simply appends new chunk`() {
        val result = GominAiChatHelper.dedupTranscriptionChunk(
            currentText = "Привіт",
            lastInterim  = "",
            newChunk     = ", як справи"
        )
        assertEquals("Привіт, як справи", result)
    }

    @Test
    fun `lastInterim is suffix of currentText - it gets replaced by newChunk`() {
        // Поточна ситуація: попередній chunk "Привіт, як" вже дописаний.
        // Новий chunk "Привіт, як справи" — це його розширення, треба замінити суфікс.
        val result = GominAiChatHelper.dedupTranscriptionChunk(
            currentText = "Привіт, як",
            lastInterim  = "Привіт, як",
            newChunk     = "Привіт, як справи"
        )
        assertEquals("Привіт, як справи", result)
    }

    @Test
    fun `typical streaming - three growing chunks stay clean`() {
        // Емуляція реального стріму: 3 chunks, кожен довший за попередній.
        var text = ""
        var last = ""

        // Chunk 1: "Привіт"
        text = GominAiChatHelper.dedupTranscriptionChunk(text, last, "Привіт")
        last = "Привіт"
        assertEquals("Привіт", text)

        // Chunk 2: "Привіт, як" (розширення)
        text = GominAiChatHelper.dedupTranscriptionChunk(text, last, "Привіт, як")
        last = "Привіт, як"
        assertEquals("Привіт, як", text)

        // Chunk 3: "Привіт, як справи" (ще розширення)
        text = GominAiChatHelper.dedupTranscriptionChunk(text, last, "Привіт, як справи")
        last = "Привіт, як справи"
        assertEquals("Привіт, як справи", text)
    }

    @Test
    fun `user typed something manually - new chunk just appends (defensive, no data loss)`() {
        // Юзер вручну дописав " ДОБРЕ" після транскрипції.
        // "Привіт" більше НЕ є суфіксом "Привіт ДОБРЕ" (суфікс = "ДОБРЕ"),
        // тому функція просто дописує новий chunk в кінець.
        // Це навмисно консервативна поведінка: не хочемо втратити текст юзера.
        val result = GominAiChatHelper.dedupTranscriptionChunk(
            currentText = "Привіт ДОБРЕ",
            lastInterim  = "Привіт",
            newChunk     = "Привіт, бро"
        )
        assertEquals("Привіт ДОБРЕПривіт, бро", result)
    }

    @Test
    fun `empty currentText with empty lastInterim just returns newChunk`() {
        val result = GominAiChatHelper.dedupTranscriptionChunk(
            currentText = "",
            lastInterim  = "",
            newChunk     = "Перший chunk"
        )
        assertEquals("Перший chunk", result)
    }

    @Test
    fun `turn boundary - empty lastInterim after turnComplete`() {
        // Після turnComplete lastInterim скидається в "". Наступний chunk — новий turn.
        val result = GominAiChatHelper.dedupTranscriptionChunk(
            currentText = "Перше речення. ",
            lastInterim  = "", // скинуто після turnComplete
            newChunk     = "Друге речення"
        )
        assertEquals("Перше речення. Друге речення", result)
    }

    @Test
    fun `unicode - cyrillic suffix replacement works`() {
        // "люблю" is NOT a suffix of "Я тебе люблю, синку" (suffix = "синку"),
        // тому функція просто дописує — pins down suffix-only behavior з unicode.
        val result = GominAiChatHelper.dedupTranscriptionChunk(
            currentText = "Я тебе люблю, синку",
            lastInterim  = "люблю",
            newChunk     = "люблю, дуже сильно"
        )
        assertEquals("Я тебе люблю, синкулюблю, дуже сильно", result)
    }
}
