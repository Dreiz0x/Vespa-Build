package dev.vskelk.cdf.core.util

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashReporter(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            // Generar el nombre del archivo con la fecha y hora exacta del putazo
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "crash_vespa_$timeStamp.txt"
            
            // Lo guardamos en una ruta que puedes ver con el explorador de archivos de tu cel
            val dir = context.getExternalFilesDir(null)
            if (dir != null) {
                val file = File(dir, fileName)
                val writer = PrintWriter(FileWriter(file))
                
                writer.println("=== VESPA FATAL CRASH REPORT ===")
                writer.println("Hora: $timeStamp")
                writer.println("Hilo: ${thread.name}")
                writer.println("Excepción: ${exception.javaClass.name}")
                writer.println("Mensaje: ${exception.message}")
                writer.println("=== STACKTRACE EXACTO ===")
                exception.printStackTrace(writer)
                writer.flush()
                writer.close()
            }
        } catch (e: Exception) {
            // Si falla guardando el archivo, nos la pelamos, pero no detenemos el crash
        } finally {
            // Le pasamos la bolita al sistema para que cierre la app
            defaultHandler?.uncaughtException(thread, exception) ?: exitProcess(1)
        }
    }

    companion object {
        fun instalar(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashReporter(context))
        }
    }
}
