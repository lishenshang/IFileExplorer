package com.app.main.ifileexplorer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.app.main.ifileexplorer.adapterhelper.CommonBean;
import com.app.main.ifileexplorer.adapterhelper.ExternalStorageAdapter;
import com.app.main.ifileexplorer.utils.ExternalStorageHelper;
import com.app.main.ifileexplorer.utils.FileCategoryHelper;
import com.app.main.ifileexplorer.utils.FileHelper;

public class MainActivity extends Activity implements OnClickListener {

	public static final String TABSPEC_CATEGORYBROWSER = "分类浏览";
	public static final String TABSPEC_EXTERNAL_STORAGE = "外部存储";
	public static final String TABSPEC_SEARCHRESULT = "搜索结果";

	public static final String PATH_EXTERNAL_STORAGE = Environment
			.getExternalStorageDirectory().getParent();

	public static final String STATE_NORMAL = "normal";
	public static final String STATE_SELECTED = "selected";
	public static final String STATE_OPERTION = "operation";
	public static String sStateCur = STATE_NORMAL;

	public static final String TAG_OPENWAY = "openWay";
	public static final String TAG_CATEGORYBROWSER = "categoryBrowser";

	public static final String STYLE_CONTEXTMENU_ONITEMCLICK = "clickItem";
	public static final String STYLE_CONTEXTMENU_ONITEMLONGCLICK = "LongClickItem";
	public static String mStyleContextMenu = null;

	//线程池
	public static ExecutorService sExecutorService = Executors
			.newCachedThreadPool();

	//线程集合
	public static final List<Thread> mListTask = new ArrayList<Thread>();

	public static final int MSG_FILE_UPDATE = 0;
	public static final int MSG_FILE_DELETE = 1;
	public static final int MSG_FILE_RENAME = 2;
	public static final int MSG_FILE_PASTE = 3;
	public static final int MSG_FILE_ATTRIBUTE = 4;
	public static final int MSG_FILE_NEW = 5;
	public static final int MSG_FILE_GETBITMAP = 6;
	public static final int MSG_CHANGEDSTATEANDVIEW = 7;
	public static final int MSG_FILE_SEARCHFINISHED = 8;
	public static final int MSG_EXECUTETHREAD = 9;
	public static final int MSG_CATEGORYBROWSER_UPDATE_DATAS = 10;

	private RelativeLayout mTopBar;
	private ImageView mTopBarImageView;
	public TextView mTopBarTextView;
	public static ProgressBar mTopBarProgressBar;

	private LinearLayout mBottomBar;
	private LinearLayout mBottomBar1;
	private ImageButton mBottomBarCopyImageButton;
	private ImageButton mBottomBarCutImageButton;
	private ImageButton mBottomBarDeleteImageButton;
	private ImageButton mBottomBarMoreImageButton;
	private LinearLayout mBottomBar2;
	private Button mBottomBar2PasteButton;
	private Button mBottomBar2CancelButton;

	private TabHost mTabHost;
	private ListView mExternalStorageListView;
	private ListView mFileCategoryListView;

	private String mTopBarText;
	private int mPosition;
	private boolean mIsError;
	private boolean mIsCopy;
	private boolean mIsRefactorTab = false;

	private ExternalStorageHelper mExternalStorageHelper;
	private FileCategoryBrowser mFileCategoryBrowser;
	private ExternalStorageAdapter mExternalStorageAdapter;

	private List<TabSpec> mAllTabs;
	private List<String> mCopyFileList;
	private List<CommonBean> mDataList;
	private List<String> mResultPathLists;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_FILE_UPDATE:
				updateDatasFinished((List<String>) msg.obj);
				mListTask.remove(0);
				mTabHost.setCurrentTabByTag(TABSPEC_EXTERNAL_STORAGE);
				break;
			case MSG_FILE_DELETE:
				if(msg.arg1==msg.arg2)
				{
					Toast.makeText(MainActivity.this, "文件删除成功", Toast.LENGTH_SHORT).show();
				}else
				{
					Toast.makeText(MainActivity.this, (msg.arg2-msg.arg1)+"个文件删除失败", Toast.LENGTH_SHORT).show();
				}
				updateDatas(mTopBarText);
				mListTask.remove(0);
				break;
			case MSG_FILE_RENAME:
				if ((Boolean) msg.obj) {
					updateDatas(mTopBarText);
				}
				else
				{
					Toast.makeText(MainActivity.this, "重命名出错", Toast.LENGTH_SHORT).show();
				}
				changedStateAndView(STATE_NORMAL);
				mListTask.remove(0);
				break;
			case MSG_FILE_PASTE:
				if(msg.arg1==msg.arg2)
				{
					Toast.makeText(MainActivity.this, "文件复制成功", Toast.LENGTH_SHORT).show();
				}else
				{
					Toast.makeText(MainActivity.this, (msg.arg2-msg.arg1)+"个文件复制失败", Toast.LENGTH_SHORT).show();
				}
				updateDatas(mTopBarText);
				mListTask.remove(0);
				break;
			case MSG_FILE_ATTRIBUTE:
				FileHelper.showFileAttribute((List<String>) msg.obj,MainActivity.this);
				mListTask.remove(0);
				break;
			case MSG_FILE_NEW:
				updateDatas(mTopBarText);
				break;
			case MSG_FILE_GETBITMAP:
				((ExternalStorageAdapter)msg.obj).notifyDataSetChanged();
				if(msg.arg1==msg.arg2-1)
				{
					
					mListTask.remove(0);
				}
				break;
			case MSG_CHANGEDSTATEANDVIEW:
				changedStateAndView((String) msg.obj);
				break;
			case MSG_FILE_SEARCHFINISHED:
				searchFinished((List<String>) msg.obj);
				mListTask.remove(0);
				break;
			case MSG_EXECUTETHREAD:
				sExecutorService.execute((Thread) msg.obj);
				mTopBarProgressBar.setVisibility(View.VISIBLE);
				mListTask.add((Thread) msg.obj);
				break;
			case MSG_CATEGORYBROWSER_UPDATE_DATAS:
				if(msg.arg1==0)
				{
					mFileCategoryBrowser.initDatas();
				}
				mListTask.remove(0);
				break;
			}
			if (mListTask.size() == 0) {
				mTopBarProgressBar.setVisibility(View.GONE);
			}
		}
	};
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		mAllTabs = new ArrayList<TabHost.TabSpec>();
		mExternalStorageHelper = new ExternalStorageHelper(MainActivity.this,
				mHandler);
		initViews();
		mTopBarText = PATH_EXTERNAL_STORAGE;
		mIsError = getStorageState(this);
		if (mIsError) {	
			initCategoryFile();
			updateDatas(PATH_EXTERNAL_STORAGE);
			registerListener();
		}
	}

	//初始化视图
	private void initViews() {
		mFileCategoryListView = (ListView) findViewById(R.id.id_mainCategoryBrowserListView);
		mTopBar = (RelativeLayout) findViewById(R.id.id_mainTopBar);
		mTopBarImageView = (ImageView) findViewById(R.id.id_mainTopBarImageView);
		mTopBarTextView = (TextView) findViewById(R.id.id_mainTopBarTextView);
		mTopBarProgressBar = (ProgressBar) findViewById(R.id.id_mainTopBarProgressBar);
		mBottomBar = (LinearLayout) findViewById(R.id.id_mainBottomBar);
		mBottomBar1 = (LinearLayout) findViewById(R.id.id_mainBottomBar1);
		mBottomBarCopyImageButton = (ImageButton) findViewById(R.id.id_mainBottomBarCopyImageButton);
		mBottomBarCutImageButton = (ImageButton) findViewById(R.id.id_mainBottomBarCutImageButton);
		mBottomBarDeleteImageButton = (ImageButton) findViewById(R.id.id_mainBottomBarDeleteImageButton);
		mBottomBarMoreImageButton = (ImageButton) findViewById(R.id.id_mainBottomBarMoreImageButton);
		mBottomBar2 = (LinearLayout) findViewById(R.id.id_mainBottomBar2);
		mBottomBar2PasteButton = (Button) findViewById(R.id.id_mainBottomBarPasteButton);
		mBottomBar2CancelButton = (Button) findViewById(R.id.id_mainBottomBarCancelButton);
		mTabHost = (TabHost) findViewById(R.id.id_mainTabHost);
		mExternalStorageListView = (ListView) findViewById(R.id.id_mainListView);
		mTabHost.setup();
		TabSpec categoryBrowser = mTabHost.newTabSpec(TABSPEC_CATEGORYBROWSER);
		categoryBrowser.setContent(R.id.id_mainCategoryBrowser);
		categoryBrowser.setIndicator(TABSPEC_CATEGORYBROWSER);

		TabSpec externalStorage = mTabHost.newTabSpec(TABSPEC_EXTERNAL_STORAGE);
		externalStorage.setContent(R.id.id_mainStore);
		externalStorage.setIndicator(TABSPEC_EXTERNAL_STORAGE);

		mAllTabs.add(categoryBrowser);
		mAllTabs.add(externalStorage);
		mTabHost.addTab(categoryBrowser);
		mTabHost.addTab(externalStorage);
		mTabHost.setCurrentTab(1);
	}
	
	//获取存储状态
	public boolean getStorageState(Context context) {
		boolean isOK=false;
		List<String> ResultPathLists=ExternalStorageHelper.getExternalStorageDirectory(this);
		for(String path:ResultPathLists)
		{
			StorageManager storageManager = (StorageManager) context
	                .getSystemService(Context.STORAGE_SERVICE);
	        try {
	            Method getVolumeState = storageManager.getClass().getMethod(
	                    "getVolumeState", String.class);
	            String state = (String) getVolumeState.invoke(storageManager,
	            		path);
	            if(Environment.MEDIA_MOUNTED.equals(state)||Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
	            {
	            	isOK=true;
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		}
		return isOK;
	}
	
	public void initCategoryFile()
	{
		mFileCategoryBrowser = new FileCategoryBrowser(
				MainActivity.this, mHandler,
				mFileCategoryListView);
	}
	
	//注册监听
	private void registerListener() {
		mBottomBarCopyImageButton.setOnClickListener(this);
		mBottomBarCutImageButton.setOnClickListener(this);
		mBottomBarDeleteImageButton.setOnClickListener(this);
		mBottomBarMoreImageButton.setOnClickListener(this);
		mBottomBar2PasteButton.setOnClickListener(this);
		mBottomBar2CancelButton.setOnClickListener(this);
		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				if (!mIsRefactorTab) {
					if (tabId.equals(TABSPEC_EXTERNAL_STORAGE)) {
						mTopBarTextView.setText(mTopBarText);
						updateDatas(mTopBarText);
					} else if (tabId.equals(TABSPEC_CATEGORYBROWSER)) {
						if(sStateCur!=STATE_NORMAL)
						{
							changedStateAndView(STATE_NORMAL);
						}
						mTopBarTextView.setText(TABSPEC_CATEGORYBROWSER);
					} else if (tabId.equals(TABSPEC_SEARCHRESULT)) {
						if(sStateCur==STATE_SELECTED)
						{
							changedStateAndView(STATE_NORMAL);
						}
						mTopBarTextView.setText(TABSPEC_SEARCHRESULT);
					}
				}
			}
		});

		mExternalStorageListView
				.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						if (sStateCur.equals(STATE_SELECTED)) {
							if (mDataList.get(position).getIsSeleced()) {
								mDataList.get(position).setSeleced(false);
							} else {
								mDataList.get(position).setSeleced(true);
							}
							Message message = null;
							if (mExternalStorageAdapter.getSelectedCount() == 0) {
								message = mHandler.obtainMessage(
										MSG_CHANGEDSTATEANDVIEW, STATE_NORMAL);
							} else {
								message = mHandler
										.obtainMessage(MSG_CHANGEDSTATEANDVIEW,
												STATE_SELECTED);
							}
							mHandler.sendMessage(message);
						} else {
							mPosition = position;
							if (new File(mDataList.get(position).getContent())
									.isDirectory()) {
								updateDatas(mDataList.get(position)
										.getContent());
							} else {
								if (!FileHelper.fileOpenWay(
										mDataList.get(position).getContent(),
										MainActivity.this)) {
									mStyleContextMenu = STYLE_CONTEXTMENU_ONITEMCLICK;
									registerForContextMenu(mExternalStorageListView);
									mExternalStorageListView.showContextMenu();
								}
							}
						}
					}
				});
		mExternalStorageListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						mPosition = position;
						mStyleContextMenu = STYLE_CONTEXTMENU_ONITEMLONGCLICK;
						registerForContextMenu(mExternalStorageListView);
						return false;
					}
				});
	}
	
	//更新数据
	private void updateDatas(final String path) {
		if(path.equals(PATH_EXTERNAL_STORAGE))
		{
			mResultPathLists=ExternalStorageHelper.getExternalStorageDirectory(this);
			mListTask.add(null);
			Message msg = mHandler.obtainMessage(
					MainActivity.MSG_FILE_UPDATE, mResultPathLists);
			mHandler.sendMessage(msg);
		}
		else
		{
			Thread updateDatasThread = new Thread() {
				public void run() {
					final List<String> ResultPathLists = FileHelper
							.getFilePathLists(path, false);
					Message msg = mHandler.obtainMessage(
							MainActivity.MSG_FILE_UPDATE, ResultPathLists);
					mHandler.sendMessage(msg);
				}
			};
			Message msg = mHandler.obtainMessage(MainActivity.MSG_EXECUTETHREAD,
					updateDatasThread);
			mHandler.sendMessage(msg);
		}
		mTopBarText = path;
	}

	//创建选项菜单
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		if (sStateCur.equals(STATE_SELECTED)) {
			getMenuInflater().inflate(R.menu.main_menu_selected, menu);
		} else {
			getMenuInflater().inflate(R.menu.main_menu_normal, menu);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (sStateCur.equals(STATE_SELECTED)) {
			onOptionsItemSelectedOnStateSelected(id);
		} else {
			onOptionsItemSelectedOnStateNormal(id);
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		if (mStyleContextMenu.equals(STYLE_CONTEXTMENU_ONITEMCLICK)) {
			menu.setHeaderTitle("选择文件类型");
			menu.add(0, 1, 0, "文本");
			menu.add(0, 2, 0, "音频");
			menu.add(0, 3, 0, "视频");
			menu.add(0, 4, 0, "图像");
		} else if (mStyleContextMenu.equals(STYLE_CONTEXTMENU_ONITEMLONGCLICK)) {
			menu.setHeaderTitle("选项：");
			menu.add(0, 5, 0, "打开方式");
			menu.add(0, 6, 0, "删除");
			menu.add(0, 7, 0, "重命名");
			menu.add(0, 8, 0, "复制");
			menu.add(0, 9, 0, "移动");
			menu.add(0, 10, 0, "属性");
			menu.add(0, 11, 0, "分享/发送");
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (mStyleContextMenu.equals(STYLE_CONTEXTMENU_ONITEMCLICK)) {
			onContextItemSelectedOnOnClick(id);
		} else if (mStyleContextMenu.equals(STYLE_CONTEXTMENU_ONITEMLONGCLICK)) {
			onContextItemSelectedOnOnLongClick(id);
		}
		return true;
	}

	public boolean onOptionsItemSelectedOnStateSelected(int id) {
		final int position = mExternalStorageAdapter
				.getCurrentSelectedPosition();
		switch (id) {
		case R.id.action_chooseAll:
			chooseAll();
			changedStateAndView(STATE_SELECTED);
			return true;
		case R.id.action_chooseReverse:
			chooseReverse();
			return true;
		case R.id.action_cancel:
			changedStateAndView(STATE_NORMAL);
			return true;
		case R.id.action_openWay:
			if (position != -1) {
				String path = mDataList.get(position).getContent();
				mDataList.get(position).setSeleced(false);
				File file = new File(path);
				if (file.isDirectory()) {
					updateDatas(path);
				} else {
					if (!FileHelper.fileOpenWay(path, MainActivity.this)) {
						ExternalStorageHelper.initOpenWay(path,this);
					}
				}
			} else {
				Toast.makeText(MainActivity.this, "无法同时打开多个文件",
						Toast.LENGTH_SHORT).show();
			}
			Message msg = mHandler.obtainMessage(MSG_CHANGEDSTATEANDVIEW,
					STATE_NORMAL);
			mHandler.handleMessage(msg);
			return true;
		case R.id.action_reName:
			if (position != -1) {
				final String path = mDataList.get(position).getContent();
				mDataList.get(position).setSeleced(false);
				mExternalStorageHelper.initReName(path);
			} else {
				Toast.makeText(MainActivity.this, "无法同时重命名多个文件",
						Toast.LENGTH_SHORT).show();
			}
			Message msg1 = mHandler.obtainMessage(MSG_CHANGEDSTATEANDVIEW,
					STATE_NORMAL);
			mHandler.handleMessage(msg1);
			return true;
		case R.id.action_attribute:
			if (position != -1) {
				mDataList.get(position).setSeleced(false);
				Thread attributeThread = new Thread() {
					@Override
					public void run() {
						List<String> list = FileHelper
								.getFileAttribute(mDataList.get(position)
										.getContent());
						Message msg2 = mHandler.obtainMessage(
								MSG_FILE_ATTRIBUTE, list);
						mHandler.sendMessage(msg2);
					}
				};

				Message msg3 = mHandler.obtainMessage(MSG_EXECUTETHREAD,
						attributeThread);
				mHandler.sendMessage(msg3);
			} else {
				Toast.makeText(MainActivity.this, "无法同时获取多个文件的属性",
						Toast.LENGTH_SHORT).show();
			}
			Message msg4 = mHandler.obtainMessage(MSG_CHANGEDSTATEANDVIEW,
					MainActivity.STATE_NORMAL);
			mHandler.handleMessage(msg4);
			return true;
		case R.id.action_shareOrSend:
			List<String> files = new ArrayList<String>();
			for (int i = 0; i < mDataList.size(); i++) {
				boolean isSelected = mDataList.get(i).getIsSeleced();
				if (isSelected) {
					files.add(mDataList.get(i).getContent());
					mDataList.get(i).setSeleced(false);
				}
			}
			if (files != null) {
				FileHelper.shareOrSend(files, MainActivity.this);
			}
			Message msg5 = mHandler.obtainMessage(MSG_CHANGEDSTATEANDVIEW,
					MainActivity.STATE_NORMAL);
			mHandler.handleMessage(msg5);

			return true;
		default:
			return false;
		}
	}

	public boolean onOptionsItemSelectedOnStateNormal(int id) {
		if(mTabHost.getCurrentTabTag().equals(TABSPEC_CATEGORYBROWSER))
		{
			if(id==R.id.action_refresh)
			{
				initCategoryFile();
			}
			else if(id==R.id.action_homePage)
			{
				updateDatas(MainActivity.PATH_EXTERNAL_STORAGE);
			}
			else if(id==R.id.action_exit)
			{
				finish();
			}
			else
			{
				Toast.makeText(MainActivity.this, "无效的操作！",
						Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		else if(mTabHost.getCurrentTabTag().equals(TABSPEC_SEARCHRESULT))
		{
			if(id==R.id.action_closeTab)
			{
				mAllTabs.remove(mTabHost.getCurrentTab());
				refactorTabHost(mTabHost, mAllTabs);
				mTabHost.setCurrentTab(1);
			}
			else if(id==R.id.action_homePage)
			{
				updateDatas(MainActivity.PATH_EXTERNAL_STORAGE);
			}
			else if(id==R.id.action_exit)
			{
				finish();
			}
			else
			{
				Toast.makeText(MainActivity.this, "无效的操作！",
						Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		else
		{
			switch (id) {
			case R.id.action_refresh:
				updateDatas(mTopBarText);
				return true;
			case R.id.action_newFolder:
				mExternalStorageHelper.initNewFile(mTopBarText, false);
				return true;
			case R.id.action_newFile:
				mExternalStorageHelper.initNewFile(mTopBarText, true);
				return true;
			case R.id.action_searchFile:
				mExternalStorageHelper.initSearchFile(mTopBarText);
				return true;
			case R.id.action_closeTab:
				// 关闭
				if (!mTabHost.getCurrentTabTag().equals(
						MainActivity.TABSPEC_CATEGORYBROWSER)
						&& !mTabHost.getCurrentTabTag().equals(
								MainActivity.TABSPEC_EXTERNAL_STORAGE)) {
					mAllTabs.remove(mTabHost.getCurrentTab());
					refactorTabHost(mTabHost, mAllTabs);
					mTabHost.setCurrentTab(1);
				} else {
					Toast.makeText(MainActivity.this, "无法关闭当前选项卡",
							Toast.LENGTH_SHORT).show();
				}
				return true;
			case R.id.action_homePage:
				updateDatas(MainActivity.PATH_EXTERNAL_STORAGE);
				return true;
			case R.id.action_exit:
				finish();
				return true;
			default:
				return false;
			}
		}
	}

	public boolean onContextItemSelectedOnOnClick(int id) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		Uri uri = Uri.fromFile(new File(mDataList.get(mPosition).getContent()));
		switch (id) {
		case 1:
			intent.setDataAndType(uri, "text/*");
			startActivity(Intent.createChooser(intent, "打开方式"));
			break;
		case 2:
			intent.setDataAndType(uri, "audio/*");
			startActivity(Intent.createChooser(intent, "打开方式"));
			break;
		case 3:
			intent.setDataAndType(uri, "video/*");
			startActivity(Intent.createChooser(intent, "打开方式"));
			break;
		case 4:
			intent.setDataAndType(uri, "image/*");
			startActivity(Intent.createChooser(intent, "打开方式"));
			break;
		}
		return true;
	}

	public boolean onContextItemSelectedOnOnLongClick(int id) {
		switch (id) {
		case 5:
			// 打开方式
			ExternalStorageHelper.initOpenWay(mDataList.get(mPosition).getContent(),this);
			break;
		case 6:
			// 删除
			final List<String> fileList = new ArrayList<String>();
			fileList.add(mDataList.get(mPosition).getContent());
			mExternalStorageHelper.initDeleteFile(fileList);
			break;
		case 7:
			// 重命名
			final String path = mDataList.get(mPosition).getContent();
			mExternalStorageHelper.initReName(path);
			break;
		case 8:
			// 复制
			mCopyFileList = new ArrayList<String>();
			String copyFile = mDataList.get(mPosition).getContent();
			mCopyFileList.add(copyFile);
			mIsCopy = true;
			changedStateAndView(STATE_OPERTION);
			break;
		case 9:
			// 移动
			mCopyFileList = new ArrayList<String>();
			String cutFile = mDataList.get(mPosition).getContent();
			mCopyFileList.add(cutFile);
			mIsCopy = false;
			changedStateAndView(STATE_OPERTION);
			break;
		case 10:
			// 属性
			final String filePath = mDataList.get(mPosition).getContent();
			Thread attributeThread = new Thread() {
				public void run() {
					List<String> resultList = FileHelper
							.getFileAttribute(filePath);
					Message msg = mHandler.obtainMessage(MSG_FILE_ATTRIBUTE,
							resultList);
					mHandler.sendMessage(msg);
				};
			};
			Message msg1 = mHandler.obtainMessage(MSG_EXECUTETHREAD,
					attributeThread);
			mHandler.sendMessage(msg1);
			break;
		case 11:
			// 分享/发送
			List<String> filePaths = new ArrayList<String>();
			filePaths.add(mDataList.get(mPosition).getContent());
			FileHelper.shareOrSend(filePaths, this);
			break;
		}
		return true;
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
		case R.id.id_mainBottomBarCopyImageButton:
			mCopyFileList = ExternalStorageHelper.initCopyFile(mDataList);
			mIsCopy = true;
			mExternalStorageAdapter.notifyDataSetChanged();
			changedStateAndView(STATE_OPERTION);
			break;
		case R.id.id_mainBottomBarCutImageButton:
			mCopyFileList = ExternalStorageHelper.initCopyFile(mDataList);
			mIsCopy = false;
			mExternalStorageAdapter.notifyDataSetChanged();
			changedStateAndView(STATE_OPERTION);
			break;
		case R.id.id_mainBottomBarDeleteImageButton:
			mCopyFileList = ExternalStorageHelper.initCopyFile(mDataList);
			mExternalStorageHelper.initDeleteFile(mCopyFileList);
			changedStateAndView(STATE_NORMAL);
			break;
		case R.id.id_mainBottomBarMoreImageButton:
			openOptionsMenu();
			break;
		case R.id.id_mainBottomBarPasteButton:
			Thread pasteThread = new Thread() {
				@Override
				public void run() {
					int size=mCopyFileList.size();
					List<String> list=FileHelper.copyFile(mCopyFileList, mTopBarText, mIsCopy);
					Message msg = mHandler.obtainMessage(MSG_FILE_PASTE, list.size(),size,null);
					mHandler.sendMessage(msg);
				}
			};
			Message msg1 = mHandler.obtainMessage(
					MainActivity.MSG_EXECUTETHREAD, pasteThread);
			mHandler.sendMessage(msg1);
			changedStateAndView(STATE_NORMAL);
			break;
		case R.id.id_mainBottomBarCancelButton:
			changedStateAndView(STATE_NORMAL);
			break;
		}
	}

	private void searchFinished(List<String> obj) {
		List<CommonBean> dataList = new ArrayList<CommonBean>();
		for (String filePath : obj) {
			File file = new File(filePath);
			dataList.add(new CommonBean(FileCategoryHelper.getFileBitmap(
					MainActivity.this, file.getAbsolutePath(), true), file
					.getName(), file.getAbsolutePath(), false));
		}
		showSearchResult(dataList, TABSPEC_SEARCHRESULT);
	};

	private void showSearchResult(final List<CommonBean> dataList, String tag) {
		final ListView listView = new ListView(getApplicationContext());
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String path = dataList.get(position).getContent();
				File file = new File(path);
				if (file.isDirectory()) {
					updateDatas(path);
				} else {
					if (!FileHelper.fileOpenWay(path, MainActivity.this)) {
						mStyleContextMenu = STYLE_CONTEXTMENU_ONITEMCLICK;
						registerForContextMenu(listView);
						listView.showContextMenu();
					}
				}
			}
		});
		ExternalStorageAdapter externalStorageAdapter = new ExternalStorageAdapter(
				MainActivity.this, mHandler, dataList,
				R.layout.item_listview_common1,listView);
		listView.setAdapter(externalStorageAdapter);
		externalStorageAdapter.notifyDataSetChanged();
		for (TabSpec tab : mAllTabs) {
			if (tab.getTag().equals(tag)) {
				mAllTabs.remove(tab);
				refactorTabHost(mTabHost, mAllTabs);
				break;
			}
		}
		addTabSpec(mTabHost, listView, tag);
		mTabHost.setCurrentTabByTag(tag);
	}

	private void updateDatasFinished(final List<String> obj) {
		mDataList = new ArrayList<CommonBean>();
		for (String filePath : obj) {
			File file = new File(filePath);
			mDataList.add(new CommonBean(FileCategoryHelper.getFileBitmap(
					MainActivity.this, file.getAbsolutePath(), true), file
					.getName(), file.getAbsolutePath(), false));
		}
		mTopBarTextView.setText(mTopBarText);
		mExternalStorageAdapter = new ExternalStorageAdapter(MainActivity.this,
				mHandler, mDataList, R.layout.item_listview_common,mExternalStorageListView);
		mExternalStorageListView.setAdapter(mExternalStorageAdapter);
		mExternalStorageAdapter.notifyDataSetChanged();
	}
	
	private void chooseAll() {
		for (int i = 0; i < mDataList.size(); i++) {
			mDataList.get(i).setSeleced(true);
		}
	}

	private void chooseReverse() {
		int selectedCount = 0;
		for (int i = 0; i < mDataList.size(); i++) {
			if (mDataList.get(i).getIsSeleced()) {
				mDataList.get(i).setSeleced(false);
			} else {
				mDataList.get(i).setSeleced(true);
				selectedCount++;
			}
		}
		if (selectedCount == 0) {
			changedStateAndView(STATE_NORMAL);
		} else {
			changedStateAndView(STATE_SELECTED);
		}
	}

	public void changedStateAndView(String state) {
		sStateCur = state;
		if (sStateCur.equals(STATE_SELECTED)) {
			mBottomBar2.setVisibility(View.GONE);
			mBottomBar1.setVisibility(View.VISIBLE);
			mTopBar.setBackgroundResource(R.color.selectedBackgroundColor);
			mTopBarTextView.setText("已选择"
					+ mExternalStorageAdapter.getSelectedCount() + "项");
			mBottomBarCopyImageButton.setVisibility(View.VISIBLE);
			mBottomBarCutImageButton.setVisibility(View.VISIBLE);
			mBottomBarDeleteImageButton.setVisibility(View.VISIBLE);
		} else if (sStateCur.equals(STATE_OPERTION)) {
			mTopBar.setBackgroundResource(R.color.normalTopBarBackgroundColor);
			mBottomBar1.setBackgroundResource(R.color.normalBackgroundColor);
			mBottomBar1.setVisibility(View.GONE);
			mBottomBar2.setVisibility(View.VISIBLE);
			mTopBarTextView.setText(mTopBarText);
		} else {
			mTopBar.setBackgroundResource(R.color.normalTopBarBackgroundColor);
			mBottomBar1.setBackgroundResource(R.color.normalBackgroundColor);
			mBottomBar2.setVisibility(View.GONE);
			mBottomBar.setVisibility(View.VISIBLE);
			mBottomBar1.setVisibility(View.VISIBLE);
			mTopBarTextView.setText(mTopBarText);
			for (int i = 0; i < mDataList.size(); i++) {
				mDataList.get(i).setSeleced(false);
			}
			mBottomBarCopyImageButton.setVisibility(View.INVISIBLE);
			mBottomBarCutImageButton.setVisibility(View.INVISIBLE);
			mBottomBarDeleteImageButton.setVisibility(View.INVISIBLE);
		}
		mExternalStorageAdapter.notifyDataSetChanged();
	}

	public void addTabSpec(TabHost tabHost, final View view, String tag) {
		TabSpec tab = tabHost.newTabSpec(tag);
		tab.setContent(new TabHost.TabContentFactory() {
			@Override
			public View createTabContent(String tag) {
				return view;
			}
		});
		tab.setIndicator(tag);
		mAllTabs.add(tab);
		tabHost.addTab(tab);
	}

	public void refactorTabHost(TabHost tabHost, List<TabSpec> tabs) {
		mIsRefactorTab = true;
		tabHost.setCurrentTab(1);
		tabHost.clearAllTabs();
		for (TabSpec tab : tabs) {
			tabHost.addTab(tab);
		}
		tabHost.setCurrentTab(1);
		mIsRefactorTab = false;
		changedStateAndView(STATE_NORMAL);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {// MENU键
			if (!mIsError)
			{
				mBottomBarMoreImageButton.setEnabled(false);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		if (sStateCur.equals(STATE_SELECTED)) {
			changedStateAndView(STATE_NORMAL);
		} else {
			if (mTopBarText.equals(PATH_EXTERNAL_STORAGE)
					|| mTabHost.getCurrentTabTag().equals(TABSPEC_CATEGORYBROWSER)||mTabHost.getCurrentTabTag().equals(TABSPEC_SEARCHRESULT)) {
				super.onBackPressed();
			} else {
				for(int i=0;i<mResultPathLists.size();i++)
				{
					if(new File(mTopBarText).getAbsolutePath().equals(mResultPathLists.get(i)))
					{
						updateDatas(PATH_EXTERNAL_STORAGE);
						return;
					}
				}
				updateDatas(new File(mTopBarText).getParent());
			}
		}
	}

}
