package uz.unnarsx.cherrygram.alerts

object AirAlertHelper {
    fun shouldProcessAlert(pushRegionId: String?, userRegionId: String): Boolean {
        if (pushRegionId == null) return true // Якщо регіон у пуші пустий, це глобальний пуш, дозволяємо
        if (userRegionId.isEmpty()) return false // Якщо користувач не вибрав регіон, ігноруємо пуш із регіоном
        return pushRegionId == userRegionId
    }
}
