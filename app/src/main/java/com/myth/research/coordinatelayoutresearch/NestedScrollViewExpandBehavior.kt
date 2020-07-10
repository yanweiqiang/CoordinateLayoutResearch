package com.myth.research.coordinatelayoutresearch

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.orhanobut.logger.Logger

class NestedScrollViewExpandBehavior : CoordinatorLayout.Behavior<NestedScrollView> {

    private val topSpacePercent = 0.2f
    private val logger = Logger.t("")
    private var lastInterceptY = 0f
    private var lastY = 0f
    private lateinit var viewConfiguration: ViewConfiguration

    constructor() : super()
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        viewConfiguration = ViewConfiguration.get(context)
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: NestedScrollView,
        dependency: View
    ): Boolean {
        return true
    }

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: NestedScrollView,
        ev: MotionEvent
    ): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                val deltaY = if (lastInterceptY == 0f) {
                    0f
                } else {
                    ev.rawY - lastInterceptY
                }
                lastInterceptY = ev.rawY

                val canScrollDown = child.canScrollVertically(-1)
                val childTop = child.top
                val topHigh = topHighPosition(parent)
                val topLow = topLowPosition(parent)

                if (deltaY > 0 && !canScrollDown && childTop < topLow) {
                    return true
                }

                if (deltaY < 0 && childTop > topHigh) {
                    return true
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                lastInterceptY = 0f
            }
        }
        return false
    }

    override fun onTouchEvent(
        parent: CoordinatorLayout,
        child: NestedScrollView,
        ev: MotionEvent
    ): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if (lastY == 0f) {
                    lastY = ev.rawY
                }
                var deltaY = ev.rawY - lastY
                lastY = ev.rawY

                logger.d("deltaY:$deltaY")

                val canScrollDown = child.canScrollVertically(-1)
                val childTop = child.top
                val topHigh = topHighPosition(parent)
                val topLow = topLowPosition(parent)

                if (deltaY > 0 && childTop < topLow && !canScrollDown) {

                    if (childTop + deltaY > topLow) {
                        deltaY = (topLow - childTop).toFloat()
                    }

                    ViewCompat.offsetTopAndBottom(child, deltaY.toInt())
                    return true
                }

                if (deltaY < 0 && childTop > topHigh) {

                    if (childTop + deltaY < topHigh) {
                        deltaY = (topHigh - childTop).toFloat()
                    }

                    ViewCompat.offsetTopAndBottom(child, deltaY.toInt())
                    return true
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                lastY = 0f
                //resetChild(parent, child)
            }
        }


        return true
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: NestedScrollView,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
            (View.MeasureSpec.getSize(parentHeightMeasureSpec) * (1f - topSpacePercent)).toInt(),
            View.MeasureSpec.getMode(parentHeightMeasureSpec)
        )
        child.measure(parentWidthMeasureSpec, heightMeasureSpec)
        return true
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: NestedScrollView,
        layoutDirection: Int
    ): Boolean {

        val top = (parent.measuredHeight * 2 * topSpacePercent).toInt()
        val bottom = (parent.measuredHeight * 2 * topSpacePercent).toInt() + child.measuredHeight

        child.layout(
            parent.left,
            top,
            parent.right,
            bottom
        )

        return true
    }


    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        target: View,
        type: Int
    ) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        /*if (dyConsumed > 0 && child.top <= coordinatorLayout.measuredHeight * topSpacePercent
            || dyConsumed < 0 && child.top > coordinatorLayout.measuredHeight * topSpacePercent * 2
        ) {
            return
        }

        ViewCompat.offsetTopAndBottom(child, -dyConsumed)*/
    }

    private fun topHighPosition(coordinatorLayout: CoordinatorLayout): Int {
        return (coordinatorLayout.measuredHeight * topSpacePercent).toInt()
    }

    private fun topLowPosition(coordinatorLayout: CoordinatorLayout): Int {
        return (coordinatorLayout.measuredHeight * topSpacePercent * 2).toInt()
    }
}