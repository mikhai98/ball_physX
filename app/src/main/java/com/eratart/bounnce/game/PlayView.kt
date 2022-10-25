package com.eratart.bounnce.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.eratart.bounnce.R
import kotlin.math.abs

class PlayView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {

    companion object {
        private const val ACCELERATION = 9810f
        private const val JUMP_EFF_X = 0.98f
        private const val JUMP_EFF_Y = 0.8f
        private const val FRICTION_EFF_X = 0.85f
        private const val FRICTION_EFF_Y = 0.7f

        private const val SPEED_DELETE = 5f

        private const val TIME_MULTIPLIER = 1000f

        private const val RADIUS = 25f

        private const val BOUNDS_OFFSET = 200f
    }

    private val paint by lazy {
        Paint().apply {
            color = ContextCompat.getColor(context, R.color.feedback_win)
        }
    }

    private val paintAlpha by lazy {
        Paint().apply {
            color = ContextCompat.getColor(context, R.color.feedback_win)
            alpha = 128
            strokeWidth = RADIUS / 2
        }
    }
    private val walls = mutableListOf<Wall>()
    private var balls = mutableListOf<Ball>()
    private var preview: Preview? = null

    private var startPosition: Position? = null
    private var endPosition: Position? = null

    private val playRect by lazy {
        RectF(-BOUNDS_OFFSET,
            -BOUNDS_OFFSET,
            width.toFloat() + BOUNDS_OFFSET,
            height.toFloat() + BOUNDS_OFFSET)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        balls.removeIf { it.readyToDelete }

        balls.forEach {
            onUpdate(it)
            canvas.drawCircle(it.center.first, it.center.second, it.radius, it.paint)
        }

        preview?.apply {
            if (start != end) {
                canvas.drawCircle(start.first, start.second, RADIUS, paint)
                canvas.drawLine(start.first, start.second, end.first, end.second, paintAlpha)
            }
        }

        walls.forEach {
            canvas.drawRect(it.rectF, paint)
        }

        invalidate()
    }

    private fun onUpdate(ball: Ball) {
        val time = System.currentTimeMillis()

        val timePassed = (time - ball.previousTime) / TIME_MULTIPLIER
        val distanceX = timePassed * ball.speedX
        val newPosX = ball.center.first + distanceX
        val distanceY = timePassed * ball.speedY
        val newPosY = ball.center.second + distanceY

        if (playRect.contains(newPosX, newPosY)) {
            ball.center = Pair(newPosX, newPosY)

            walls.forEach { wall ->
                val hint = wall.getHint(RADIUS)
                if (hint.contains(ball.center.first, ball.center.second)) {
                    val distances = mutableListOf<Pair<Float, CollisionType>>()
                    distances.add(Pair(ball.center.first - hint.left, CollisionType.LEFT))
                    distances.add(Pair(ball.center.second - hint.top, CollisionType.TOP))
                    distances.add(Pair(hint.right - ball.center.first, CollisionType.RIGHT))
                    distances.add(Pair(hint.bottom - ball.center.second, CollisionType.BOTTOM))

                    when (distances.minBy { it.first }.second) {
                        CollisionType.TOP -> {
                            ball.center = Pair(ball.center.first, hint.top - 1f)
                            val newSpeed = -ball.speedY * JUMP_EFF_Y
                            ball.speedY = if (newSpeed > -150f) 0f else newSpeed
                            ball.speedX = ball.speedX * FRICTION_EFF_X
                        }
                        CollisionType.BOTTOM -> {
                            ball.center = Pair(ball.center.first, hint.bottom + 1f)
                            ball.speedY = -ball.speedY
                            ball.speedX = ball.speedX * FRICTION_EFF_X
                        }
                        CollisionType.LEFT -> {
                            ball.center = Pair(hint.left - 1f, ball.center.second)
                            ball.speedX = -ball.speedX * JUMP_EFF_X
                            ball.speedY = ball.speedY * FRICTION_EFF_Y
                        }
                        CollisionType.RIGHT -> {
                            ball.center = Pair(hint.right + 1f, ball.center.second)
                            ball.speedX = -ball.speedX * JUMP_EFF_X
                            ball.speedY = ball.speedY * FRICTION_EFF_Y
                        }
                    }
                }
            }
            val isOnWall = walls.any { abs(it.rectF.top - ball.center.second) < ball.radius + 10f }
            if (abs(ball.speedY) < SPEED_DELETE && abs(ball.speedX) < SPEED_DELETE && isOnWall) {
                ball.readyToDelete = true
            } else {
                ball.speedY = ball.speedY + ACCELERATION * timePassed
            }
        } else {
            ball.readyToDelete = true
        }
        ball.previousTime = System.currentTimeMillis()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        val eventX = event.x
        val eventY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> onFingerDown(eventX, eventY)
            MotionEvent.ACTION_MOVE -> onFingerMove(eventX, eventY)
            MotionEvent.ACTION_UP -> onFingerUp(eventX, eventY)
        }

        invalidate()
        return true
    }

    private fun onFingerDown(eventX: Float, eventY: Float) {
        var proceed = true
        walls.forEach {
            if (it.getHint(RADIUS).contains(eventX, eventY)) {
                proceed = false
            }
        }
        if (!proceed) return
        startPosition = Position(eventX, eventY, System.currentTimeMillis())
        endPosition = startPosition
        preview = Preview(Pair(eventX, eventY), Pair(eventX, eventY))
    }

    private fun onFingerMove(eventX: Float, eventY: Float) {
        if (startPosition == null) return
        endPosition = Position(eventX, eventY, System.currentTimeMillis())
        val start = startPosition ?: return
        val end = endPosition ?: return

        val diffX = end.x - start.x
        val diffY = end.y - start.y

        val endX = start.x - diffX / 5
        val endY = start.y - diffY / 5

        preview = Preview(Pair(start.x, start.y), Pair(endX, endY))
    }

    private fun onFingerUp(eventX: Float, eventY: Float) {
        val start = startPosition ?: return
        val end = endPosition ?: return

        preview = null

        val time = (end.time - start.time) / TIME_MULTIPLIER

        val speedX = (end.x - start.x) * 10f/// time
        val speedY = (end.y - start.y) * 10f/// time
        val maxSpeed = 4500f
        val realSpeedX = if (speedX > maxSpeed) maxSpeed else speedX
        val realSpeedY = if (speedY > maxSpeed) maxSpeed else speedY
        balls.add(Ball(Pair(start.x, start.y), RADIUS, paint, -realSpeedX, -realSpeedY))

        postInvalidate()
        startPosition = null
        endPosition = null
    }

    fun setup() {
        val centerX = width / 2f
        val centerY = height / 2f

        val lineWidth = BOUNDS_OFFSET

        val rectF2 = RectF(0f, centerY - lineWidth / 2, centerX + 200f, centerY + lineWidth / 2)
        val rectF3 =
            RectF(centerX - 100f, centerY + 400f, width.toFloat(), centerY + 400f + lineWidth)

        val left = RectF(
            -BOUNDS_OFFSET,
            0f,
            0f,
            height.toFloat()
        )
        val top = RectF(
            -BOUNDS_OFFSET,
            -BOUNDS_OFFSET,
            width.toFloat() + BOUNDS_OFFSET,
            0f
        )
        val right = RectF(
            width.toFloat(),
            0f,
            width + BOUNDS_OFFSET,
            height.toFloat()
        )
        val bottom = RectF(
            -BOUNDS_OFFSET,
            height.toFloat(),
            width.toFloat() + BOUNDS_OFFSET,
            height.toFloat() + BOUNDS_OFFSET
        )

        walls.add(Wall(rectF2))
        walls.add(Wall(rectF3))

        walls.add(Wall(left))
        walls.add(Wall(top))
        walls.add(Wall(right))
        walls.add(Wall(bottom))
    }

    inner class Ball(
        var center: Pair<Float, Float>,
        val radius: Float,
        val paint: Paint,
        var speedX: Float = 0f,
        var speedY: Float = 0f,
        var previousTime: Long = System.currentTimeMillis(),
        var readyToDelete: Boolean = false,
    )

    inner class Position(
        val x: Float,
        val y: Float,
        val time: Long,
    )

    inner class Preview(
        val start: Pair<Float, Float>,
        val end: Pair<Float, Float>,
    )

    inner class Wall(val rectF: RectF) {
        fun getHint(radius: Float): RectF {
            return RectF(
                rectF.left - radius,
                rectF.top - radius,
                rectF.right + radius,
                rectF.bottom + radius
            )
        }
    }

    enum class CollisionType {
        TOP, LEFT, BOTTOM, RIGHT
    }
}