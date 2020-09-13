package com.appx.compat.toolbar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPresenter
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.widget.TextViewCompat
import kotlin.math.max

/**
 * AppToolbar 扩展自Toolbar
 */
class AppToolbar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.toolbarStyle
) : Toolbar(context, attrs, defStyleAttr) {

    private var mPopupContext: Context = getContext()
    private lateinit var mMenuView: ActionMenuView

    /**
     * 导航图标, close/back
     */
    private lateinit var mNavigationButton: AppCompatImageButton

    /**
     * Title view
     */
    private lateinit var mTitleView: TextView
    private var mBottomDivider: Drawable? = null

    private var mTitleTextColor: ColorStateList? = null
    private var mTitleTextAppearance: Int = 0
    private var mNavigationIcon: Drawable? = null
    private var mTitleCenter: Boolean = true
    private var mShowBottomDivider: Boolean = true
    private var mPopupTheme = 0
    private var mOuterActionMenuPresenter: ActionMenuPresenter? = null
    private var mMenuBuilderCallback: MenuBuilder.Callback? = null
    private var mActionMenuPresenterCallback: MenuPresenter.Callback? = null
    private var mOnMenuItemClickListener: OnMenuItemClickListener? = null
    private val mMenuViewItemClickListener = ActionMenuView.OnMenuItemClickListener { item ->
        mOnMenuItemClickListener?.onMenuItemClick(item) ?: false
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AppToolbar, defStyleAttr, 0)
        val titleTextAppearance =
                ta.getResourceId(R.styleable.AppToolbar_titleTextAppearance, 0)
        val titleTextColor = ta.getColorStateList(R.styleable.AppToolbar_titleTextColor)
        val navigationIcon = ta.getDrawable(R.styleable.AppToolbar_navigationIcon)
        val bottomDivider = ta.getDrawable(R.styleable.AppToolbar_bottomDivider)
        val showBottomDivider = ta.getBoolean(R.styleable.AppToolbar_showBottomDivider, true)
        val title = ta.getString(R.styleable.AppToolbar_title) ?: ""
        mTitleCenter = ta.getBoolean(R.styleable.AppToolbar_titleCenter, true)
        ta.recycle()
        setTitle(title)
        setNavigationIcon(navigationIcon)
        setBottomDivider(bottomDivider)
        setShowBottomDivider(showBottomDivider)
        setTitleTextAppearance(context, titleTextAppearance)
        if (titleTextColor != null) {
            setTitleTextColor(titleTextColor)
        }
        setWillNotDraw(false)
    }

    @SuppressLint("RestrictedApi")
    fun setMenu(menu: MenuBuilder?, outerPresenter: ActionMenuPresenter) {
        if (menu == null && (!::mMenuView.isInitialized)) {
            return
        }

        ensureMenuView()
        val oldMenu = mMenuView.peekMenu()
        if (oldMenu === menu) {
            return
        }

        outerPresenter.setExpandedActionViewsExclusive(false)
        if (menu != null) {
            menu.addMenuPresenter(outerPresenter, mPopupContext)
        } else {
            outerPresenter.initForMenu(mPopupContext, null)
            outerPresenter.updateMenuView(true)
        }
        mMenuView.popupTheme = mPopupTheme
        mMenuView.setPresenter(outerPresenter)
        mOuterActionMenuPresenter = outerPresenter
    }

    override fun getMenu(): Menu {
        ensureMenu()
        return mMenuView.menu
    }

    private fun ensureMenu() {
        ensureMenuView()
    }

    private fun ensureMenuView() {
        if (!::mMenuView.isInitialized) {
            mMenuView = ActionMenuView(context)
            mMenuView.popupTheme = mPopupTheme
            mMenuView.setOnMenuItemClickListener(mMenuViewItemClickListener)
            mMenuView.setMenuCallbacks(mActionMenuPresenterCallback, mMenuBuilderCallback)
            val lp: LayoutParams = generateDefaultLayoutParams()
            lp.gravity = GravityCompat.END or Gravity.CENTER_VERTICAL
            mMenuView.layoutParams = lp
            addView(mMenuView)
        }
    }

    override fun setOnMenuItemClickListener(listener: OnMenuItemClickListener?) {
        mOnMenuItemClickListener = listener
    }

    override fun setMenuCallbacks(pcb: MenuPresenter.Callback?, mcb: MenuBuilder.Callback?) {
        mActionMenuPresenterCallback = pcb
        mMenuBuilderCallback = mcb
        if (::mMenuView.isInitialized) {
            mMenuView.setMenuCallbacks(pcb, mcb)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var useWidth = paddingLeft+paddingRight
        var height = 0
        var childState = 0
        var leftUseWidth = paddingLeft
        var rightUseWidth = paddingRight

        if (::mNavigationButton.isInitialized && shouldLayout(mNavigationButton)) {
            measureChildMargins(
                    mNavigationButton,
                    widthMeasureSpec,
                    useWidth,
                    heightMeasureSpec
            )
            val navWidth = mNavigationButton.measuredWidth + getHorizontalMargins(mNavigationButton)
            height = mNavigationButton.height + getVerticalMargins(mNavigationButton)
            useWidth += navWidth
            leftUseWidth += navWidth
            childState = View.combineMeasuredStates(childState,
                    mNavigationButton.measuredState)
        }
        var menuWidth = 0
        if (::mMenuView.isInitialized && shouldLayout(mMenuView)) {
            measureChildMargins(mMenuView, widthMeasureSpec, useWidth, heightMeasureSpec)
            menuWidth = mMenuView.measuredWidth + getHorizontalMargins(mMenuView)
            height = max(height, mMenuView.measuredHeight +
                    getVerticalMargins(mMenuView))
            rightUseWidth += menuWidth
            useWidth += menuWidth
            childState = View.combineMeasuredStates(childState,
                    mMenuView.measuredState)
        }
        if (::mTitleView.isInitialized && shouldLayout(mTitleView)) {
            val titleWidth =
                    if (mTitleCenter) {
                        max(leftUseWidth, rightUseWidth) * 2
                    } else {
                        leftUseWidth + rightUseWidth
                    }
            measureChildMargins(
                    mTitleView,
                    widthMeasureSpec,
                    titleWidth,
                    heightMeasureSpec
            )
            height = max(height, mTitleView.measuredHeight +
                    getVerticalMargins(mTitleView))
            childState = View.combineMeasuredStates(childState,
                    mTitleView.measuredState)
        }
        height += paddingTop + paddingBottom
        val measuredWidth = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val measuredHeight = View.resolveSizeAndState(max(suggestedMinimumHeight, height) + getBottomDividerHeight(),
                heightMeasureSpec, childState shl View.MEASURED_HEIGHT_STATE_SHIFT)

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun getVerticalMargins(v: View): Int {
        val mlp = v.layoutParams as MarginLayoutParams
        return mlp.topMargin + mlp.bottomMargin
    }

    private fun getHorizontalMargins(v: View): Int {
        val mlp = v.layoutParams as MarginLayoutParams
        return MarginLayoutParamsCompat.getMarginStart(mlp) +
                MarginLayoutParamsCompat.getMarginEnd(mlp)
    }

    private fun measureChildMargins(
            child: View, parentWidthMeasureSpec: Int, widthUsed: Int,
            parentHeightMeasureSpec: Int
    ): Int {
        val lp = child.layoutParams as MarginLayoutParams
        val hMargin = lp.leftMargin + lp.rightMargin
        val childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(
                parentWidthMeasureSpec,
                paddingLeft + paddingRight + hMargin + widthUsed, lp.width
        )
        val childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(
                parentHeightMeasureSpec,
                paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin, lp.height
        )
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        return child.measuredWidth + hMargin
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var left = paddingLeft
        var right = width - paddingRight
        val parentHeight = height - getBottomDividerHeight()
        if (::mNavigationButton.isInitialized && shouldLayout(mNavigationButton)) {
            left = layoutChildLeft(mNavigationButton, left)
        }
        if (::mMenuView.isInitialized && shouldLayout(mMenuView)) {
            right = layoutChildRight(mMenuView, right)
        }
        if (::mTitleView.isInitialized && shouldLayout(mTitleView)) {
            layoutTitle(parentHeight, left)
        }
    }

    private fun shouldLayout(view: View?): Boolean = view != null && view.visibility == View.VISIBLE

    private fun layoutTitle(parentHeight: Int, left: Int) {
        val titleTop = (parentHeight - mTitleView.measuredHeight) / 2
        val lp = mTitleView.layoutParams as MarginLayoutParams
        val childWidth = mTitleView.measuredWidth
        val titleLeft = if (mTitleCenter) (width - childWidth) / 2 else left + lp.leftMargin
        mTitleView.layout(
                titleLeft, titleTop, titleLeft + childWidth,
                titleTop + mTitleView.measuredHeight
        )
    }

    private fun layoutChildLeft(child: View, left: Int): Int {
        var l = left
        val lp = child.layoutParams as MarginLayoutParams
        l += lp.leftMargin
        val top = getChildTop(child)
        val childWidth = child.measuredWidth
        child.layout(l, top, l + childWidth, top + child.measuredHeight)
        l += childWidth + lp.rightMargin
        return l
    }

    private fun layoutChildRight(child: View, right: Int): Int {
        var r = right
        val lp = child.layoutParams as LayoutParams
        r -= lp.rightMargin
        val top = getChildTop(child)
        val childWidth = child.measuredWidth
        child.layout(r - childWidth, top, r, top + child.measuredHeight)
        r -= childWidth + lp.leftMargin
        return r
    }

    private fun getChildTop(child: View): Int {
        val lp = child.layoutParams as Toolbar.LayoutParams
        val childHeight = child.measuredHeight
        return when (getChildVerticalGravity(lp.gravity)) {
            Gravity.TOP -> paddingTop
            Gravity.BOTTOM -> {
                height - paddingBottom - childHeight - lp.bottomMargin - getBottomDividerHeight()
            }
            Gravity.CENTER_VERTICAL -> {
                val paddingTop = paddingTop
                val paddingBottom = paddingBottom
                val height = height - getBottomDividerHeight()
                val space = height - paddingTop - paddingBottom
                var spaceAbove = (space - childHeight) / 2
                if (spaceAbove < lp.topMargin) {
                    spaceAbove = lp.topMargin
                } else {
                    val spaceBelow = height - paddingBottom - childHeight -
                            spaceAbove - paddingTop
                    if (spaceBelow < lp.bottomMargin) {
                        spaceAbove =
                                Math.max(0, spaceAbove - (lp.bottomMargin - spaceBelow))
                    }
                }
                paddingTop + spaceAbove
            }
            else -> {
                val paddingTop = paddingTop
                val paddingBottom = paddingBottom
                val height = height
                val space = height - paddingTop - paddingBottom
                var spaceAbove = (space - childHeight) / 2
                if (spaceAbove < lp.topMargin) {
                    spaceAbove = lp.topMargin
                } else {
                    val spaceBelow = height - paddingBottom - childHeight -
                            spaceAbove - paddingTop
                    if (spaceBelow < lp.bottomMargin) {
                        spaceAbove =
                                Math.max(0, spaceAbove - (lp.bottomMargin - spaceBelow))
                    }
                }
                paddingTop + spaceAbove
            }
        }
    }

    private fun getChildVerticalGravity(gravity: Int): Int {
        val vgrav = gravity and Gravity.VERTICAL_GRAVITY_MASK
        return when (vgrav) {
            Gravity.TOP, Gravity.BOTTOM, Gravity.CENTER_VERTICAL -> vgrav
            else -> Gravity.VERTICAL_GRAVITY_MASK
        }
    }

    private fun getBottomDividerHeight(): Int {
        return if (mShowBottomDivider) {
            mBottomDivider?.intrinsicHeight ?: 0
        } else {
            0
        }
    }

    fun setShowBottomDivider(show: Boolean) {
        if (mShowBottomDivider != show) {
            mShowBottomDivider = show
            requestLayout()
        }
    }

    fun setBottomDivider(resId: Int) {
        setBottomDivider(ContextCompat.getDrawable(context, resId))
    }

    fun setBottomDivider(divider: Drawable?) {
        if (mBottomDivider != divider) {
            mBottomDivider = divider
            requestLayout()
        }
    }

    fun setTitleCenter(titleCenter: Boolean) {
        if (mTitleCenter != titleCenter) {
            mTitleCenter = titleCenter
            requestLayout()
        }
    }

    override fun setTitleTextAppearance(context: Context?, resId: Int) {
        ensureTitleView()
        mTitleTextAppearance = resId
        TextViewCompat.setTextAppearance(mTitleView, resId)
    }

    override fun setTitleTextColor(color: ColorStateList) {
        ensureTitleView()
        mTitleTextColor = color
        mTitleView.setTextColor(mTitleTextColor)
    }

    override fun setTitle(title: CharSequence?) {
        if (!TextUtils.isEmpty(title)) {
            ensureTitleView()
            if (mTitleView.parent == null) {
                addView(mTitleView)
            }
            mTitleView.visibility = View.VISIBLE
            if (mTitleTextColor != null) {
                mTitleView.setTextColor(mTitleTextColor)
            }
            TextViewCompat.setTextAppearance(mTitleView, mTitleTextAppearance)
            mTitleView.text = title
        } else if (::mTitleView.isInitialized) {
            mTitleView.visibility = View.GONE
        }
    }

    private fun ensureTitleView() {
        if (!::mTitleView.isInitialized) {
            mTitleView = AppCompatTextView(context)
            mTitleView.setSingleLine()
            mTitleView.ellipsize = TextUtils.TruncateAt.END
            val lp = generateDefaultLayoutParams()
            mTitleView.layoutParams = lp
        }
    }

    override fun setNavigationOnClickListener(listener: OnClickListener?) {
        ensureNavButton()
        mNavigationButton.setOnClickListener(listener)
    }

    override fun setNavigationIcon(icon: Drawable?) {
        if (icon != null) {
            ensureNavButton()
            if (mNavigationButton.parent == null) {
                addView(mNavigationButton)
            }
            mNavigationButton.visibility = View.VISIBLE
            mNavigationButton.setImageDrawable(icon)
        } else if (::mNavigationButton.isInitialized) {
            mNavigationButton.visibility = View.GONE
        }
        mNavigationIcon = icon
    }

    private fun ensureNavButton() {
        if (!::mNavigationButton.isInitialized) {
            mNavigationButton = AppCompatImageButton(
                    context,
                    null,
                    R.attr.navigationButtonStyle
            )
            val lp = generateDefaultLayoutParams()
            lp.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            mNavigationButton.layoutParams = lp
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        drawBottomDivider(canvas)
    }

    /**
     * Draw bottom divider
     */
    private fun drawBottomDivider(canvas: Canvas) {
        if (mShowBottomDivider) {
            mBottomDivider?.let {
                val top = height - it.intrinsicHeight
                it.setBounds(0, top, width, top + it.intrinsicHeight)
                it.draw(canvas)
            }
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): LayoutParams {
        return when (p) {
            is LayoutParams -> LayoutParams(p)
            is MarginLayoutParams -> LayoutParams(p)
            else -> LayoutParams(p)
        }
    }

    class LayoutParams : Toolbar.LayoutParams {

        constructor(source: ViewGroup.LayoutParams?) : super(source)
        constructor(source: MarginLayoutParams?) : super(source)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs)
    }
}