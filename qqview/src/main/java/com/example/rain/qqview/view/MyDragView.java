package com.example.rain.qqview.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.example.rain.qqview.Utils.ColorUtil;
import com.nineoldandroids.animation.FloatEvaluator;
import com.nineoldandroids.animation.IntEvaluator;
import com.nineoldandroids.view.ViewHelper;

/**
 * Created by rain on 2015/11/17.
 */
public class MyDragView extends FrameLayout {

    private View mMenuView;
    private View mMainView;
    private ViewDragHelper mDragHelper;
    private FloatEvaluator mFloatEvaluator;
    private IntEvaluator mIntEvaluator;
    private int mWidth;
    private float mDragRange;

    public MyDragView(Context context) {
        super(context);
        initView();
    }

    public MyDragView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MyDragView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {

        mDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return child == mMenuView || child == mMainView;
//                返回true表示允许拖动，控制哪个子控件可以拖动
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {

                if (child == mMainView) {
                    if (left < 0) {
                        left = 0;
                    } else if (left > (getMeasuredWidth() - child.getMeasuredWidth())) {
                        // 限制右边界
                        left = getMeasuredWidth() - child.getMeasuredWidth();
                    }
                }
                return left;

            }

            @Override
            public int getViewHorizontalDragRange(View child) {
                return (int) mDragRange;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                if (top < 0) {
                    top = 0;
                } else if (top > (getMeasuredHeight() - child.getMeasuredHeight())) {
                    top = getMeasuredHeight() - child.getMeasuredHeight();
                }
                return top;
            }

            /**
             * 控制child在垂直方向的移动 top:
             * 表示ViewDragHelper认为你想让当前child的top改变的值,top=chile.getTop()+dy dy:
             * 本次child垂直方向移动的距离 return: 表示你真正想让child的top变成的值
             */
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                if(changedView==mMenuView){
                    //固定住menuView
                    mMenuView.layout(0, 0, mMenuView.getMeasuredWidth(),mMenuView.getMeasuredHeight());
                    //让mainView移动起来
                    int newLeft = mMainView.getLeft()+dx;
                    if(newLeft<0)newLeft=0;//限制mainView的左边
                    if(newLeft>mDragRange)newLeft=(int) mDragRange;//限制mainView的右边
                    mMainView.layout(newLeft,mMainView.getTop()+dy,newLeft+mMainView.getMeasuredWidth(),mMainView.getBottom()+dy);
                }

                //1.计算滑动的百分比
                float fraction = mMainView.getLeft()/mDragRange;
                //2.执行伴随动画
                executeAnim(fraction);
                //3.更改状态，回调listener的方法
                if(fraction==0 && mCurrentState!=DragState.Close){
                    //更改状态为关闭，并回调关闭的方法
                    mCurrentState = DragState.Close;
                    if(listener!=null)listener.onClose();
                }else if (fraction==1f && mCurrentState!=DragState.Open) {
                    //更改状态为打开，并回调打开的方法
                    mCurrentState = DragState.Open;
                    if(listener!=null)listener.onOpen();
                }
                //将drag的fraction暴漏给外界
                if(listener!=null){
                    listener.onDraging(fraction);
                }
            }
            /**
             * 当child的位置改变的时候执行,一般用来做其他子View的伴随移动 changedView：位置改变的child
             * left：child当前最新的left top: child当前最新的top dx: 本次水平移动的距离 dy: 本次垂直移动的距离
             */
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                if(mMainView.getLeft()<mDragRange/2){
                    //在左半边
                    close();
                }else {
                    //在右半边
                    open();
                }

                //处理用户的稍微滑动
                if(xvel>200 && mCurrentState!=DragState.Open){
                    open();
                }else if (xvel<-200 && mCurrentState!=DragState.Close) {
                    close();
                }
            }
        });
        mFloatEvaluator = new FloatEvaluator();
        mIntEvaluator = new IntEvaluator();

    }


    @Override
    protected void onFinishInflate() {

        super.onFinishInflate();
        mMenuView = getChildAt(0);
        mMainView = getChildAt(1);

    }

    enum DragState {
        Open, Close;
    }

    /**
     * 获取当前的状态
     *
     * @return
     */
    public DragState getCurrentState() {
        return mCurrentState;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = getMeasuredWidth();
        mDragRange = mWidth * 0.6f;
    }

    private DragState mCurrentState = DragState.Close;//当前SlideMenu的状态默认是关闭的

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        return true;
    }

    /*@Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        mMenuView.layout(left, top, left + mMenuView.getMeasuredWidth(), top + mMenuView.getMeasuredHeight());
        mMainView.layout(left, mMenuView.getBottom(), left + mMainView.getMeasuredWidth(), top + mMenuView.getBottom() + mMainView.getMeasuredHeight());

    }*/

    public void close() {
        mDragHelper.smoothSlideViewTo(mMainView, 0, mMainView.getTop());
        ViewCompat.postInvalidateOnAnimation(MyDragView.this);
    }

    /**
     * 打开菜单
     */
    public void open() {
        mDragHelper.smoothSlideViewTo(mMainView, (int) mDragRange, mMainView.getTop());
        ViewCompat.postInvalidateOnAnimation(MyDragView.this);
    }


    /**
     * 执行伴随动画
     *
     * @param fraction
     */
    private void executeAnim(float fraction) {
        //fraction:0-1
        //缩小mainView
//		float scaleValue = 0.8f+0.2f*(1-fraction);//1-0.8f
        ViewHelper.setScaleX(mMainView, mFloatEvaluator.evaluate(fraction, 1f, 0.8f));
        ViewHelper.setScaleY(mMainView, mFloatEvaluator.evaluate(fraction, 1f, 0.8f));
        //移动menuView
        ViewHelper.setTranslationX(mMenuView, mIntEvaluator.evaluate(fraction, -mMenuView.getMeasuredWidth() / 2, 0));
        //放大menuView
        ViewHelper.setScaleX(mMenuView, mFloatEvaluator.evaluate(fraction, 0.5f, 1f));
        ViewHelper.setScaleY(mMenuView, mFloatEvaluator.evaluate(fraction, 0.5f, 1f));
        //改变menuView的透明度
        ViewHelper.setAlpha(mMenuView, mFloatEvaluator.evaluate(fraction, 0.3f, 1f));

        //给SlideMenu的背景添加黑色的遮罩效果
        getBackground().setColorFilter((Integer) ColorUtil.evaluateColor(fraction, Color.BLACK, Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
    }

    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(MyDragView.this);
        }
    }

    ;

    private OnDragStateChangeListener listener;

    public void setOnDragStateChangeListener(OnDragStateChangeListener listener) {
        this.listener = listener;
    }

    public interface OnDragStateChangeListener {
        /**
         * 打开的回调
         */
        void onOpen();

        /**
         * 关闭的回调
         */
        void onClose();

        /**
         * 正在拖拽中的回调
         */
        void onDraging(float fraction);
    }


}
