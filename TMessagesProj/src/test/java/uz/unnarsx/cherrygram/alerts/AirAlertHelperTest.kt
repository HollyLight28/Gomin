package uz.unnarsx.cherrygram.alerts

import org.junit.Assert.assertEquals
import org.junit.Test

class AirAlertHelperTest {

    @Test
    fun `shouldProcessAlert returns true when pushRegionId is null`() {
        val result = AirAlertHelper.shouldProcessAlert(null, "19")
        assertEquals(true, result)
    }

    @Test
    fun `shouldProcessAlert returns false when userRegionId is empty`() {
        val result = AirAlertHelper.shouldProcessAlert("19", "")
        assertEquals(false, result)
    }

    @Test
    fun `shouldProcessAlert returns true when regionIds match`() {
        val result = AirAlertHelper.shouldProcessAlert("19", "19")
        assertEquals(true, result)
    }

    @Test
    fun `shouldProcessAlert returns false when regionIds mismatch`() {
        val result = AirAlertHelper.shouldProcessAlert("19", "25")
        assertEquals(false, result) // This will fail because dummy returns true
    }
}
