package it.anak.bruneiazan.alarms

/** Pure wrap-around arithmetic for the D-pad time picker wheels. */
object TimeWheel {
    fun wrap(value: Int, count: Int): Int = ((value % count) + count) % count
}
