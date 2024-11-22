package slack


import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Slack(private val url: String) {

    @Throws(Exception::class)
    fun enviarMensagem(message: JSONObject) {
        val obj = URL(url)
        val con = obj.openConnection() as HttpURLConnection

        con.requestMethod = "POST"
        con.doOutput = true
        con.setRequestProperty("Content-Type", "application/json")

        OutputStreamWriter(con.outputStream, "UTF-8").use { writer ->
            writer.write(message.toString())
            writer.flush()
        }

        val responseCode = con.responseCode
        println("Enviando 'POST' para a URL: $url")
        println("Parâmetros do POST: ${message.toString()}")
        println("Response Code: $responseCode")

        val responseStream = if (responseCode == 200) con.inputStream else con.errorStream
        BufferedReader(InputStreamReader(responseStream)).use { reader ->
            val response = StringBuilder()
            reader.lines().forEach { line -> response.append(line) }
            println("Resposta: $response")
        }
    }
}
