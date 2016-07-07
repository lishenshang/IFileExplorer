package com.app.main.ifileexplorer.utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.widget.EditText;
import android.widget.Toast;

import com.app.main.ifileexplorer.CommonActivity;
import com.app.main.ifileexplorer.FileCategoryBrowser;
import com.app.main.ifileexplorer.MainActivity;
import com.app.main.ifileexplorer.adapterhelper.CommonBean;

public class ExternalStorageHelper {

	Context mContext;
	Handler mHandler;

	public ExternalStorageHelper(Context context, Handler handler) {
		mContext = context;
		mHandler = handler;
	}

	public void initSearchFile(final String mCurrentPath) {
		final List<String> filePathLists = new ArrayList<String>();
		final EditText editTextView = new EditText(mContext);
		new AlertDialog.Builder(mContext).setTitle("搜索").setView(editTextView)
				.setNegativeButton("取消", null)
				.setNeutralButton("搜索", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String keyWord=editTextView.getText().toString();
						if(keyWord==""||keyWord.replaceAll(" ", "").toLowerCase()=="")
						{
							Toast.makeText(mContext, "请输入有效关键词",
									Toast.LENGTH_SHORT).show();
							return;
						}
						Thread searchFileThread = new Thread() {
							@Override
							public void run() {
								FileHelper.searchFile(filePathLists,
										mCurrentPath, keyWord);
								Message msg = mHandler.obtainMessage(
										MainActivity.MSG_FILE_SEARCHFINISHED,
										filePathLists);
								mHandler.sendMessage(msg);
							}
						};
						Message msg = mHandler.obtainMessage(
								MainActivity.MSG_EXECUTETHREAD,
								searchFileThread);
						mHandler.sendMessage(msg);
					}
				}).show();
	}	

	public void initDeleteFile(final List<String> fileList) {
		new AlertDialog.Builder(mContext).setTitle("删除？").setMessage("删除所选项目？")
				.setNegativeButton("否", null)
				.setNeutralButton("是", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Thread deleteThread = new Thread() {
							public void run() {
								List<String> List=FileHelper.deleteFile(fileList);
								Message msg = mHandler.obtainMessage(
										MainActivity.MSG_FILE_DELETE, List.size(),fileList.size(),null);
								mHandler.sendMessage(msg);
							};
						};
						Message msg = mHandler.obtainMessage(
								MainActivity.MSG_EXECUTETHREAD, deleteThread);
						mHandler.sendMessage(msg);
					}
				}).show();
	}

	public void initNewFile(final String parentPath, final boolean isFile) {
		final EditText editTextView = new EditText(mContext);
		new AlertDialog.Builder(mContext).setTitle("新文件名")
				.setView(editTextView).setNegativeButton("取消", null)
				.setNeutralButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String fileName=editTextView.getText().toString();
						if(fileName==""||fileName.replaceAll(" ", "").toLowerCase()=="")
						{
							Toast.makeText(mContext, "未输入有效文件名，无法创建文件",Toast.LENGTH_SHORT).show();
							return;
						}
						if (!FileHelper.newFile(parentPath, fileName, isFile)) {
							Toast.makeText(mContext, "创建文件失败",
									Toast.LENGTH_SHORT).show();
						} else {
							Message msg = mHandler.obtainMessage(
									MainActivity.MSG_FILE_NEW, null);
							mHandler.sendMessage(msg);
						}
					}
				}).show();
	}

	public static void initOpenWay(String filePath,Context context) {
		Intent intent = new Intent(context, CommonActivity.class);
		intent.putExtra("dataList", filePath);
		intent.putExtra("tag", MainActivity.TAG_OPENWAY);
		context.startActivity(intent);
	}

	public void initReName(final String path) {
		final EditText editTextView = new EditText(mContext);
		editTextView.setText(new File(path).getName());
		Builder dialog=new AlertDialog.Builder(mContext).setTitle("重命名").setView(editTextView).setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Field field=null;
						try {
							field=dialog.getClass().getSuperclass().getDeclaredField("mShowing");
							field.setAccessible(true);
							field.set(dialog, true);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						dialog.dismiss();
					}
				})
				.setNeutralButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Field field=null;
						try {
							field=dialog.getClass().getSuperclass().getDeclaredField("mShowing");
							field.setAccessible(true);
							field.set(dialog, false);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						String name=editTextView.getText().toString();
						if(name==""||name.replaceAll(" ", "").toLowerCase()==""||name.equals(new File(path).getName()))
						{
							Toast.makeText(mContext, "未输入新的文件名，重命名失败",
									Toast.LENGTH_SHORT).show();
						} else {
							try {
								field.set(dialog, true);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							final String newName = name.replaceAll(" ", "").toLowerCase();
							Thread reNameThread = new Thread() {
								public void run() {
									boolean isSuessed = FileHelper.reNameFile(
											path, newName);
									Message msg = mHandler.obtainMessage(
											MainActivity.MSG_FILE_RENAME,
											isSuessed);
									mHandler.sendMessage(msg);
								};
							};
							Message msg = mHandler.obtainMessage(
									MainActivity.MSG_EXECUTETHREAD,
									reNameThread);
							mHandler.sendMessage(msg);
						}
					}
				});
		dialog.show();
	}

	public static List<String> initCopyFile(List<CommonBean> dataList) {
		List<String> mCopyFileList = new ArrayList<String>();
		for (int i = 0; i < dataList.size(); i++) {
			if (dataList.get(i).getIsSeleced()) {
				mCopyFileList.add(dataList.get(i).getContent());
				dataList.get(i).setSeleced(false);
			}
		}
		return mCopyFileList;
	}

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static List<String> getExternalStorageDirectory(Context context) {
    List<String> devList=new ArrayList<String>();
    try {
    StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);  
        Class<?>[] paramClasses = {};
        Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths",paramClasses);
        getVolumePathsMethod.setAccessible(true);
        Object[] params = {};  
        Object invoke = getVolumePathsMethod.invoke(storageManager, params);  
        if(((String[])invoke).length > 1){
        	for (int i = 0; i < ((String[])invoke).length; i++) {
        		if(FileCategoryBrowser.getSDCardSize(((String[])invoke)[i],context)>0.00)
        		devList.add(((String[])invoke)[i]);
        }
        } else {
        	devList.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        }
    } catch (Exception e) {
    	devList.add(Environment.getExternalStorageDirectory().getAbsolutePath());
    }
    return devList;
    }
	
}
