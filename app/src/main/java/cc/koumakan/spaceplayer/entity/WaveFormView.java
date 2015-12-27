package cc.koumakan.spaceplayer.entity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by lhq on 2015/12/27.
 */
public class WaveFormView extends View {

    private Paint wavePaint;//绘制波形画笔
    private Paint fftPaint; //绘制频谱画笔

    private int type = 1;//绘制类型：0 - 波形图， 1 - 频谱图
    private int mSpectrumNum = 48;//频谱图中柱状图个数

    private int width, height;//控件的宽和高
    private static final float scale = 0.7f;//波形高度占视图的比例
    private int dy;//Y轴偏移量

    private byte[] mBytes = new byte[1024];//数据

    public WaveFormView(Context context) {
        super(context);
        init();
    }

    public WaveFormView(Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    public WaveFormView(Context context, AttributeSet attr, int defStyleAttr) {
        super(context, attr, defStyleAttr);
        init();
    }

    private void init() {

        setFocusable(true);

        wavePaint = new Paint();
        wavePaint.setColor(Color.WHITE);
        wavePaint.setStrokeWidth(2f);
        wavePaint.setAntiAlias(true);

        fftPaint = new Paint();
        fftPaint.setStrokeWidth(8f);
        fftPaint.setAntiAlias(true);
        fftPaint.setColor(Color.rgb(0, 128, 255));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes != null) {

            if (type == 0) {

                /**
                 * 绘制波形图
                 */
//                float dx = width * 1.0f / (mBytes.length - 1);

//                float midHeight = height * 1.0f / 2;

                float[] pts = new float[(mBytes.length - 1) * 4];

                for (int i = 0; i < mBytes.length - 1; i++) {

                    pts[i * 4] = width * i / (mBytes.length - 1);
                    pts[i * 4 + 1] = height / 2
                            + ((byte) (mBytes[i] + 128)) * (height / 2) / 128;
                    pts[i * 4 + 2] = width * (i + 1) / (mBytes.length - 1);
                    pts[i * 4 + 3] = height / 2
                            + ((byte) (mBytes[i + 1] + 128)) * (height / 2) / 128;
                }

                canvas.drawLines(pts, wavePaint);

//                System.out.println("刷新...");

                mBytes = null;

            } else {
                /**
                 * 绘制柱状频谱图
                 */
                float[] pts = new float[mBytes.length * 4];

                float baseX = width / mSpectrumNum;

                for (int i = 0; i < mSpectrumNum; i++) {

                    if (mBytes[i] < 0) {
                        mBytes[i] = 127;
                    }

                    final int xi = (int) (baseX * i + baseX / 2);

                    pts[i * 4] = xi;
                    pts[i * 4 + 1] = (int) (0.4 * height);
                    pts[i * 4 + 2] = xi;
                    pts[i * 4 + 3] = (int) (0.4 * height) - mBytes[i] * 3;
                }

                canvas.drawLines(pts, fftPaint);

            }

        } else {
            System.out.println("更新失败：waveform为空");
        }

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        System.out.println("宽度：" + w + ", 高度：" + h);
        width = w;
        height = h;
        dy = (int) ((1.0 - scale) * h) / 2;
    }

    public void setWaveForm(byte[] bytes, int type) {
        this.type = type;

        if (type == 0) {

//            if(mBytes == null) {

                mBytes = new byte[bytes.length];

                for (int i = 0; i < bytes.length; i++) {
                    mBytes[i] = bytes[i];
                }

                invalidate();
//            }
        } else {

            mBytes = new byte[bytes.length / 2 + 1];

            mBytes[0] = (byte) Math.abs(bytes[0]);

            mSpectrumNum = bytes.length / 2;

            for (int i = 2, j = 1; j < mSpectrumNum; ) {

                mBytes[j] = (byte) Math.hypot(bytes[i], bytes[i + 1]);

                if (mBytes[j] == 0) mBytes[j] = 2;

                i += 2;
                j++;
            }

            invalidate();
        }
    }

}
