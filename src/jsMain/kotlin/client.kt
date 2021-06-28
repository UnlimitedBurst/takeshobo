import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event
import react.dom.render

val globalJson = Json { ignoreUnknownKeys = true }

fun WebSocket.sendCommand(command: WebSocketClientCommand) {
    send(globalJson.encodeToString(WebSocketClient(command = command.name)))
}

fun main() {
    window.onload = {
        val webSocket =
            WebSocket("${if (window.location.protocol == "https:") "wss" else "ws"}://${window.location.host}${Api.websocketPath}")
        webSocket.onopen = { event: Event -> console.info("打开连接:${event}") }
        webSocket.onclose = { event: Event -> console.info("关闭连接:${event}") }
        webSocket.onerror = { event: Event -> console.error("发生错误:${event}") }
        window.setInterval({
            webSocket.sendCommand(command = WebSocketClientCommand.Heart)
        }, 60000)
        render(document.getElementById("root")) {
            child(Welcome::class) {
                attrs {
                    this.webSocket = webSocket
                }
            }
        }
    }
}

