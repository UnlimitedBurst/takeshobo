import cool.kirito.bili.live.server.plugins.configureHTTP
import cool.kirito.bili.live.server.plugins.configureMonitoring
import cool.kirito.bili.live.server.plugins.configureSerialization
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.html.respondHtml
import io.ktor.http.*
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.*
import io.ktor.serialization.*
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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