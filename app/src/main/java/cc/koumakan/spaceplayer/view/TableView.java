package cc.koumakan.spaceplayer.view;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.*;

import cc.koumakan.spaceplayer.R;

/**
 * Created by remilia on 15-12-27.
 */
public class TableView extends TableLayout {
	private View rootView;
	private int max_height = 0;
	private Context context;
	public boolean isShow = false;
	private int width;

	public TableView(final Context context) {
		super(context);
		this.context = context;
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		width = wm.getDefaultDisplay().getWidth();
		width = (width - 110) / 3;
	}

	/**
	 * 初始化布局
	 */
	public void init(int[] rc, String[] center, String[] num, int kind) {
		childeView = new View[rc.length];
		for (int i = 0; i <= (rc.length - 1) / 3; i++) {
			LinearLayout tableRow = new LinearLayout(context);
			max_height += 380;
			for (int j = 0; j < 3 && 3 * i + j < rc.length; j++) {
				childeView[3 * i + j] = make(rc[3 * i + j], center[3 * i + j], num[3 * i + j], "" + kind);
				tableRow.addView(childeView[3 * i + j], new LinearLayout.LayoutParams(width, 380));
			}
			addView(tableRow, new LayoutParams(-1, -2));
		}
		rootView = this.getRootView();
	}

	View[] childeView;

	/**
	 * 设置点击相应事件
	 */
	public void setOnclickListener(int i, OnClickListener onclickListener) {
		childeView[i].setOnClickListener(onclickListener);
	}

	/**
	 * 生成项
	 */
	private View make(int rc, String center, String num, String tag) {
		View contentView = LayoutInflater.from(context).inflate(R.layout.view_main_album, null);
		((ImageView) contentView.findViewById(R.id.view_main_list_iv)).setImageResource(rc);
		((TextView) contentView.findViewById(R.id.view_main_list_tv_center)).setText(center);
		((TextView) contentView.findViewById(R.id.view_main_list_tv_bottom)).setText(num);
		((TextView) contentView.findViewById(R.id.view_main_list_tv_tag)).setText(tag);
		return contentView;
	}


	/**
	 * 显示
	 */
	public void show(int i) {
		ScrollView scrollView = (ScrollView) rootView.findViewById(i);
		ViewGroup.LayoutParams layoutParams = scrollView.getLayoutParams();
		layoutParams.height = 0;
		if (max_height > 800) {
			layoutParams.height = 850;
		} else {
			layoutParams.height = -2;
		}
		scrollView.setLayoutParams(layoutParams);
		isShow = true;
	}

	/**
	 * 关闭
	 */
	public void hide(int i) {
		ScrollView scrollView = (ScrollView) rootView.findViewById(i);
		ViewGroup.LayoutParams layoutParams = scrollView.getLayoutParams();
		layoutParams.height = 0;
		scrollView.setLayoutParams(layoutParams);
		isShow = false;
	}
}
