package com.barbie.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface

object BarbieBrand {
    fun bitmap(): Bitmap {
        val size = 512
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(256f, 210f, 300f, intArrayOf(0xFFFF76CF.toInt(), 0xFFFF219D.toInt(), 0xFF160914.toInt()), null, Shader.TileMode.CLAMP) }
        c.drawCircle(256f, 256f, 250f, glow)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 9f; color = 0xFFFFD4F1.toInt(); setShadowLayer(20f,0f,0f,0xFFFF3BAF.toInt()) }
        c.drawCircle(256f, 256f, 239f, ring)
        val hair = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF32101F.toInt() }
        c.drawOval(105f, 65f, 407f, 365f, hair)
        val face = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFC7BC.toInt() }
        c.drawOval(138f, 98f, 374f, 335f, face)
        val pinkHair = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF4DB4.toInt(); strokeWidth = 24f; style = Paint.Style.STROKE }
        c.drawArc(105f, 65f, 407f, 365f, 205f, 130f, false, pinkHair)
        val eye = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5B173E.toInt() }
        c.drawOval(188f, 190f, 218f, 220f, eye); c.drawOval(294f, 190f, 324f, 220f, eye)
        val mouth = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE83E91.toInt(); style = Paint.Style.STROKE; strokeWidth = 6f }
        c.drawArc(220f, 235f, 292f, 285f, 10f, 160f, false, mouth)
        val jacket = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF180E1B.toInt() }
        c.drawRoundRect(105f, 325f, 407f, 470f, 45f, 45f, jacket)
        val mic = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFE8F8.toInt(); style = Paint.Style.STROKE; strokeWidth = 11f }
        c.drawRoundRect(345f, 225f, 390f, 290f, 22f, 22f, mic); c.drawArc(330f, 250f, 405f, 325f, 0f, 180f, false, mic); c.drawLine(368f,325f,368f,345f,mic)
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 48f; setShadowLayer(12f,0f,0f,0xFFFF39AA.toInt()) }
        c.drawText("Barbie", 256f, 410f, label)
        val ai = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; textSize = 34f }
        c.drawText("AI", 256f, 452f, ai)
        return b
    }
}
