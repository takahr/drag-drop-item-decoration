package com.kiguruming.recyclerview.dragdrop.example;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kiguruming.recyclerview.dragdrop.DragDropLinearLayoutManager;

/**
 * @author takahr@gmail.com
 */
public class RecyclerExampleFragment extends Fragment implements View.OnLongClickListener, DragDropLinearLayoutManager.OnItemDragDropListener {

	private RecyclerView mRecyclerView;
	private ExampleRecyclerAdapter mAdapter;
	private DragDropLinearLayoutManager mLayoutManager;

	public RecyclerExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_example, container, false);

		mRecyclerView = (RecyclerView) root.findViewById(R.id.recycler);
		mRecyclerView.setHasFixedSize(true);

		mLayoutManager = new DragDropLinearLayoutManager(getActivity(), DragDropLinearLayoutManager.VERTICAL, false);
		mLayoutManager.setOnItemDragDropListener(this);
		mLayoutManager.setDraggingEnabled(true);
		mRecyclerView.setLayoutManager(mLayoutManager);

		mAdapter = new ExampleRecyclerAdapter();
		mRecyclerView.setAdapter(mAdapter);
		
		mAdapter.setOnItemLongClickListener(this);

		return root;
	}

	@Override
	public void onItemDrag(RecyclerView parent, View view, int position, long id) {

	}

	@Override
	public void onItemDrop(RecyclerView parent, int startPosition, int endPosition, long id) {

	}

	@Override
	public boolean canDrag(RecyclerView parent, View view, int position, long id) {
		return true;
	}

	@Override
	public boolean canDrop(RecyclerView parent, int startPosition, int endPosition, long id) {
		return !(endPosition % 5 == 0);
	}

	@Override
	public void moveItem(int fromPosition, int toPosition) {
		mAdapter.moveItem(fromPosition, toPosition);
	}

	@Override
	public boolean onLongClick(View v) {
		mLayoutManager.startDrag(v, 0, 0, 0, 0);
		return true;
	}
}