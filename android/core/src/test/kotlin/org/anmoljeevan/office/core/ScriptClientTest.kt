package org.anmoljeevan.office.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class ScriptClientTest {
    private fun client(answer: (String) -> String): Pair<ScriptClient, MutableList<String>> {
        val urls = mutableListOf<String>()
        val c = ScriptClient("https://script.google.com/macros/s/abc/exec", { url -> urls += url; answer(url) }, Dispatchers.Unconfined)
        return c to urls
    }

    @Test
    fun encodesParametersLikeEncodeURIComponent() {
        val (c, urls) = client { """{"ok":true}""" }
        runBlocking { c.call(listOf("name" to "Women's Meet — 28 Sep", "phone" to "+91 99999", "key" to "a&b=c")) }
        assertEquals(
            "https://script.google.com/macros/s/abc/exec?name=Women%27s%20Meet%20%E2%80%94%2028%20Sep&phone=%2B91%2099999&key=a%26b%3Dc",
            urls.single(),
        )
    }

    @Test
    fun unauthorizedBecomesItsOwnError() {
        val (c, _) = client { """{"error":"unauthorized"}""" }
        try {
            runBlocking { c.call(emptyList()) }
            fail()
        } catch (e: ApiException.Unauthorized) {
        }
    }

    @Test
    fun otherErrorsKeepTheirCode() {
        val (c, _) = client { """{"error":"too many ids"}""" }
        try {
            runBlocking { c.call(emptyList()) }
            fail()
        } catch (e: ApiException.Server) {
            assertEquals("too many ids", e.code)
        }
    }

    @Test
    fun htmlErrorPageIsABadResponse() {
        val (c, _) = client { "<html>Script function not found: doGet</html>" }
        try {
            runBlocking { c.call(emptyList()) }
            fail()
        } catch (e: ApiException.BadResponse) {
        }
    }

    @Test
    fun ioProblemIsANetworkError() {
        val (c, _) = client { throw IOException("timeout") }
        try {
            runBlocking { c.call(emptyList()) }
            fail()
        } catch (e: ApiException.Network) {
        }
    }

    @Test
    fun webAppUrlCleanUp() {
        assertEquals("https://script.google.com/macros/s/X/exec", WebAppUrl.normalize("  https://script.google.com/macros/s/X/exec?foo=1#x "))
        assertEquals("https://script.google.com/macros/s/X/exec", WebAppUrl.normalize("https://script.google.com/macros/s/X/exec/"))
        assertNotNull(WebAppUrl.problem(""))
        assertNotNull(WebAppUrl.problem("http://script.google.com/macros/s/X/exec"))
        assertNull(WebAppUrl.problem("https://script.google.com/macros/s/X/exec"))
        assertTrue(WebAppUrl.looksLikeAppsScript("https://script.google.com/macros/s/X/exec"))
        assertFalse(WebAppUrl.looksLikeAppsScript("https://example.com/"))
    }

    @Test
    fun textHelpers() {
        assertEquals("1,23,456", Text.number(123456))
        assertEquals("999", Text.number(999))
        assertEquals("12,34,56,789", Text.number(123456789))
        assertEquals("–", Text.numberOrDash(0))
        assertEquals("AB", Text.initials("  anita   bose "))
        assertEquals("S", Text.initials("sunil"))
        assertEquals("?", Text.initials(" "))
        assertEquals("&lt;b&gt; &amp; &quot;x&quot; &#39;", Text.html("<b> & \"x\" '"))
        assertTrue(Text.naturalCompare("Item 2", "Item 10") < 0)
        assertTrue(Text.naturalCompare("apple", "Banana") < 0)
        assertEquals(0, Text.naturalCompare("a", "a"))
    }
}
