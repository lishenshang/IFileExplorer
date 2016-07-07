package com.app.main.ifileexplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.app.main.ifileexplorer.adapterhelper.CommonAdapter;
import com.app.main.ifileexplorer.adapterhelper.CommonBean;
import com.app.main.ifileexplorer.adapterhelper.CommonViewHolder;
import com.app.main.ifileexplorer.adapterhelper.ExternalStorageAdapter;
import com.app.main.ifileexplorer.utils.ExternalStorageHelper;
import com.app.main.ifileexplorer.utils.FileCategoryHelper;
import com.app.main.ifileexplorer.utils.FileHelper;
import com.app.main.ifileexplorer.utils.MimeTypeUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class CommonActivity extends Activity {

	private ListView mCommonListView;
	private LinearLayout mCommonLinearLayout;
	private List<CommonBean> mDataList;

	private String mTag = null;
	private String mFilePath;

	private int mPositionByList;
	private int mPositionByCategory;

	private CommonAdapter mExternalStorageAdapter;

	private Handler mHandler = new Handler() {
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MainActivity.MSG_FILE_GETBITMAP:
				mExternalStorageAdapter.notifyDataSetChanged();
				break;
			case MainActivity.MSG_EXECUTETHREAD:
				MainActivity.sExecutorService.execute((Thread) msg.obj);
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_common);
		initViews();
		initDatas();
		registerListener();
	}

	//初始化视图
	private void initViews() {
		mCommonListView = (ListView) findViewById(R.id.id_commonListView);
		mCommonLinearLayout = (LinearLayout) findViewById(R.id.id_commonLinearLayout);
	}

	public List<CommonBean> getApplicationInfo(boolean isAll) {
		List<CommonBean> dataList = new ArrayList<CommonBean>();
		PackageManager packageManager = this.getPackageManager();
		List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
		if (pinfo != null) {
			Bitmap bm = BitmapFactory.decodeResource(this.getResources(),
					R.drawable.apk);
			for (int i = 0; i < pinfo.size(); i++) {
				if (isAll) {
					Drawable appIcon = pinfo.get(i).applicationInfo
							.loadIcon(getPackageManager());
					String appLabel = pinfo.get(i).applicationInfo.loadLabel(
							getPackageManager()).toString();
					String appPackageName = pinfo.get(i).packageName;
					BitmapDrawable bd = (BitmapDrawable) appIcon;
					Bitmap bitmap = bd.getBitmap();
					dataList.add(new CommonBean(bitmap, appLabel,
							appPackageName));
				} else {
					String appLabel = pinfo.get(i).applicationInfo.loadLabel(
							getPackageManager()).toString();
					String appPackageName = pinfo.get(i).packageName;
					dataList.add(new CommonBean(bm, appLabel, appPackageName));
				}
			}
		}
		return dataList;
	}

	//初始化数据
	private void initDatas() {
		mDataList = new ArrayList<CommonBean>();
		if (getIntent().getExtras() != null) {
			mTag = getIntent().getExtras().getString("tag");
		}
		if (mTag != null) {
			if (mTag.equals(MainActivity.TAG_OPENWAY)) {
				mDataList = getApplicationInfo(false);
				Thread thread = new Thread() {
					@Override
					public void run() {
						List<CommonBean> list = getApplicationInfo(true);
						for (int i = 0; i < list.size(); i++) {
							mDataList.get(i).setIcon(
									list.get(i).getIcon(CommonActivity.this));
						}
						Message msg = mHandler.obtainMessage(MainActivity.MSG_FILE_GETBITMAP);
						mHandler.sendMessage(msg);
					}
				};
				MainActivity.sExecutorService.execute(thread);
				mFilePath = getIntent().getExtras().getString("dataList");
				mExternalStorageAdapter = new CommonAdapter<CommonBean>(this,mDataList, R.layout.item_listview_common1){
					@Override
					public void convert(CommonViewHolder commonViewHolder,
							CommonBean commonBean) {
						commonViewHolder.setImageBitmap(R.id.id_icon, commonBean.getIcon(mContext));
						commonViewHolder.setText(R.id.id_filename, commonBean.getTitle());
						commonViewHolder.setChecked(R.id.id_checked, commonBean.getIsSeleced());
					}
					
				};
			} else if (mTag.equals(MainActivity.TAG_CATEGORYBROWSER)) {
				mPositionByCategory = getIntent().getExtras()
						.getInt("dataList");
				List<String> dataList = FileCategoryBrowser
						.getSystemCategoryFile(mPositionByCategory);
				if (dataList.size() != 0) {
					String path = dataList.get(0);
					Bitmap bitmap = FileCategoryHelper.getFileBitmap(this,
							path, true);
					for (String filePath : dataList) {
						File file = new File(filePath);
						mDataList.add(new CommonBean(bitmap, file.getName(),
								file.getAbsolutePath(), false));
					}
					mCommonListView.setVisibility(View.VISIBLE);
					mCommonLinearLayout.setVisibility(View.GONE);
				} else {
					mCommonListView.setVisibility(View.GONE);
					mCommonLinearLayout.setVisibility(View.VISIBLE);
				}
				mExternalStorageAdapter = new ExternalStorageAdapter(this, mHandler,
						mDataList, R.layout.item_listview_common1,mCommonListView);
			}
			mCommonListView.setAdapter(mExternalStorageAdapter);
			mExternalStorageAdapter.notifyDataSetChanged();
		}
	}

	//更新Adapter和List的数据
	private void updateDatas(int position, String tag) {
		String name = mDataList.get(mPositionByList).getContent();
		if (tag.equals("ren")) {
			FileCategoryBrowser.getSystemCategoryFile(position).set(mPositionByList,
					name);
		} else if (tag.equals("del")) {
			FileCategoryBrowser.getSystemCategoryFile(position).remove(mPositionByList);
			FileCategoryBrowser.setTitle(position);
		}
	}

	//注册监听
	private void registerListener() {
		mCommonListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (mTag != null && mTag.equals(MainActivity.TAG_OPENWAY)) {
					try {
						String packageName = mDataList.get(position)
								.getContent();
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent = CommonActivity.this.getPackageManager()
								.getLaunchIntentForPackage(packageName);
						String type = MimeTypeUtils.getMimeType(mFilePath);
						if (type != null) {
							intent.setDataAndType(
									Uri.fromFile(new File(mFilePath)), type);
							startActivity(intent);
							CommonActivity.this.finish();
							return;
						}

					} catch (Exception e) {
						
					}
					Toast.makeText(CommonActivity.this, "打开文件失败",
							Toast.LENGTH_SHORT).show();
					CommonActivity.this.finish();
				} else if (mTag != null
						&& mTag.equals(MainActivity.TAG_CATEGORYBROWSER)) {
					FileHelper.fileOpenWay(mDataList.get(position).getContent(),
							CommonActivity.this);

				}
			}
		});
		mCommonListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						if (mTag.equals(MainActivity.TAG_CATEGORYBROWSER)) {
							mPositionByList = position;
							registerForContextMenu(mCommonListView);
						}
						return false;
					}
				});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("选项：");
		menu.add(0, 0, 0, "打开方式");
		menu.add(0, 1, 0, "删除");
		menu.add(0, 2, 0, "重命名");
		menu.add(0, 3, 0, "属性");
		menu.add(0, 4, 0, "分享/发送");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			ExternalStorageHelper.initOpenWay(mDataList.get(mPositionByList)
					.getContent(), CommonActivity.this);
			break;
		case 1:
			delFile(mDataList.get(mPositionByList).getContent());
			break;
		case 2:
			initRenameFile(mDataList.get(mPositionByList).getContent());
			break;
		case 3:
			List<String> fileInfoList = FileHelper.getFileAttribute(mDataList.get(
					mPositionByList).getContent());
			FileHelper.showFileAttribute(fileInfoList,CommonActivity.this);
			break;
		case 4:
			List<String> list = new ArrayList<String>();
			list.add(mDataList.get(mPositionByList).getContent());
			FileHelper.shareOrSend(list, this);
			break;
		}
		return true;
	}

	private void initRenameFile(final String content) {
		final EditText editTextView = new EditText(this);
		editTextView.setText(new File(content).getName());
		new AlertDialog.Builder(CommonActivity.this).setTitle("重命名")
				.setView(editTextView)
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setNeutralButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String newName = editTextView.getText()
								.toString();
						if (newName==""||newName.replaceAll(" ", "").toLowerCase()=="") {
							Toast.makeText(CommonActivity.this, "请输入新的文件名",
									Toast.LENGTH_SHORT).show();
						} else {
							//重命名
							FileHelper.reNameFile(content, newName);
							mDataList.get(mPositionByList).setContent(
									content.substring(0,
											content.lastIndexOf("/") + 1)
											+ newName);
							mDataList.get(mPositionByList).setTitle(newName);
							mExternalStorageAdapter.notifyDataSetChanged();
							dialog.dismiss();
							updateDatas(mPositionByCategory, "ren");
						}
					}
				}).show();
	}
	
	private void delFile(final String content) {
		new AlertDialog.Builder(this).setTitle("删除？").setMessage("删除所选项目？")
				.setNegativeButton("否", null)
				.setNeutralButton("是", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						File file = new File(content);
						if (file.exists()) {
							file.delete();
						}
						mDataList.remove(mPositionByList);
						mExternalStorageAdapter.notifyDataSetChanged();
						updateDatas(mPositionByCategory, "del");
					}
				}).show();
	}

}
