package com.barbie.ai

import android.graphics.BitmapFactory
import android.util.Base64

object BarbieBrand {
    private const val DP_B64 = """/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAH/AP/EABQQAQAAAAAAAAAAAAAAAAAAACD/2gAIAQEAAQUCf//EABQRAQAAAAAAAAAAAAAAAAAAACD/2gAIAQMBAT8Bf//EABQRAQAAAAAAAAAAAAAAAAAAACD/2gAIAQIBAT8Bf//EABQQAQAAAAAAAAAAAAAAAAAAACD/2gAIAQEAAT8hP//Z"""

    fun bitmap() = runCatching {
        val bytes = Base64.decode(DP_B64.replace("\\n", ""), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
