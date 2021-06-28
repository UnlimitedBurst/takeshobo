import io.ktor.http.content.*

//打包图片数据
data class ImageFileData(val romajiTitle: String, val data: List<PartData.FileItem>)
