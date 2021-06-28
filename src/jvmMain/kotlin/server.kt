import cool.kirito.bili.live.server.plugins.configureHTTP
import cool.kirito.bili.live.server.plugins.configureMonitoring
import cool.kirito.bili.live.server.plugins.configureSerialization
import io.ktor.application.*
import kotlinx.html.*
import plugins.configureWebSockets

fun HTML.index() {
    head {
        title(websiteTitle)
    }
    body {
        div {
            id = "root"
        }
        script(src = "/static/js.js") {}
    }
}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    configureWebSockets()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    parse()
}