package app.ladefuchs.android.dataClasses

data class Operator(
    val displayName: String,
    val identifier: String,
    val types: List<String>,
    val updated: Long,
    val image: String
){
    override fun toString(): String = displayName
}
