package org.anmoljeevan.office.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import org.anmoljeevan.office.core.admin.Phones
import java.io.File

/** Opening other apps: WhatsApp, the dialler, email, the share sheet, printing. */
object Launch {

    private fun start(context: Context, intent: Intent, failMessage: String): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, failMessage, Toast.LENGTH_SHORT).show()
        false
    }

    /** Opens a WhatsApp chat (pre-filled with [message] when given). */
    fun whatsApp(context: Context, phone: String, message: String? = null): Boolean {
        val url = Phones.whatsAppUrl(phone, message) ?: run {
            Toast.makeText(context, "No phone number", Toast.LENGTH_SHORT).show()
            return false
        }
        return start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "WhatsApp isn't installed")
    }

    fun dial(context: Context, phone: String): Boolean {
        val digits = phone.filter { it.isDigit() || it == '+' }
        if (digits.isEmpty()) return false
        return start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")), "No phone app found")
    }

    fun email(context: Context, address: String): Boolean {
        if (address.isBlank()) return false
        return start(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address.trim())), "No email app found")
    }

    fun openUrl(context: Context, url: String): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "Couldn't open the link")

    fun copy(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    /** Writes [content] to a CSV in the app's cache and opens the share sheet (Drive, WhatsApp, Files…). */
    fun shareCsv(context: Context, fileName: String, content: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 86_400_000L) it.delete() }
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(context, Intent.createChooser(send, "Export $fileName"), "No app to share with")
    }

    // Kept until the print job has laid the page out; a WebView that is collected too early prints nothing.
    @SuppressLint("StaticFieldLeak")
    private var printView: WebView? = null

    /** Prints (or saves as PDF) an HTML page through Android's print dialog. */
    fun printHtml(context: Context, jobName: String, html: String) {
        val view = WebView(context)
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, url: String?) {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                pm.print(jobName, v.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
            }
        }
        view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        printView = view
    }
}
