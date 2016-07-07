package com.app.main.ifileexplorer.adapterhelper;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class CommonAdapter<T> extends BaseAdapter {
	
	protected LayoutInflater mInflater;
	protected List<T> mDataList;
	protected Context mContext;
	private int mLayoutId;
	
	public CommonAdapter(Context context,List<T> dataList,int layoutId) {
		mContext=context;
		mDataList=dataList;
		mLayoutId=layoutId;
		mInflater=LayoutInflater.from(mContext);
	}

	@Override
	public int getCount() {
		return mDataList.size();
	}

	@Override
	public T getItem(int position) {
		return mDataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		CommonViewHolder commonViewHolder=CommonViewHolder.getCommonViewHolder(mContext, convertView, parent, mLayoutId, position);
		convert(commonViewHolder,getItem(position));
		return commonViewHolder.getConvertView();
	}
	
	public abstract void convert(CommonViewHolder commonViewHolder,T t);

}
