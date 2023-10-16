package app.ladefuchs.android.dataClasses

data class Operator(
    val displayName: String,
    val identifier: String,
    val types: List<String>,
    val updated: Long? = null,
    val image: String? = null
) {
    override fun toString(): String = displayName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Operator)
            return false
        if (this.displayName !== other.displayName) return false
        if (this.identifier !== other.identifier) return false
        if (this.types !== other.types) return false
        if (this.updated !== other.updated) return false
        if (this.image !== other.image) return false
        return true
    }
}
