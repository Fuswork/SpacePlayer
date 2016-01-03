package cc.koumakan.spaceplayer.entity;

import android.content.Context;
import android.widget.TextView;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;

/**
 * Created by lhq on 2015/12/25.
 */
public class LyricView extends TextView {

	private Paint mPaint;//非高亮部分画笔
	private Paint mLightPaint;//高亮部分画笔

	private String[] lrcLines = null;

	private float centerX;
	private float centerY;

	public static final int LINE_COUNT = 10;//每次显示 10 行
	public static final int CURRENT_INDEX = 5;
	public static final int DY = 100; // 每一行的间隔

	public LyricView(Context context) {
		super(context);
		init();
	}

	public LyricView(Context context, AttributeSet attr) {
		super(context, attr);
		init();
	}

	public LyricView(Context context, AttributeSet attr, int defStyleAttr) {
		super(context, attr, defStyleAttr);
		init();
	}

	/**
	 * 使用前必须初始化
	 */
	public void init() {

		setFocusable(true);

		/** 非高亮部分 显示效果设置 */
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setTextSize(40);
		mPaint.setColor(Color.LTGRAY);
		mPaint.setTypeface(Typeface.SANS_SERIF);

		/** 高亮部分 显示效果设置 */
		mLightPaint = new Paint();
		mLightPaint.setAntiAlias(true);
		mLightPaint.setColor(Color.WHITE);
		mLightPaint.setTextSize(40);
		mLightPaint.setTypeface(Typeface.SERIF);
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawColor(Color.argb(0, 0, 0, 0));

		mPaint.setTextAlign(Paint.Align.CENTER);
		mLightPaint.setTextAlign(Paint.Align.CENTER);

		int count = 0;

		if (lrcLines == null || lrcLines.length < LINE_COUNT) {
			System.out.println("字幕绘制出错！");
			return;
		}

		if (lrcLines[CURRENT_INDEX] != null) {
			canvas.drawText(lrcLines[CURRENT_INDEX], centerX, centerY, mLightPaint);
			count++;
		}

		for (int i = CURRENT_INDEX - 1; i >= 0; i--) {
			if (lrcLines[i] != null) {
				canvas.drawText(lrcLines[i], centerX, centerY - (CURRENT_INDEX - i) * DY, mPaint);
				count++;
			}
		}

		for (int i = CURRENT_INDEX + 1; i < LINE_COUNT; i++) {
			if (lrcLines[i] != null) {
				canvas.drawText(lrcLines[i], centerX, centerY + (i - CURRENT_INDEX) * DY, mPaint);
				count++;
			}
		}
		if (count == 0) {
			canvas.drawText("暂无歌词", centerX, centerY, mPaint);
		}

	}

	protected void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);
		centerX = w * 0.5f;
		centerY = h * 0.5f;
	}

	public void setLRCLines(String[] lrcLines) {
		this.lrcLines = lrcLines;
		invalidate();
	}

}