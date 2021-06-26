import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event
import org.w3c.fetch.RequestInit
import org.w3c.xhr.FormData
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.*
import styled.css
import styled.styledDiv
import styled.styledH1
import styled.styledInput

external interface WelcomeProps : RProps {
    var webSocket:WebSocket
}

data class WelcomeState(var url:String="",var result:String="",var percentage:kotlin.Float=0F,var allowInput:Boolean=true) : RState

fun Double.format(digits: Int): String = this.asDynamic().toFixed(digits)
fun Float.format(digits: Int): String = this.asDynamic().toFixed(digits)

@OptIn(ExperimentalJsExport::class)
@JsExport
class Welcome(props: WelcomeProps) : RComponent<WelcomeProps, WelcomeState>(props) {

    init {
        state=WelcomeState()
        props.webSocket.onmessage={messageEvent: MessageEvent ->
            when(val data=messageEvent.data){
                is String-> {
                    if(data.contains("ptimg-version")){
                        val urlResult=Json.decodeFromString<UrlResult>(data)
                        console.info("ptimg_version:${urlResult.t.ptimg_version}")
                        ImageLoader(urlResult = urlResult).rebuild()
                    }else{
                        val task=Json.decodeFromString<ParseTask>(data)
                        state.result="解析进度：${task.percentage}%"
                        state.allowInput=(state.percentage==100F)
                        setState(state)
                    }
                }
                else->{
                    console.info("unknow data:${data}")
                }
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
                    attrs{
                        type=InputType.text
                        value = state.url
                        disabled = !state.allowInput
                        onChangeFunction = { event ->
                            (event.target as HTMLInputElement).let {
                                console.info(it.value)
                                state.url=it.value
                                setState(
                                    state
                                )
                            }

                        }
                    }
                }
                styledDiv {
                    css{
                        visibility=Visibility.hidden
                        height=0.px
                        paddingLeft=10.px
                    }
                    + state.url
                }
            }

            button {
                attrs {
                    disabled=!state.allowInput
                    onClickFunction={
                    state.allowInput=false
                    state.result="初始化解析任务请稍等"
                    setState(state)
                    val formData=FormData()
                        formData.append("url",state.url)
                        window.fetch("/api/json", RequestInit(method = "post",body = formData))
                        .then {
                            it.text()
                        }.then {
                            console.info(it)
                            state.result=Json.decodeFromString<MessageResponse>(it).message
                            setState(state)
                        }
                    }
                }
                +"开始解析"
            }

            div {
                +state.result
            }

            if(state.percentage>0F&&state.percentage<100F){
                button {
                    attrs {
                        onClickFunction={
                            props.webSocket.send("cancel")
                            state=WelcomeState()
                            setState(state)
                        }
                    }
                    +"取消解析任务"
                }
            }
        }

    }
}
