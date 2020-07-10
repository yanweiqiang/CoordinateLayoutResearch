package com.myth.research.coordinatelayoutresearch

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.*
import android.widget.ScrollView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ScrollingView
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.SCROLL_AXIS_VERTICAL
import androidx.core.view.get
import androidx.dynamicanimation.animation.DynamicAnimation.SCROLL_Y
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.orhanobut.logger.Logger

class ViewGroupExpandBehavior : CoordinatorLayout.Behavior<ViewGroup> {
    private val logger = Logger.t("")

    private var mParent: CoordinatorLayout? = null
    private var mChild: ViewGroup? = null

    private var offsetPercent = 0.2f
    private var offsetDimension = -1
    private var expandPercent = 0.2f
    private var expandDimension = -1

    private var lastInterceptX = 0f
    private var lastInterceptY = 0f
    private var lastY = 0f

    //layout child
    private var mLayoutTop = -1

    //park child high or low
    private var animator: ValueAnimator? = null
    private var flingAnimation = FlingAnimation(FloatValueHolder())
    private lateinit var gestureDetector: GestureDetector

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            return false
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {

            logger.d("velocityY: $velocityY")
            val realParent = mParent ?: return false
            val realChild = mChild ?: return false

            if (isHighest(realParent, realChild) || isLowest(realParent, realChild)) {
                return false
            }

            val childTop = realChild.top
            val high = topHighPosition(realParent)
            val low = topLowPosition(realParent)


            logger.d("minValue: $high, maxValue: $low, startValue: $childTop")

            val highPart = inHighPart(realParent, realChild)

            if (flingAnimation.isRunning) {
                flingAnimation.cancel()
            }

            flingAnimation.apply {
                setStartVelocity(velocityY)
                setMinValue(high.toFloat())
                setMaxValue(low.toFloat())
                setStartValue(childTop.toFloat())
                addUpdateListener { _, value, velocity ->
                    val curTop = realChild.top

                    offsetTopAndBottom(realParent, realChild, (value - curTop).toInt())

                    logger.d("value: $value, velocity: $velocity")
                }
                addEndListener { animation, canceled, value, velocity ->
                    //park(realParent, realChild)
                }
                friction = 1f
                start()
            }

            return true
        }
    }

    /**
     * filter touch cancel event in [CoordinatorLayout.resetTouchBehaviors]
     */
    private var inRealTouch = false

    private lateinit var viewConfiguration: ViewConfiguration

    constructor() : super()
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        viewConfiguration = ViewConfiguration.get(context)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.layoutExpandable)
        offsetPercent = ta.getFloat(
            R.styleable.layoutExpandable_layoutExpandable_offset_percent,
            offsetPercent
        )
        offsetDimension = ta.getDimensionPixelSize(
            R.styleable.layoutExpandable_layoutExpandable_offset_dimension,
            offsetDimension
        )
        expandPercent =
            ta.getFloat(R.styleable.layoutExpandable_layoutExpandable_expand_percent, expandPercent)
        expandDimension =
            ta.getDimensionPixelOffset(
                R.styleable.layoutExpandable_layoutExpandable_expand_dimension,
                expandDimension
            )
        ta.recycle()

        gestureDetector = GestureDetector(context, gestureListener)
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: ViewGroup,
        dependency: View
    ): Boolean {
        return true
    }

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: ViewGroup,
        ev: MotionEvent
    ): Boolean {

        if (ev.y < child.top) {
            //park(parent, child)
            return false
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastInterceptX = 0f
                lastInterceptY = 0f
            }
            MotionEvent.ACTION_MOVE -> {

                val deltaX = if (lastInterceptX == 0f) {
                    0f
                } else {
                    ev.x - lastInterceptX
                }

                lastInterceptX = ev.x

                val deltaY = if (lastInterceptY == 0f) {
                    0f
                } else {
                    ev.y - lastInterceptY
                }

                lastInterceptY = ev.y

                if (deltaY != 0f && deltaX / deltaY > 1f) {
                    return false
                }

                val canScrollDown = canScrollDown(child)
                val childTop = child.top
                val topHigh = topHighPosition(parent)
                val topLow = topLowPosition(parent)

                if (deltaY > 0 && childTop < topLow && !canScrollDown) {
                    return true
                }

                if (deltaY < 0 && childTop > topHigh) {
                    return true
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                lastInterceptY = 0f
                /*if (inRealTouch) {
                    park(parent, child)
                }*/
            }
        }

        return false
    }


    override fun onTouchEvent(
        parent: CoordinatorLayout,
        child: ViewGroup,
        ev: MotionEvent
    ): Boolean {
        if (ev.y < child.top) {
            //park(parent, child)
            return false
        }

        if (gestureDetector.onTouchEvent(ev)) {
            return true
        }

        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                inRealTouch = true

                if (animator?.isRunning == true) {
                    animator?.cancel()
                }

                if (flingAnimation.isRunning) {
                    flingAnimation.cancel()
                }

                if (lastY == 0f) {
                    lastY = ev.y
                }

                var deltaY = ev.y - lastY
                lastY = ev.y

                val canScrollDown = canScrollDown(child)
                val childTop = child.top
                val topHigh = topHighPosition(parent)
                val topLow = topLowPosition(parent)

                if (deltaY > 0 && childTop < topLow && !canScrollDown) {

                    if (childTop + deltaY > topLow) {
                        deltaY = (topLow - childTop).toFloat()
                    }

                    offsetTopAndBottom(parent, child, deltaY.toInt())
                    return true
                }

                if (deltaY < 0 && childTop > topHigh) {

                    if (childTop + deltaY < topHigh) {
                        deltaY = (topHigh - childTop).toFloat()
                    }

                    offsetTopAndBottom(parent, child, deltaY.toInt())
                    return true
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                lastY = 0f
                /*if (inRealTouch) {
                    park(parent, child)
                }*/
            }
        }

        return true
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: ViewGroup,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        mParent = parent
        mChild = child

        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
            (View.MeasureSpec.getSize(parentHeightMeasureSpec) - topHighPosition(parent)),
            View.MeasureSpec.getMode(parentHeightMeasureSpec)
        )
        child.measure(parentWidthMeasureSpec, heightMeasureSpec)
        return true
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: ViewGroup,
        layoutDirection: Int
    ): Boolean {

        val top = if (mLayoutTop == -1) topLowPosition(parent) else mLayoutTop
        val bottom = top + child.measuredHeight

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
        child: ViewGroup,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return (axes and SCROLL_AXIS_VERTICAL) != 0
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: ViewGroup,
        target: View,
        type: Int
    ) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: ViewGroup,
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
        child: ViewGroup,
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

        offsetTopAndBottom(child, -dyConsumed)*/
    }

    private fun canScrollDown(child: ViewGroup): Boolean {
        var sv: View? = null

        (0 until child.childCount).forEach {
            val view = child[it]

            if (view is ViewPager) {
                val adapter = view.adapter
                if (adapter is FragmentPagerAdapter) {
                    sv = findScrollingView(adapter.getItem(view.currentItem).view as ViewGroup)
                } else if (adapter is PagerAdapter) {
                    sv = findScrollingView(
                        adapter.instantiateItem(
                            view,
                            view.currentItem
                        ) as ViewGroup
                    )
                }
            }

            if (sv == null && view is ViewGroup) {
                sv = findScrollingView(view)
            }
        }

        return sv?.canScrollVertically(-1) == true
    }

    /**
     * find ScrollView NestedScrollView RecyclerView
     */
    private fun findScrollingView(viewGroup: ViewGroup): View? {
        if (viewGroup is ScrollingView || viewGroup is ScrollView) {
            return viewGroup
        }

        (0 until viewGroup.childCount).forEach {
            val view = viewGroup.getChildAt(it)
            if (view is ScrollingView || view is ScrollView) {
                return view
            }
        }

        return null
    }

    private fun park(parent: CoordinatorLayout, child: ViewGroup) {
        val childTop = child.top
        val high = topHighPosition(parent)
        val low = topLowPosition(parent)

        if (childTop == high || childTop == low) {
            mLayoutTop = childTop
            return
        }

        if (inHighPart(parent, child)) {
            mLayoutTop = high
            animator = ValueAnimator.ofInt(childTop, high)
            animator?.addUpdateListener {
                val value = it.animatedValue as Int
                val curTop = child.top
                offsetTopAndBottom(parent, child, value - curTop)
            }
            animator?.start()
        } else {
            mLayoutTop = low
            animator = ValueAnimator.ofInt(childTop, low)
            animator?.addUpdateListener {
                val value = it.animatedValue as Int
                val curTop = child.top
                offsetTopAndBottom(parent, child, value - curTop)
            }
            animator?.start()
        }
    }

    private fun offsetTopAndBottom(parent: CoordinatorLayout, child: View, deltaY: Int) {
        val topSpace = topSpace(parent)
        val childTop = child.top
        val topLow = topLowPosition(parent)
        val percent = (topLow - childTop) / topSpace.toFloat()
        parent.setBackgroundColor(Color.argb((128 * percent).toInt(), 0, 0, 0))
        ViewCompat.offsetTopAndBottom(child, deltaY)
    }

    private fun inHighPart(parent: CoordinatorLayout, child: ViewGroup): Boolean {
        val childTop = child.top
        val topSpace = topSpace(parent)
        val high = topHighPosition(parent)
        val low = topLowPosition(parent)

        if (childTop == high || childTop == low) {
            mLayoutTop = childTop
            return false
        }

        return childTop - high < topSpace / 2
    }

    private fun isHighest(parent: CoordinatorLayout, child: ViewGroup): Boolean {
        return child.top == topHighPosition(parent)
    }

    private fun isLowest(parent: CoordinatorLayout, child: ViewGroup): Boolean {
        return child.top == topLowPosition(parent)
    }

    private fun topSpace(parent: CoordinatorLayout): Int {
        return topLowPosition(parent) - topHighPosition(parent)
    }

    private fun topHighPosition(coordinatorLayout: CoordinatorLayout): Int {
        if (offsetDimension > 0) {
            return offsetDimension
        }
        return (coordinatorLayout.measuredHeight * offsetPercent).toInt()
    }

    private fun topLowPosition(coordinatorLayout: CoordinatorLayout): Int {
        if (expandDimension > 0) {
            return offsetDimension + expandDimension
        }
        return (coordinatorLayout.measuredHeight * offsetPercent * 2).toInt()
    }
}