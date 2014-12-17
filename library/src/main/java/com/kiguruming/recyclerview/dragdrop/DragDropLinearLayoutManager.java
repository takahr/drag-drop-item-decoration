package com.kiguruming.recyclerview.dragdrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * @author takahr@gmail.com
 */
public class DragDropLinearLayoutManager extends LinearLayoutManager implements RecyclerView.OnItemTouchListener {
	private final static String TAG = DragDropLinearLayoutManager.class.getSimpleName();

	public static interface OnItemDragDropListener {
		void onItemDrag(RecyclerView parent, View view, int position, long id);
		void onItemDrop(RecyclerView parent, int startPosition, int endPosition, long id);
		boolean canDrag(RecyclerView parent, View view, int position, long id);
		boolean canDrop(RecyclerView parent, int startPosition, int endPosition, long id);

		void moveItem(int fromPosition, int toPosition);
	}

	private RecyclerView mRecyclerView;

	private boolean mDraggingEnabled = true;

	private int mStartPosition;
	private long mDragItemId;
	private int mDragPointOffsetX;
	private int mDragPointOffsetY;

	private int mPlaceholderPosition;

	private WindowManager mWm;
	private ImageView mDragView;

	private OnItemDragDropListener mItemDragDropListener;

	public DragDropLinearLayoutManager(Context context) {
		super(context);
	}

	public DragDropLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
		super(context, orientation, reverseLayout);
	}

	public void setOnItemDragDropListener(OnItemDragDropListener listener) {
		mItemDragDropListener = listener;
	}

	@Override
	public void onAttachedToWindow(RecyclerView view) {
		super.onAttachedToWindow(view);
		view.addOnItemTouchListener(this);
		mRecyclerView = view;
	}

	@Override
	public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
		super.onDetachedFromWindow(view, recycler);
		view.removeOnItemTouchListener(this);
		mRecyclerView = null;
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent ev) {
		if (!mDraggingEnabled || mRecyclerView == null) {
			return false;
		}

		final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
		if (adapter == null) {
			return false;
		}

		return mDragView != null;
	}

	@Override
	public void onTouchEvent(RecyclerView rv, MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();

		if (mDragView == null|| !mDraggingEnabled || mRecyclerView == null) {
			return;
		}

		final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
		if (adapter == null) {
			return;
		}

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_MOVE:
				inDragging(0, y);
				break;
			case MotionEvent.ACTION_CANCEL:
				cancelDrag();
				break;
			case MotionEvent.ACTION_UP:
			default:
				if (mStartPosition >= 0) {
					// check if the position is a header/footer
					final View dropAtView = mRecyclerView.findChildViewUnder(x, y);
					if (dropAtView != null) {
						final int dropPosition = getPosition(dropAtView);
						final long dropItemId = adapter.getItemId(dropPosition);
						if (canDrop(dropPosition, dropItemId)) {
							dropAt(dropPosition);
							break;
						}
					}
					cancelDrag();
				}
				break;
		}

		// intercept and cancel  event in
		ev.setAction(MotionEvent.ACTION_CANCEL);
	}

	private boolean canDrag(View dragItem, int startPosition, long id) {
		if (mItemDragDropListener != null) {
			return mItemDragDropListener.canDrag(mRecyclerView, dragItem, startPosition, id);
		}

		return false;
	}

	private boolean canDrop(int dropPosition, long id) {
		if (mItemDragDropListener != null) {
			return mItemDragDropListener.canDrop(mRecyclerView, mStartPosition, dropPosition, id);
		}

		return false;
	}

	public void startDrag(View dragItem, int x, int y, int offsetX, int offsetY) {
		if (dragItem == null || mRecyclerView == null) {
			return;
		}

		final int startPosition = getPosition(dragItem);

		final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
		if (adapter == null) {
			return;
		}

		final long id = adapter.getItemId(startPosition);

		if (!canDrag(dragItem, startPosition, id)) {
			return;
		}

		mStartPosition = startPosition;
		if (mItemDragDropListener != null) {
			mItemDragDropListener.onItemDrag(mRecyclerView, dragItem, mStartPosition, id);
		}

		dragItem.setDrawingCacheEnabled(true);

		// Create a copy of the drawing cache so that it does not get recycled
		// by the framework when the list tries to clean up memory
		Bitmap bitmap = Bitmap.createBitmap(dragItem.getDrawingCache());

		mDragPointOffsetX = offsetX;
		mDragPointOffsetY = offsetY;

		WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
		mWindowParams.gravity = Gravity.TOP;
		mWindowParams.x = x - mDragPointOffsetX;
		mWindowParams.y = y - mDragPointOffsetY;

		mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		mWindowParams.format = PixelFormat.TRANSLUCENT;
		mWindowParams.windowAnimations = 0;

		Context context = mRecyclerView.getContext();
		ImageView v = new ImageView(context);
		v.setImageBitmap(bitmap);

		if (mWm == null) {
			mWm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		}

		mWm.addView(v, mWindowParams);
		mDragView = v;

		dragItem.setVisibility(View.INVISIBLE);
		mPlaceholderPosition = mStartPosition;

		dragItem.invalidate(); // We have not changed anything else.
	}

	private void inDragging(int x, int y) {
		if (mRecyclerView == null) {
			return;
		}

		final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
		if (adapter == null) {
			return;
		}

		final View currentView = mRecyclerView.findChildViewUnder(x, y);
		final int currentPosition = mRecyclerView.getChildPosition(currentView);
		final long id = adapter.getItemId(currentPosition);

		if (mPlaceholderPosition != currentPosition && currentPosition >= 0) {
			if (canDrop(currentPosition, id)) {
				if (mPlaceholderPosition >= 0 && currentPosition >= 0) {
					Log.d(TAG, String.format("moveView %d -> %d", mPlaceholderPosition, currentPosition));
					moveItem(mPlaceholderPosition, currentPosition);
				}
				mPlaceholderPosition = currentPosition;
			}
		}

		if (mDragView == null) return;

		WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams)mDragView.getLayoutParams();
		layoutParams.x = x - mDragPointOffsetX;
		layoutParams.y = y - mDragPointOffsetY;

		mWm.updateViewLayout(mDragView, layoutParams);
	}

	private void dropAt(int dropPosition) {
		if (mRecyclerView == null) {
			return;
		}

		if (mPlaceholderPosition != dropPosition) {
			moveItem(mPlaceholderPosition, dropPosition);
		}

		mWm.removeView(mDragView);
		mDragView.setImageDrawable(null);
		mDragView = null;
		showItemView(dropPosition);

		if (mItemDragDropListener != null) {
			mItemDragDropListener.onItemDrop(mRecyclerView, mStartPosition, dropPosition, mDragItemId);
		}
	}

	private void cancelDrag() {
		moveItem(mPlaceholderPosition, mStartPosition);
		showItemView(mStartPosition);
		mStartPosition = -1;
		mDragItemId = -1;
		mWm.removeView(mDragView);
		mDragView.setImageDrawable(null);
		mDragView = null;
	}

	private void moveItem(int fromPosition, int toPosition) {
		if (mItemDragDropListener != null) {
			mItemDragDropListener.moveItem(fromPosition, toPosition);
		}
	}

	private void showItemView(int position) {
		for (int i = 0; i < getChildCount(); i++) {
			final View childView = getChildAt(i);
			final int childPosition = getPosition(childView);
			if (position == childPosition) {
				childView.setVisibility(View.VISIBLE);
			}
		}
	}

	public void setDraggingEnabled(boolean draggingEnabled) {
		mDraggingEnabled = draggingEnabled;
	}

}
