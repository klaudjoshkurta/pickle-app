package com.shkurta.pickle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.ui.theme.Pickle

/** Small outlined icons drawn as vectors, matching the supplied screens. */
@Composable
fun TemplateIcon(kind: String, modifier: Modifier = Modifier) {
    val color = Pickle.colors.ink
    Canvas(modifier.size(22.dp)) {
        val w = size.width; val h = size.height; val stroke = 1.8.dp.toPx()
        when (kind) {
            "pizza" -> {
                for (x in listOf(.17f,.28f,.39f)) drawLine(color,Offset(w*x,h*.12f),Offset(w*x,h*.4f),stroke)
                drawLine(color,Offset(w*.28f,h*.4f),Offset(w*.28f,h*.88f),stroke)
                drawLine(color,Offset(w*.72f,h*.12f),Offset(w*.72f,h*.88f),stroke)
                drawLine(color,Offset(w*.72f,h*.12f),Offset(w*.58f,h*.52f),stroke)
                drawLine(color,Offset(w*.58f,h*.52f),Offset(w*.72f,h*.52f),stroke)
            }
            "place" -> {
                drawCircle(color,w*.3f,Offset(w*.5f,h*.4f),style=Stroke(stroke))
                drawCircle(color,w*.09f,Offset(w*.5f,h*.4f),style=Stroke(stroke))
                drawLine(color,Offset(w*.25f,h*.6f),Offset(w*.5f,h*.93f),stroke)
                drawLine(color,Offset(w*.75f,h*.6f),Offset(w*.5f,h*.93f),stroke)
            }
            "driver" -> {
                drawRoundRect(color,Offset(w*.15f,h*.3f),androidx.compose.ui.geometry.Size(w*.7f,h*.45f),androidx.compose.ui.geometry.CornerRadius(w*.1f),style=Stroke(stroke))
                drawCircle(color,w*.08f,Offset(w*.28f,h*.77f)); drawCircle(color,w*.08f,Offset(w*.72f,h*.77f))
                drawLine(color,Offset(w*.22f,h*.53f),Offset(w*.78f,h*.53f),stroke)
            }
            else -> {
                drawRoundRect(color,Offset(w*.13f,h*.15f),androidx.compose.ui.geometry.Size(w*.74f,h*.7f),androidx.compose.ui.geometry.CornerRadius(w*.08f),style=Stroke(stroke))
                if (kind == "date") {
                    drawLine(color,Offset(w*.13f,h*.35f),Offset(w*.87f,h*.35f),stroke)
                    for(x in listOf(.3f,.7f)) drawLine(color,Offset(w*x,h*.05f),Offset(w*x,h*.24f),stroke)
                } else if (kind == "game") {
                    for(pos in listOf(.3f to .32f,.7f to .32f,.5f to .5f,.3f to .68f,.7f to .68f)) drawCircle(color,w*.045f,Offset(w*pos.first,h*pos.second))
                } else {
                    for(x in listOf(.29f,.71f)) drawLine(color,Offset(w*x,h*.15f),Offset(w*x,h*.85f),stroke)
                    for(y in listOf(.34f,.65f)) drawLine(color,Offset(w*.13f,h*y),Offset(w*.29f,h*y),stroke)
                }
            }
        }
    }
}
