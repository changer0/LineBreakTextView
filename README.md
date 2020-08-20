
### 开始前

> 前几天做了一个需求（首章漏出），要求对一段文字可以进行分段且可以设置它的段间距，行间距等属性，大致需要以下功能点

![LineBreakTextView 功能点](https://gitee.com/luluzhang/ImageCDN/raw/master/blog/20200120100936.png)

#### 实现思路

基本的实现思路就是将每个文字进行排版布局，计算出当前文字的位置，绘制在 View 上。

![](https://gitee.com/luluzhang/ImageCDN/raw/master/blog/20200120144421.png)

#### 准备知识点

根据上述的实现思路我们需要准备下面的知识点：

**canvas.drawText(x,y) 的位置问题：**


首先 x 值，有两种：

- 当你的 Paint 设置为myPaint.setTextAlign(Paint.Align.LEFT)，x 就是文字最左侧到当前 view 左边距的距离
- 当你的 Paint 设置为myPaint.setTextAlign(Paint.Align.CENTER)，x 就是文字中央到当前 view 左边距的距离。

x 值比较容易确认，默认是为 Paint.Align.LEFT，但 y 的值并不是 text 的顶部，而是以 baseline 为基准的，y 是基线到当前 view 顶部的距离（请看下图）。

![文字在 View 上绘制的位置](https://gitee.com/luluzhang/ImageCDN/raw/master/blog/20200120122126.png)

可以用下面代码获取到图中对应的 top，ascent，descent，bottom


```
FontMetrics fontMetrics = mPaint.getFontMetrics();
fontMetrics.top;
fontMetrics.ascent;
fontMetrics.descent;
fontMetrics.bottom;
```
所有的四个值都是以基线baseLine为基准来计算的。==baseline 以上的就是负的；以下的是正的。==

根据上面的值我们就可以指定 y 的位置：

中间位置


```
float baselineY = centerY
    + (fontMetrics.bottom-fontMetrics.top)/2 - fontMetrics.bottom
```

顶部位置


```
float baselineY = Y - fontMetrics.top;
```

![文字在 View 上绘制的 y 的 位置](https://gitee.com/luluzhang/ImageCDN/raw/master/blog/20200120114719.png)

### LineBreadkTextView 的实现

> 具备上面的内容，我们就可以是实现支持段落的 TextView 了。

首先写一个 LineBreakTextView 继承自 View，重写必要方法


```
class LineBreakTextView : View {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //...
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        //...
    }
}
```

#### 排版

为了能够确认每个文字的位置我们添加一个用来描述当前文字位置的实体内部类，并声明一个全局的List：


```

/**
 * 文字位置
 */
private val textPositions = ArrayList<TextPosition>()

class TextPosition {
    companion object {
        const val NORMAL = 0x0
        const val TITLE = 0x1
    }

    var text = ""
    var x = 0f
    var y = 0f
    var type = NORMAL
}
```
> 其中 type 用来表示是标题还是正文

在 LineBreakTextView 中添加 lineBreak(maxWidth: Int) 方法，用来排版，并声明两个局部变量用 curX curY 用来表示当前文字的位置

```
public fun lineBreak(maxWidth: Int) {
        val availableWidth = maxWidth - paddingRight
        textLineYs.clear()
        textPositions.clear()
        //X 的初始化位置
        val initX = paddingLeft.toFloat()
        var curX = initX
        var curY = paddingTop.toFloat()
        //...
}
```

取出标题和正文的 fontMetrics，方便后面使用，并计算出正文的 1 倍行高

```
public fun lineBreak(maxWidth: Int) {
        //...
        val titleFontMetrics = titlePaint.fontMetrics
        val textFontMetrics = textPaint.fontMetrics
        val lineHeight = textFontMetrics.bottom - textFontMetrics.top
        //...
}
```

textType 用来确认当前文字类型，结合之前描述通过指定文字顶部位置的方式来确认文字的 y 位置
```
public fun lineBreak(maxWidth: Int) {
        //...
        //首行是否为标题
        var textType = if (isNeedTitle) {
            curY -= titleFontMetrics.top//指定顶点坐标
            TextPosition.TITLE
        } else {
            curY -= textFontMetrics.top//指定顶点坐标
            TextPosition.NORMAL
        }
        //...
}
```

准备工作做完之后开始计算文字位置，并保存在 textPositions 中
，下面代码用来计算 x 的位置：

```
/**
 * 文本内容
 */
var text = ""
    set(value) {
        field = value
        textCharArray = value.toCharArray()
    }
```

```

public fun lineBreak(maxWidth: Int) {
        //...
        val size = textCharArray?.size
        size?.let {
            var i = 0
            while (i < size) {
                val textPosition = TextPosition()
                val c = textCharArray?.get(i)

                //...

                //当前文字宽度
                val cW = if (textType == TextPosition.TITLE) {
                    titlePaint.measureText(c.toString())
                } else {
                    textPaint.measureText(c.toString())
                }

                //位置保存点
                textPosition.x = curX
                textPosition.y = curY
                textPosition.text = c.toString()
                textPosition.type = textType

                //curX 向右移动一个字
                curX += cW
                //...

                textPositions.add(textPosition)
                //移动下一个游标
                i++

            }
            curY += paddingBottom
            layoutHeight = curY + textFontMetrics.bottom//应加上后面的Bottom
        }
        //...
}
```
仔细观察会发现，上面的代码并没有断行的情况，接下来通过计算 y 的位置来生成断行后的 x 的位置

y 的位置相对复杂，因为还有考虑行高和段高等问题， isParagraph 该方法是来判断是否是段末，如果是段末则 x 位置设置段首缩进，y 根据当前文字类型设置段高，isNeedNewLine 方法用来判断文字是否到达行末，如果到达之后，x 位置回溯到初始位置，y 则设置正文行高

```
//...
//curX 向右移动一个字
curX += cW

if (isParagraph(textCharArray, i)) {
    //如果是段落,再移动一位
    i++
    curX = initX + textPaint.measureText("中") * paragraphIndentSize//段首缩进
    //根据不同的文字类型设置不同的行高
    curY += if (textType == TextPosition.TITLE) {
        (lineHeight * titleSpacingMultiplier)
    } else {
        (lineHeight * paragraphSpacingMultiplier)
    }
    //除了首段，后续段落都为 Normal
    textType = TextPosition.NORMAL

} else if (isNeedNewLine(textCharArray, i, curX, availableWidth)) {
    textLineYs.add(curY)
    //断行需要回溯
    curX = initX
    curY += lineHeight * lineSpacingMultiplier
}
//...
```

下面是两个方法的具体实现

```
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
        if (curX + textPaint.measureText(charArray[curIndex+1].toString())
            > maxWith) {
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
        if (charArray[curIndex] == '\r'
            && charArray[curIndex+1] == '\n') {
            return true
        }
    }
    return false
}

```


#### 测量

针对咱们的 LineBreakTextView 只需关心当前 View 的高度

为了实现可以设置 maxLines 需要记录下每一行的 y 坐标

```
/**
 * 行 Y 坐标
 */
private val textLineYs = ArrayList<Float>()
```
记录 y 坐标

```
public fun lineBreak(maxWidth: Int) {
    textLineYs.clear()
    //...
    if (isParagraph(textCharArray, i)) {
        textLineYs.add(curY)
        //...
    } else if (isNeedNewLine(textCharArray, i, curX, availableWidth)) {
        textLineYs.add(curY)
        //...
    }
    //...
}
```
完成测量

```
override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val width = MeasureSpec.getSize(widthMeasureSpec)
    var height = MeasureSpec.getSize(heightMeasureSpec)

    if (layoutHeight > 0 ) {
        height = layoutHeight.toInt()
    }
    if (getLines() > maxLines && maxLines - 1 > 0) {
        val textBottomH = textPaint.fontMetrics.bottom.toInt()
        height = (textLineYs[maxLines-1]).toInt() + paddingBottom + textBottomH
    }
    if (height > maxHeight) {
        height = maxHeight
    }
    setMeasuredDimension(width, height)
}
```

#### 绘制

绘制相对简单些，在完成了排版之后，位置都已经记录了下来直接画在 canvas 上即可。


```
override fun draw(canvas: Canvas?) {
    super.draw(canvas)
    for (i in 0 until textPositions.size) {
        val textPosition = textPositions[i]
        val paint = if (textPosition.type == TextPosition.TITLE) {
            titlePaint
        } else {
            textPaint
        }
        canvas?.drawText(textPosition.text, textPosition.x, textPosition.y, paint)
    }
}
```

### 效果展示

在布局中添加 LineBreakTextView，在 Activity 中做如下设置：

```
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    textView.text = "第1章 楔子：琴乱\r\n...."
    val dm = this.applicationContext.resources
        .displayMetrics
    textView.lineBreak(dm.widthPixels)
}
```

效果图

![效果图](https://gitee.com/luluzhang/ImageCDN/raw/master/blog/20200120160623.jpg)


### Github 地址

https://github.com/changer0/LineBreakTextView