package app.ladefuchs.android.dataClasses

data class Operator(
    val displayName: String,
    val identifier: String,
    val types: List<String>,
    val updated: Long? = null,
    val image: String? = null
){
    override fun toString(): String = displayName
}
