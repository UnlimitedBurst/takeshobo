import react.dom.render
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event

fun main() {
    window.onload = {
        val webSocket=WebSocket("ws://localhost:8080${websocketPath}")
        webSocket.onopen={event: Event -> console.info("打开连接:${event}") }
        webSocket.onclose={event: Event -> console.info("关闭连接:${event}") }
        webSocket.onerror={event: Event -> console.error("发生错误:${event}") }
        render(document.getElementById("root")) {
            child(Welcome::class) {
                attrs {
                    this.webSocket=webSocket
                }
            }
        }
    }
}
