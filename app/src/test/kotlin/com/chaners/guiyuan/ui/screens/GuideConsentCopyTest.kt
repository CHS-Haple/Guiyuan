package com.chaners.guiyuan.ui.screens

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideConsentCopyTest {
    private fun label(locale: String, name: String): String {
        val relative = "src/main/res/$locale/strings.xml"
        val file =
            listOf(File(relative), File("app/$relative"))
                .firstOrNull(File::isFile)
                ?: error("Missing Android string resources: $relative")
        val nodes = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
            .getElementsByTagName("string")
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node.attributes?.getNamedItem("name")?.nodeValue == name) {
                return node.textContent
            }
        }
        error("Missing string resource: $name")
    }

    @Test
    fun oneCheckboxExplicitlyAgreesToBothLinkedDocumentsInChineseAndEnglish() {
        val expected = mapOf(
            "values-zh-rCN" to "我已阅读并同意《使用条款》和《隐私政策》",
            "values" to "I have read and agree to the Terms of Use and Privacy Policy",
        )
        expected.forEach { (locale, wording) ->
            val actual = label(locale, "guide_privacy_ack")
            assertEquals(wording, actual)
            for (key in listOf("guide_terms_link_text", "guide_policy_link_text")) {
                val link = label(locale, key)
                assertTrue("Missing $key hyperlink label in $locale", actual.contains(link))
                assertEquals(
                    "Link label $key must occur exactly once in $locale",
                    1,
                    actual.windowed(link.length).count { it == link },
                )
            }
        }
    }
}
