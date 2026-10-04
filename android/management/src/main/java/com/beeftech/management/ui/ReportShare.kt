package com.beeftech.management.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.beeftech.management.data.ReportFile
import java.io.File

/*
 * Reports hold usernames, so they are written to the app's private cache (never
 * external storage) and shared through a FileProvider. Old files are cleared first.
 * The app declares the provider: authority "<applicationId>.fileprovider", with a
 * cache-path named "reports".
 */
object ReportShare {

    private const val DIR = "reports"

    fun shareIntent(context: Context, file: ReportFile): Intent {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }

        /* The name comes from the client, but keep any path part out of it anyway. */
        val target = File(dir, File(file.name).name)
        target.writeBytes(file.bytes)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = file.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share report").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
