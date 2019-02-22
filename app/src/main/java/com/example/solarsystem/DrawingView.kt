package com.example.solarsystem

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Created by gmartins on 2019-02-20.
 */
class DrawingView constructor(context: Context, attributeSet: AttributeSet): ConstraintLayout(context, attributeSet) {

    private var colour = Color.GREEN
    private var drawPaint = Paint()
    private var circlePoints: ArrayList<Point>? = null
    private var path = Path()

    init {
        setWillNotDraw(false)

        circlePoints = ArrayList()
        setupPaint()

    }

    private fun setupPaint(){
        drawPaint = Paint()
        drawPaint.color = colour
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = 5f
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas?) {
        canvas!!.drawPath(path, drawPaint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val pointX = event!!.x
        val pointY = event.y
        when(event.action){
            MotionEvent.ACTION_DOWN -> path.moveTo(pointX, pointY)
            MotionEvent.ACTION_MOVE -> path.lineTo(pointX, pointY)
        }
        // indicate view should be redrawn
        postInvalidate()
        return true
    }

}