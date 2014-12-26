package com.kiguruming.recyclerview.dragdrop;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author takahr@gmail.com
 */
public class DragDropItemDecoration extends RecyclerView.ItemDecoration implements RecyclerView.OnItemTouchListener {
	private final static String TAG = DragDropItemDecoration.class.getSimpleName();

    public static interface OnItemDragDropListener {
		void onItemDrag(RecyclerView parent, View view, int position, long id);
		void onItemDrop(RecyclerView parent, int startPosition, int endPosition, long id);
		boolean canDrag(RecyclerView parent, View view, int position, long id);
		boolean canDrop(RecyclerView parent, int startPosition, int endPosition, long id);

		void moveItem(int fromPosition, int toPosition);
	}

	private boolean mDraggingEnabled = true;

	private int mStartPosition;
	private long mDragItemId;
	private int mDragPointOffsetX;
	private int mDragPointOffsetY;

    private int mDragViewX;
    private int mDragViewY;

	private int mPlaceholderPosition;

	private View mDragView;

    private MotionEvent mLastDownEvent;

	private OnItemDragDropListener mItemDragDropListener;

	public void setOnItemDragDropListener(OnItemDragDropListener listener) {
		mItemDragDropListener = listener;
	}

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        if (mDragView != null) {
            c.save();
            c.translate(mDragViewX, mDragViewY);
            mDragView.draw(c);
            c.restore();
        }
    }

    @Override
	public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent ev) {
		if (!mDraggingEnabled || rv == null) {
			return false;
		}

		final RecyclerView.Adapter adapter = rv.getAdapter();
		if (adapter == null) {
			return false;
		}

        mLastDownEvent = MotionEvent.obtain(ev);

        return mDragView != null;
	}

	@Override
	public void onTouchEvent(RecyclerView rv, MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();

		if (mDragView == null|| !mDraggingEnabled || rv == null) {
			return;
		}

		final RecyclerView.Adapter adapter = rv.getAdapter();
		if (adapter == null) {
			return;
		}

        final RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();
        if (layoutManager == null) {
            return;
        }

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_MOVE:
				inDragging(rv, 0, y);
				break;
			case MotionEvent.ACTION_CANCEL:
				cancelDrag(rv);
				break;
			case MotionEvent.ACTION_UP:
			default:
				if (mStartPosition >= 0) {
					// check if the position is a header/footer
					final View dropAtView = rv.findChildViewUnder(x, y);
					if (dropAtView != null) {
						final int dropPosition = layoutManager.getPosition(dropAtView);
						final long dropItemId = adapter.getItemId(dropPosition);
						if (canDrop(rv, dropPosition, dropItemId)) {
							dropAt(rv, dropPosition);
							break;
						}
					}
					cancelDrag(rv);
				}
				break;
		}

		// intercept and cancel  event in
		ev.setAction(MotionEvent.ACTION_CANCEL);
	}

    public MotionEvent getLastDownEvent() {
        return mLastDownEvent;
    }

	private boolean canDrag(RecyclerView rv, View dragItem, int startPosition, long id) {
		if (mItemDragDropListener != null) {
			return mItemDragDropListener.canDrag(rv, dragItem, startPosition, id);
		}

		return false;
	}

	private boolean canDrop(RecyclerView rv, int dropPosition, long id) {
		if (mItemDragDropListener != null) {
			return mItemDragDropListener.canDrop(rv, mStartPosition, dropPosition, id);
		}

		return false;
	}

	public void startDrag(RecyclerView rv, View dragItem, int x, int y, int offsetX, int offsetY) {
		if (dragItem == null || rv == null) {
			return;
		}

        final RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
		final int startPosition = layoutManager.getPosition(dragItem);

		final RecyclerView.Adapter adapter = rv.getAdapter();
		if (adapter == null) {
			return;
		}

		final long id = adapter.getItemId(startPosition);

		if (!canDrag(rv, dragItem, startPosition, id)) {
			return;
		}

		mStartPosition = startPosition;
		if (mItemDragDropListener != null) {
			mItemDragDropListener.onItemDrag(rv, dragItem, mStartPosition, id);
		}

		mDragPointOffsetX = offsetX;
		mDragPointOffsetY = offsetY;

        mDragViewX = x - mDragPointOffsetX;
        mDragViewY = y - mDragPointOffsetY;

        final RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(rv, adapter.getItemViewType(mStartPosition));
        adapter.bindViewHolder(viewHolder, mStartPosition);

        mDragView = viewHolder.itemView;

        // measure & layout
        final int ws = View.MeasureSpec.makeMeasureSpec(dragItem.getWidth(), View.MeasureSpec.EXACTLY);
        final int hs = View.MeasureSpec.makeMeasureSpec(dragItem.getHeight(), View.MeasureSpec.EXACTLY);
        mDragView.measure(ws, hs);
        mDragView.layout(0, 0, mDragView.getMeasuredWidth(), mDragView.getMeasuredHeight());

		dragItem.setVisibility(View.INVISIBLE);
		mPlaceholderPosition = mStartPosition;

		dragItem.invalidate(); // We have not changed anything else.
	}

	private void inDragging(RecyclerView rv, int x, int y) {
		if (rv == null) {
			return;
		}

		final RecyclerView.Adapter adapter = rv.getAdapter();
		if (adapter == null) {
			return;
		}

		final View currentView = rv.findChildViewUnder(x, y);
		final int currentPosition = rv.getChildPosition(currentView);
		final long id = adapter.getItemId(currentPosition);

		if (mPlaceholderPosition != currentPosition && currentPosition >= 0) {
			if (canDrop(rv, currentPosition, id)) {
				if (mPlaceholderPosition >= 0 && currentPosition >= 0) {
					moveItem(mPlaceholderPosition, currentPosition);
				}
				mPlaceholderPosition = currentPosition;
			}
		}

		if (mDragView == null) return;

		mDragViewX = x - mDragPointOffsetX;
		mDragViewY = y - mDragPointOffsetY;

        if (y - mDragPointOffsetY < rv.getTop() && rv.getScrollState() != RecyclerView.SCROLL_STATE_SETTLING) {
            rv.scrollBy(0, -mDragView.getHeight() / 8);
        }
        if (y - mDragPointOffsetY + mDragView.getHeight() > rv.getBottom() && rv.getScrollState() != RecyclerView.SCROLL_STATE_SETTLING) {
            rv.scrollBy(0, mDragView.getHeight() / 8);
        }
	}

	private void dropAt(RecyclerView rv, int dropPosition) {
		if (rv == null) {
			return;
		}

		if (mPlaceholderPosition != dropPosition) {
			moveItem(mPlaceholderPosition, dropPosition);
		}

		mDragView = null;
		showItemView(rv, dropPosition);

		if (mItemDragDropListener != null) {
			mItemDragDropListener.onItemDrop(rv, mStartPosition, dropPosition, mDragItemId);
		}
	}

	private void cancelDrag(RecyclerView rv) {
		moveItem(mPlaceholderPosition, mStartPosition);
		showItemView(rv, mStartPosition);
		mStartPosition = -1;
		mDragItemId = -1;
		mDragView = null;
	}

	private void moveItem(int fromPosition, int toPosition) {
		if (mItemDragDropListener != null) {
			mItemDragDropListener.moveItem(fromPosition, toPosition);
		}
	}

	private void showItemView(RecyclerView rv, int position) {
        if (rv == null) {
            return;
        }
        final RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
		for (int i = 0; i < layoutManager.getChildCount(); i++) {
			final View childView = layoutManager.getChildAt(i);
			final int childPosition = layoutManager.getPosition(childView);
			if (position == childPosition) {
				childView.setVisibility(View.VISIBLE);
			}
		}
	}

	public void setDraggingEnabled(boolean draggingEnabled) {
		mDraggingEnabled = draggingEnabled;
	}

}
