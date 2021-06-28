import kotlinx.browser.window
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.fetch.RequestInit
import org.w3c.xhr.FormData
import react.*
import react.dom.*
import styled.*

external interface WelcomeProps : RProps {
    var webSocket: WebSocket
}


data class WelcomeState(
    var inputValue: String = "",
    var result: String = "",
    var percentage: kotlin.Float = 0F,
    var allowInput: Boolean = true,
    var mangaInfo: MangaInfo? = null,
    var zipInfo: ZipResult? = null
) : RState

@OptIn(ExperimentalJsExport::class)
@JsExport
class Welcome(props: WelcomeProps) : RComponent<WelcomeProps, WelcomeState>(props) {

    init {
        state = WelcomeState()
        props.webSocket.apply {
            onmessage = { messageEvent: MessageEvent ->
                when (val data = messageEvent.data) {
                    is String -> {
                        try {
                            val res = globalJson.decodeFromString<WebSocketServerType>(data)
                            when (res.dataType) {
                                WebSocketResType.Heart.name -> {
                                    console.info("响应心跳")
                                }
                                WebSocketResType.Text.name -> {
                                    globalJson.decodeFromString<WebSocketServer<String>>(data).apply {
                                        console.info("message:${body}")
                                        setState {
                                            result = body
                                        }
                                    }

                                }
                                WebSocketResType.Task.name -> {
                                    globalJson.decodeFromString<WebSocketServer<ParseTask>>(data).apply {
                                        setState {
                                            percentage = body.percentage
                                            result =
                                                "共${body.total}张图片，解析进度：${body.percentage}%(${body.finish}/${body.total})"
                                            allowInput = (body.percentage == 100F)
                                        }
                                    }

                                }
                                WebSocketResType.Image.name -> {
                                    globalJson.decodeFromString<WebSocketServer<UrlResult>>(data).apply {
                                        ImageLoader(urlResult = body).rebuild().then {
                                            when {
                                                it && body.isLast -> {
                                                    setState {
                                                        result = "打包漫画图片"
                                                    }
                                                    ImageDataManager.requestZip(urlResult = body)
                                                }
                                                it -> console.info("漫画地址${body.serverImagePath}解析成功")
                                                else -> console.info("漫画地址${body.serverImagePath}解析失败")
                                            }
                                        }
                                    }

                                }
                                WebSocketResType.Zip.name -> {
                                    globalJson.decodeFromString<WebSocketServer<ZipResult>>(data).apply {
                                        console.info("返回压缩包：${body}")
                                        setState {
                                            allowInput = true
                                            inputValue = ""
                                            zipInfo = body
                                        }
                                    }

                                }
                                WebSocketResType.Manga.name -> {
                                    globalJson.decodeFromString<WebSocketServer<MangaInfo>>(data).apply {
                                        setState {
                                            mangaInfo = body
                                        }
                                    }
                                }
                                WebSocketResType.Cancel.name -> {
                                    globalJson.decodeFromString<WebSocketServer<Boolean>>(data).apply {
                                        if (body) {
                                            setState(WelcomeState())
                                        }
                                    }
                                }
                                else -> {
                                    console.warn("返回未知数据：${data}")
                                }
                            }
                        } catch (e: Exception) {
                            console.error(e)
                        }
                    }
                    else -> {
                        console.info("unknow data:${data}")
                    }
                }
            }
        }

        window.onbeforeunload = {
            console.info("窗口即将被关闭")
            if (!state.allowInput) {
                val msg = "漫画解析任务正在执行，关闭窗口将自动取消当前解析任务并且无法恢复"
                it.returnValue = msg
                msg
            } else {
                props.webSocket.close()
                console.info("关闭websocket")
                null
            }
        }
    }

    override fun RBuilder.render() {

        styledH1 {
            css{
                textAlign=TextAlign.center
            }
            +"朴实无华的"
            a {
                attrs {
                    href="https://gammaplus.takeshobo.co.jp"
                    target="_blank"
                }
                +"takeshobo"
            }
            +"漫画解析工具"
        }

        styledDiv{
            css{
                textAlign=TextAlign.center
            }
            styledDiv {
                css {
                    width = LinearDimension.fitContent
                    margin="0 auto"
                }
                styledInput {
                    css {
                        width = 100.pct
                    }
                    attrs {
                        type = InputType.text
                        placeholder = "请拷贝完整的漫画阅读页地址到此处。"
                        value = state.inputValue
                        disabled = !state.allowInput
                        onChangeFunction = { event ->
                            (event.target as HTMLInputElement).let {
                                console.info("inpiut url:${it.value}")
                                setState {
                                    inputValue = it.value
                                }
                            }

                        }
                    }
                }
                styledDiv {
                    +"输入示例："
                    styledUl {
                        styledLi {
                            styledA {
                                val href = "https://gammaplus.takeshobo.co.jp/manga/kobito_recipe/_files/02_1/"
                                attrs {
                                    target = "_blank"
                                    this.href = href
                                }
                                +href
                            }
                        }
                    }
                }
                styledDiv {
                    css {
                        visibility = Visibility.hidden
                        height = 0.px
                        paddingLeft = 10.px
                    }
                    +state.inputValue
                }
            }

            button {
                attrs {
                    disabled = !state.allowInput
                    onClickFunction = {
                        setState {
                            allowInput = false
                            result = "初始化解析任务"
                            zipInfo = null
                            mangaInfo = null
                        }
                        val formData = FormData()
                        formData.append("url", state.inputValue)
                        window.fetch(Api.JSON_API, RequestInit(method = "post", body = formData))
                    }
                }
                +"开始解析"
            }

            div {
                state.mangaInfo?.let {
                    h1 {
                        a(href = it.href, target = "_blank") {
                            +"${it.title}(${it.romajiTitle})"
                        }
                    }
                }
                state.zipInfo?.let {
                    h2 {
                        a(href = it.zipUrl, target = "_blank") {
                            +"下载${it.name}(${it.size})"
                        }
                    }
                }
                h3 {
                    +state.result
                }
            }

            if(state.percentage>0F&&state.percentage<100F){
                button {
                    attrs {
                        onClickFunction={
                            props.webSocket.sendCommand(command = WebSocketClientCommand.Cancel)
                        }
                    }
                    +"取消解析任务"
                }
            }
        }

    }
}
