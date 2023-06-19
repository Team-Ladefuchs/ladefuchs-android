package app.ladefuchs.android.dataClasses

enum class ChargeType {
    AC,
    DC;

    override fun toString(): String {
        return name.lowercase()
    }
}