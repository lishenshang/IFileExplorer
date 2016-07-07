package com.app.main.ifileexplorer.adapterhelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class CommonViewHolder {
	
	private SparseArray<View> mViews;
	private int mPosition;
	private View mConvertView;
	
	private CommonViewHolder(Context context,ViewGroup parent,int layoutId,int position)
	{
		mPosition=position;
		mViews=new SparseArray<View>();
		mConvertView=LayoutInflater.from(context).inflate(layoutId, parent,false);
		mConvertView.setTag(this);
	}
	
	public static CommonViewHolder getCommonViewHolder(Context context,View convertView,ViewGroup parent,int layoutId,int position)
	{
		if(convertView==null)
		{
			return new CommonViewHolder(context, parent, layoutId, position);
		}
		else
		{
			CommonViewHolder commonViewHolder=(CommonViewHolder) convertView.getTag();
			commonViewHolder.mPosition=position;
			return commonViewHolder;
		}
	}
	
	public <T extends View> T getView(int viewId)
	{
		View view=mViews.get(viewId);
		if(view==null)
		{
			view=mConvertView.findViewById(viewId);
			mViews.put(viewId, view);
		}
		return (T) view;
	}

	public View getConvertView() {
		return mConvertView;
	}
	
	public CommonViewHolder setText(int viewId,String text)
	{
		TextView tv=getView(viewId);
		tv.setText(text);
		return this;
	}
	
	public CommonViewHolder setImageResource(int viewId,int resId)
	{
		ImageView view=getView(viewId);
		view.setImageResource(resId);
		return this;
	}
	
	public CommonViewHolder setImageBitmap(int viewId,Bitmap bitmap)
	{
		ImageView view=getView(viewId);
		view.setImageBitmap(bitmap);
		return this;
	}
	
	public CommonViewHolder setChecked(int viewId,boolean flag)
	{
		CheckBox view=getView(viewId);
		view.setChecked(flag);
		return this;
	}

	public int getPosition() {
		return mPosition;
	}
	
}
