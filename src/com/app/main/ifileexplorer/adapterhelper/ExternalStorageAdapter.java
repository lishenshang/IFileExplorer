package com.app.main.ifileexplorer.adapterhelper;

import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.app.main.ifileexplorer.MainActivity;
import com.app.main.ifileexplorer.R;
import com.app.main.ifileexplorer.utils.FileCategoryHelper;

public class ExternalStorageAdapter extends CommonAdapter<CommonBean> implements OnScrollListener{

	private List<CommonBean> mDataList;
	private Handler mHandler;
	private ListView mListView;
	private int mStart,mEnd,mOldStart,mOldEnd;
	private boolean mIsOne=true;
	
	public ExternalStorageAdapter(Context context, Handler handler,List<CommonBean> dataList,
			int layoutId,ListView listView) {
		super(context, dataList, layoutId);
		mDataList=dataList;
		mHandler=handler;
		mListView=listView;
		mIsOne=true;
		mListView.setOnScrollListener(this);
	}

	@Override
	public void convert(CommonViewHolder commonViewHolder, final CommonBean commonBean) {
		commonViewHolder.setImageBitmap(R.id.id_icon, commonBean.getIcon(mContext));
		commonViewHolder.setText(R.id.id_filename, commonBean.getTitle());
		commonViewHolder.setChecked(R.id.id_checked, commonBean.getIsSeleced());
		commonViewHolder.getView(R.id.id_checked).setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View view) {
				if(commonBean.getIsSeleced())
				{
					commonBean.setSeleced(false);
				}
				else
				{
					commonBean.setSeleced(true);
				}
				Message msg=null;
				if(getSelectedCount()==0)
				{
					msg=mHandler.obtainMessage(MainActivity.MSG_CHANGEDSTATEANDVIEW, MainActivity.STATE_NORMAL);					
				}
				else
				{
					msg=mHandler.obtainMessage(MainActivity.MSG_CHANGEDSTATEANDVIEW, MainActivity.STATE_SELECTED);
				}
				mHandler.sendMessage(msg);
			}
		});
	}
	
	public int getSelectedCount()
	{
		int count=0;
		for(int i=0;i<mDataList.size();i++)
		{
			if(mDataList.get(i).getIsSeleced())
			{
				count++;
			}
		}
		return count;
	}

	public int getCurrentSelectedPosition()
	{
		if(getSelectedCount()==1)
		{
			for (int i = 0; i < mDataList.size(); i++) {
				if(mDataList.get(i).getIsSeleced())
				{
					return i;
				}
			}
		}
		return -1;
	}
	
	public List<CommonBean> getDataList()
	{
		return mDataList;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if(scrollState==SCROLL_STATE_IDLE)
		{
			Thread updateThread1 = new Thread() {
				public void run() {
					
					for (int i = mOldStart; i < mOldEnd; i++) {
						mDataList.get(i).setIcon(FileCategoryHelper.getFileBitmap(
								mContext, mDataList.get(i).getContent(), true));
//						mDataList.get(i).setIconId(R.drawable.image);
						Message msg = mHandler.obtainMessage(MainActivity.MSG_FILE_GETBITMAP,0,2,ExternalStorageAdapter.this);
						mHandler.sendMessage(msg);
					}
					
					for (int i = mStart; i < mEnd; i++) {
						mDataList.get(i).setIcon(
								FileCategoryHelper.getFileBitmap(
										mContext, mDataList.get(i).getContent(), false));
						Message msg = mHandler.obtainMessage(MainActivity.MSG_FILE_GETBITMAP,i,mEnd,ExternalStorageAdapter.this);
						mHandler.sendMessage(msg);
					}
					if(mEnd==0)
					{
						Message msg = mHandler.obtainMessage(MainActivity.MSG_FILE_GETBITMAP,0,1,ExternalStorageAdapter.this);
						mHandler.sendMessage(msg);
					}	
					mOldStart=mStart;
					mOldEnd=mEnd;					
				};
			};
			Message msg = mHandler.obtainMessage(MainActivity.MSG_EXECUTETHREAD,
					updateThread1);
			mHandler.sendMessage(msg);
			
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		mStart=firstVisibleItem;
		mEnd=firstVisibleItem+visibleItemCount;
		if(mIsOne&&visibleItemCount>0)
		{
			Thread updateThread1 = new Thread() {
				public void run() {
					for (int i = mStart; i < mEnd; i++) {
						mDataList.get(i).setIcon(
								FileCategoryHelper.getFileBitmap(
										mContext, mDataList.get(i).getContent(), false));
						Message msg = mHandler.obtainMessage(MainActivity.MSG_FILE_GETBITMAP,i,mEnd,ExternalStorageAdapter.this);
						mHandler.sendMessage(msg);
					}
					if(mEnd==0)
					{
						Message msg = mHandler.obtainMessage(MainActivity.MSG_FILE_GETBITMAP,0,1,ExternalStorageAdapter.this);
						mHandler.sendMessage(msg);
					}
					mOldStart=mStart;
					mOldEnd=mEnd;	
				};
			};
			Message msg = mHandler.obtainMessage(MainActivity.MSG_EXECUTETHREAD,
					updateThread1);
			mHandler.sendMessage(msg);
			mIsOne=false;
			
		}
	}
	
}
