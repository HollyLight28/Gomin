package uz.unnarsx.cherrygram.preferences

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class GominAboutTest {

    @Test
    fun `test ukrainian cg_strings contains about story text`() {
        val xmlFile = File("TMessagesProj/src/main/res-cherrygram/values-uk/cg_strings.xml")
        assertTrue("XML file must exist", xmlFile.exists())

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlFile)
        doc.documentElement.normalize()

        val stringList = doc.getElementsByTagName("string")
        var storyText: String? = null
        var storyTitle: String? = null

        for (i in 0 until stringList.length) {
            val node = stringList.item(i)
            val nameAttr = node.attributes.getNamedItem("name")?.nodeValue
            if (nameAttr == "CGP_About_StoryText") {
                storyText = node.textContent
            } else if (nameAttr == "CGP_About_StoryTitle") {
                storyTitle = node.textContent
            }
        }

        assertNotNull("Story title string must be present", storyTitle)
        assertNotNull("Story text string must be present", storyText)
        assertTrue("Story text must mention Rivne", storyText!!.contains("Рівного"))
        assertTrue("Story text must mention Rivne", storyText.contains("брати та сестри"))
    }
}
