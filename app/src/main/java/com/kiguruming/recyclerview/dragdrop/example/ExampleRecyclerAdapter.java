package com.kiguruming.recyclerview.dragdrop.example;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * @author takahr@gmail.com
 */
public class ExampleRecyclerAdapter extends RecyclerView.Adapter<ExampleRecyclerAdapter.ItemViewHolder> {
	public static class Item {
		long id;
		String label;

        public Item(long id, String label) {
			this.id = id;
            this.label = label;
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
		private final TextView mTestLabel;
		private final TextView mDataLabel;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mDataLabel = (TextView) itemView.findViewById(R.id.item_label);
			mTestLabel = (TextView) itemView.findViewById(R.id.test_label);
        }
    }

    ArrayList<Item> mItems = new ArrayList<Item>();
	private View.OnLongClickListener mOnItemLongClickListener;

	public ExampleRecyclerAdapter() {
		super();

		for (int i = 0; i < 40; i++) {
			mItems.add(new Item(i, String.valueOf(i)));
		}
	}

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

		View itemView = inflater.inflate(R.layout.adapter_item, parent, false);
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder viewHolder, int position) {
        final Item item = mItems.get(position);
		viewHolder.mDataLabel.setText(item.label);
		if (item.id % 2 == 0) {
			viewHolder.mTestLabel.setText(item.label);
			viewHolder.mTestLabel.setVisibility(View.VISIBLE);
		} else {
			viewHolder.mTestLabel.setVisibility(View.GONE);
		}
		viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (mOnItemLongClickListener != null) {
					mOnItemLongClickListener.onLongClick(v);
				}
				return true;
			}
		});
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

	public void moveItem(int fromPosition, int toPosition) {
		final Item tmp = mItems.remove(fromPosition);
        mItems.add(toPosition, tmp);
		notifyItemMoved(fromPosition, toPosition);
	}

	public void setOnItemLongClickListener(View.OnLongClickListener listener) {
		mOnItemLongClickListener = listener;
	}
}
