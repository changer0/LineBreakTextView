package com.lulu.firstchaptertextview

import android.content.Context
import android.graphics.Canvas
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View

/**
 * @author zhanglulu on 2019/12/9.
 * for 断行 TextView 排版 绘制 <br/>
 *  使用方式： <br/>
 *  1. 设置文本 text
 *  2. 排版 lineBreak
 */

class LineBreakTextView : View {
    /**
     * 段首缩进 字符数
     */
    public var paragraphIndentSize = 2
    /**
     * 段间距 倍数
     */
    public var paragraphSpacingMultiplier = 2.0f
    /**
     * 正文间距 倍数
     */
    public var lineSpacingMultiplier = 1.0f
    /**
     * 最大高度
     */
    public var maxHeight = Int.MAX_VALUE
        set(value) {
            field = value
            //重新测量
            requestLayout()
        }
    /**
     * 文字大小
     */
    public var textSize = 40f
        set(value) {
            field = value
            paint.textSize = value
        }

    /**
     * 文字颜色
     */
    public var textColor = 0x000000
        set(value) {
            field = value
            paint.color = value
        }
    /**
     * 文字透明度
     */
    public var textAlpha = 255
        set(value) {
            field = value
            paint.alpha = value
        }

    /**
     * 文字位置
     */
    private val textPositions = ArrayList<TextPosition>()

    /**
     * 布局高度
     */
    private var layoutHeight  = 0f

    /**
     * 文本内容
     */
    var text = ""
        set(value) {
            field = value
            textCharArray = value.toCharArray()
        }

    private var textCharArray: CharArray?= null
    private var paint: TextPaint = TextPaint()

    constructor(ctx: Context) : super(ctx) {init(ctx)}
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx,attrs) {init(ctx)}
    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr : Int) : super(ctx,attrs, defStyleAttr) {init(ctx)}

    private fun init(ctx: Context) {
        paint.color = 0x000000
        paint.alpha = 225
        paint.textSize = 40f
        paint.isAntiAlias = true

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)

        if (layoutHeight > 0 ) {
            height = layoutHeight.toInt()
        }
        if (height > maxHeight) {
            height = maxHeight
        }
        setMeasuredDimension(width, height)

    }


    /**
     * 绘制
     */
    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        for (i in 0 until textPositions.size) {
            val textPosition = textPositions[i]
            canvas?.drawText(textPosition.text, textPosition.x, textPosition.y, paint)
        }
    }


    /**
     * 排版
     */
    public fun lineBreak(maxWidth: Int) {
        val availableWidth = maxWidth - paddingRight

        textPositions.clear()
        //X 的初始化位置
        val initX = paddingLeft.toFloat()
        var curX = initX
        var curY = 0f

        var isNeedCheckParagraphHeadEmptyChar = false//是否检查段首空白字符

        val fontMetrics = paint.fontMetrics
        val lineHeight = (fontMetrics.bottom - fontMetrics.top) * lineSpacingMultiplier
        curY = lineHeight

        val size = textCharArray?.size
        size?.let {
            var i = 0
            while (i < size) {
                val textPosition = TextPosition()

                val c = textCharArray?.get(i)

                if (isNeedCheckParagraphHeadEmptyChar) {
                    //空白字符判断
                    if (c == ' ' || c == '\u0020' || c == '\u3000') {
                        i++
                        continue
                    }
                }

                //当前文字宽度
                val cW = paint.measureText(c.toString())
                //位置保存点
                textPosition.x = curX
                textPosition.y = curY
                textPosition.text = c.toString()
                //curX 向右移动一个字
                curX += cW

                isNeedCheckParagraphHeadEmptyChar = false
                if (isParagraph(textCharArray, i)) {
                    //如果是段落,再移动一位
                    i++
                    curX = initX + paint.measureText("中") * paragraphIndentSize//段首缩进
                    curY += (lineHeight * paragraphSpacingMultiplier)
                    isNeedCheckParagraphHeadEmptyChar = true
                } else if (isNeedNewLine(textCharArray, i, curX, availableWidth)) {
                    //断行需要回溯
                    curX = initX
                    curY += lineHeight
                }
                textPositions.add(textPosition)
                //移动下一个游标
                i++
            }
            layoutHeight = curY
        }
    }


    /**
     * 是否需要另起一行
     */
    private fun isNeedNewLine(
        charArray: CharArray?,
        curIndex: Int,
        curX: Float,
        maxWith: Int
    ) : Boolean{
        charArray?.let {
            if (charArray.size <= curIndex+1) {//需要判断下一个 char
                return false
            }
            //判断下一个 char 是否到达边界
            if (curX + paint.measureText(charArray[curIndex+1].toString()) > maxWith) {
                return true
            }
        }
        if (curX > maxWith) {
            return true
        }
        return false
    }

    /**
     * 是否是段落
     */
    private fun isParagraph(charArray: CharArray?, curIndex: Int): Boolean {
        charArray?.let {
            if (charArray.size <= curIndex+1) {//需要判断下一个 char
                return false
            }
            if (charArray[curIndex] == '\r' && charArray[curIndex+1] == '\n') {
                return true
            }
        }
        return false
    }

    /**
     * 当前文字位置
     */
    class TextPosition {
        val NORMAL = 0x0
        val PARAGRAPH_START = 0x1
        val PARAGRAPH_END = 0x2

        var text = ""
        var x = 0f
        var y = 0f
        var type = NORMAL
    }
}
