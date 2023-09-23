package dev.panwar.kidsdrawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context,attrs: AttributeSet) : View(context,attrs) { //inheriting view class//we will draw on this view

    private var mDrawPath: CustomPath?=null
    private var mCanvasBitmap: Bitmap?=null//required of drawing....read description of Path class for more details
    private var mDrawPaint:Paint?=null
    private var mCanvasPaint:Paint?=null
    private var mBrushSize:Float=10.toFloat() //default brush size
    private var color=Color.BLACK//default color
    private var canvas: Canvas?=null //background on which we will paint
    private  val mPaths=ArrayList<CustomPath>()//to store path after something is drawn
    private  val mUndoPaths=ArrayList<CustomPath>()



    init {
        setupDrawing()
    }

    fun onClickUndo(){
        if(mPaths.size>0){
            mUndoPaths.add(mPaths.removeAt(mPaths.size-1))
            //this invalidate will auto call OnDraw function to show changes to the user because onDraw is an overridden function
            invalidate()
        }
    }

    fun onClickRedo(){
        if(mUndoPaths.size>0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            //this invalidate will auto call OnDraw function to show changes to the user because onDraw is an overridden function
            invalidate()
        }
    }

    private fun setupDrawing() {
       mDrawPaint=Paint()
        mDrawPath=CustomPath(color,mBrushSize)
        mDrawPaint!!.color=color//we just set in line 28 the value of mDraw Paint so we know it is not null so we use !!
        mDrawPaint!!.style=Paint.Style.STROKE
        mDrawPaint!!.strokeJoin=Paint.Join.ROUND//stroke
        mDrawPaint!!.strokeCap=Paint.Cap.ROUND//tip of stroke
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
//         mBrushSize=20.toFloat()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {//function of view class
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap= Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
         canvas=Canvas(mCanvasBitmap!!)
    }
//change Canvas to Canvas? if fails
    override fun onDraw(canvas: Canvas) {//inbuilt function of canvas we need to implement
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!,0f,0f,mCanvasPaint)
    
    for (path in mPaths){
        mDrawPaint!!.strokeWidth=path.brushThickness//maybe different strokes added by us in drawing may have different properties
        mDrawPaint!!.color=path.color
        canvas.drawPath(path,mDrawPaint!!)//to draw all paths on screen
    }
       if (!mDrawPath!!.isEmpty){
           mDrawPaint!!.strokeWidth=mDrawPath!!.brushThickness
           mDrawPaint!!.color=mDrawPath!!.color
           canvas.drawPath(mDrawPath!!,mDrawPaint!!)
       }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {//inbuilt function for touch functionality
        val touchX = event?.x
        val touchY = event?.y

        when (event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness=mBrushSize

                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX,touchY)
                    }
                }
            }

            MotionEvent.ACTION_MOVE ->{
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX,touchY)
                    }
                }
            }

            MotionEvent.ACTION_UP ->{

                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color,mBrushSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize:Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)//to read new size from screen Typed value is function for more read documentation

        mDrawPaint!!.strokeWidth=mBrushSize
    }


    internal inner class CustomPath(var color: Int,var brushThickness:Float) : Path(){//inner class//Path is an inbuilt class which help in drawing...for more cmd+click on it to read it's description//custom path class will save our path

    }

    fun setColor(newColor: String){
        color=Color.parseColor(newColor)
        mDrawPaint!!.color=color
    }

}