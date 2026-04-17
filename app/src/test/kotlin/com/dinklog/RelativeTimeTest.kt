package com.dinklog

import com.dinklog.utils.DateMessages
import com.dinklog.utils.formatRelativeTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class RelativeTimeTest {

    @Test
    fun formatRelativeTime_returnsToday_forTimestampWithinLastHour() {
        val oneHourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        assertEquals(DateMessages.TODAY, oneHourAgo.formatRelativeTime())
    }

    @Test
    fun formatRelativeTime_returnsYesterday_forTimestamp25HoursAgo() {
        val twentyFiveHoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
        assertEquals(DateMessages.YESTERDAY, twentyFiveHoursAgo.formatRelativeTime())
    }

    @Test
    fun formatRelativeTime_returnsDaysAgo_forTimestamp3DaysAgo() {
        val threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        assertEquals("3 days ago", threeDaysAgo.formatRelativeTime())
    }
}
