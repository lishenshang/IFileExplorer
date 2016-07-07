package com.app.main.ifileexplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.app.main.ifileexplorer.adapterhelper.CommonAdapter;
import com.app.main.ifileexplorer.adapterhelper.CommonBean;
import com.app.main.ifileexplorer.adapterhelper.CommonViewHolder;
import com.app.main.ifileexplorer.utils.ExternalStorageHelper;
import com.app.main.ifileexplorer.utils.FileCategoryHelper;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.text.format.Formatter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class FileCategoryBrowser {

	// 扫描文件的线程List
	public static final List<Thread> TASK = new ArrayList<Thread>();

	private static List<String> sAudioList;
	private static List<String> sVideoList;
	private static List<String> sImageList;
	private static List<String> sDocumentList;
	private static List<String> sCompressionList;
	private static List<String> sApkList;
	private static List<CommonBean> sDataList;

	protected static CommonAdapter mCommonAdapter;

	protected static String[] sCategoryTitle = { "音乐", "视频", "图片", "文档", "压缩包",
			"安装包" };

	private Context mContext;
	private Handler mHandler;
	private ListView mListView;
	private int mLev = 0;

	private List<String> mSdcardList;

	private FileCategoryHelper mFileCategoryHelper;

	public FileCategoryBrowser(Context context, Handler handler,
			ListView listView) {
		mContext = context;
		mHandler = handler;
		mListView = listView;
		sAudioList = new ArrayList<String>();
		sVideoList = new ArrayList<String>();
		sImageList = new ArrayList<String>();
		sDocumentList = new ArrayList<String>();
		sCompressionList = new ArrayList<String>();
		sApkList = new ArrayList<String>();
		mFileCategoryHelper = new FileCategoryHelper();
		categoryFile();
		initDatas();
		registerListener();
	}

	public void categoryFile() {
		mSdcardList=ExternalStorageHelper.getExternalStorageDirectory(mContext);
		Thread initDatasThread1 = new Thread() {
			@Override
			public void run() {
				sAudioList = FileCategoryHelper.getSystemAudio(mContext);
				sVideoList = FileCategoryHelper.getSystemVideo(mContext);
				sImageList = FileCategoryHelper.getSystemImage(mContext);
				Collections.sort(sAudioList);
				Collections.sort(sVideoList);
				Collections.sort(sImageList);
				TASK.remove(0);
				Message message = mHandler.obtainMessage(
						MainActivity.MSG_CATEGORYBROWSER_UPDATE_DATAS,
						TASK.size(), 0);
				mHandler.sendMessage(message);
			}
		};
		TASK.add(initDatasThread1);
		Message msg = mHandler.obtainMessage(MainActivity.MSG_EXECUTETHREAD,
				initDatasThread1);
		mHandler.sendMessage(msg);
		distributionThread(mSdcardList,7);
	}

	// 初始化数据
	public void initDatas() {
		sDataList = new ArrayList<CommonBean>();
		for (int i = 0; i < sCategoryTitle.length; i++) {
			sDataList.add(new CommonBean(getImageBitmap(mContext, i),
					sCategoryTitle[i] + "(" + getSystemCategoryFile(i).size()
							+ ")", false));
		}
		for (int i = 0; i < mSdcardList.size(); i++) {
			sDataList.add(new CommonBean(BitmapFactory.decodeResource(
					mContext.getResources(), R.drawable.sd), new File(
					mSdcardList.get(i)).getName()
					+ "("
					+ getSDCardInfo(mSdcardList.get(i),mContext) + ")", false));
		}
		mCommonAdapter=new CommonAdapter<CommonBean>(mContext,sDataList,R.layout.item_listview_common1) {
			@Override
			public void convert(CommonViewHolder commonViewHolder, CommonBean commonBean) {
				commonViewHolder.setImageBitmap(R.id.id_icon, commonBean.getIcon(mContext));
				commonViewHolder.setText(R.id.id_filename, commonBean.getTitle());
				commonViewHolder.setChecked(R.id.id_checked, commonBean.getIsSeleced());
			}
		};
		mListView.setAdapter(mCommonAdapter);
		mCommonAdapter.notifyDataSetChanged();

	}

	// 注册监听
	public void registerListener() {
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position < 6) {
					Intent intent = new Intent(mContext, CommonActivity.class);
					intent.putExtra("dataList", position);
					intent.putExtra("tag", MainActivity.TAG_CATEGORYBROWSER);
					mContext.startActivity(intent);
				}
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public static String getSDCardInfo(String path,Context context) {
		
		StatFs sf = new StatFs(path);
		long oneSize = 0;
		long freeCount = 0;
		long blockCount = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			oneSize = sf.getBlockSizeLong();
			blockCount = sf.getBlockCountLong();
			freeCount =  sf.getAvailableBlocksLong();
		}
		else
		{
			oneSize = sf.getBlockSize();
		blockCount = sf.getBlockCount();
		freeCount = sf.getAvailableBlocks();
		}
		String size=Formatter.formatFileSize(context, oneSize*blockCount);
		String usedSize=Formatter.formatFileSize(context, oneSize*(blockCount-freeCount));
		return usedSize+"/"+size;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public static float getSDCardSize(String path,Context context) {
		StatFs sf = new StatFs(path);
		long oneSize = 0;
		long freeCount = 0;
		long blockCount = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			oneSize = sf.getBlockSizeLong();
			blockCount = sf.getBlockCountLong();
			freeCount =  sf.getAvailableBlocksLong();
		}
		else
		{
			oneSize = sf.getBlockSize();
			blockCount = sf.getBlockCount();
			freeCount = sf.getAvailableBlocks();
		}
		float size = (float) (oneSize * blockCount) / 1024 / 1024 / 1024;
		return size;
	}
	
	// 分配线程扫描SD卡
	public void distributionThread(final List<String> pathList,final int lev) {
		mLev++;
		for (final String path : pathList) {
			Thread initDatasThread = new Thread() {
				@Override
				public void run() {
					File file=new File(path);
					if(mLev==lev&&file.isDirectory())
					{
						List<String> list = new ArrayList<String>();
						File[] files = new File(path).listFiles();
						for (File tempFile : files) {
							list.add(tempFile.getAbsolutePath());
						}
						distributionThread(list,lev);
					}
					else
					{
						mFileCategoryHelper.getSystemFileCategory(path,
								mContext);
					}
					TASK.remove(0);
					Message message = mHandler.obtainMessage(
							MainActivity.MSG_CATEGORYBROWSER_UPDATE_DATAS,
							TASK.size(), 0);
					mHandler.sendMessage(message);
					if (TASK.size() == 0) {
						sDocumentList = mFileCategoryHelper.getSystemDocument();
						sCompressionList = mFileCategoryHelper
								.getSystemCompression();
						sApkList = mFileCategoryHelper.getSystemApk();
						Collections.sort(sDocumentList);
						Collections.sort(sCompressionList);
						Collections.sort(sApkList);
					}
				}
			};
			TASK.add(initDatasThread);
			Message msg1 = mHandler.obtainMessage(
					MainActivity.MSG_EXECUTETHREAD, initDatasThread);
			mHandler.sendMessage(msg1);
		}
	}

	// 获取系统的分类文件图标
	public static Bitmap getImageBitmap(Context context, int position) {
		if (position == 0) {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.music);
		} else if (position == 1) {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.video);
		} else if (position == 2) {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.image);
		} else if (position == 3) {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.text);
		} else if (position == 4) {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.archive_yellow);
		} else if (position == 5) {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.apk);
		} else {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.unknown);
		}
	}

	// 获取系统的分类文件列表
	public static List<String> getSystemCategoryFile(int position) {
		if (position == 0) {
			return sAudioList;
		} else if (position == 1) {
			return sVideoList;
		} else if (position == 2) {
			return sImageList;
		} else if (position == 3) {
			return sDocumentList;
		} else if (position == 4) {
			return sCompressionList;
		} else if (position == 5) {
			return sApkList;
		}
		return null;
	}

	public static List<CommonBean> getDataList() {
		return sDataList;
	}

	public static void setTitle(int position) {
		sDataList.get(position).setTitle(
				sCategoryTitle[position] + "("
						+ getSystemCategoryFile(position).size() + ")");
		mCommonAdapter.notifyDataSetChanged();
	}

}
