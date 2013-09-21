package com.snot.bloatware;

import java.util.List;
import java.io.DataOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.snot.bloatware.loader.AppEntry;
import com.snot.bloatware.loader.SysAppListLoader;

public class SystemApplicationsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<AppEntry>> {

	private AppListAdapter mAdapter;
	private static final int LOADER_ID = 1;

	private int position;

	public SystemApplicationsFragment() {
	}

//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);

		ListView lv = getListView();
		registerForContextMenu(lv);

		mAdapter = new AppListAdapter(getActivity());
		//setEmptyText(getString(R.string.no_system_applications));
		setListAdapter(mAdapter);

		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
//		AppEntry mAppEntry = (AppEntry)mAdapter.getItem(position);
//		Toast.makeText(getActivity(), mAppEntry.toString(), Toast.LENGTH_SHORT).show();

// TODO: Store the position before opening the context menu.
// This is needed since menuitem we access in onContextItemSelected isn't populated with this value.
// This probably isnt a really nice solution
		this.position = position;
		getActivity().openContextMenu(list);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.setHeaderTitle(getString(R.string.header_title));
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.context_menu_system_applications, menu);
	}

	public boolean onContextItemSelected(MenuItem item)
	{
		AppEntry mAppEntry = (AppEntry)getListView().getItemAtPosition(this.position);
		switch(item.getItemId())
		{
			case R.id.mark_bloat:
				markAsBloat(mAppEntry);
				break;
			case R.id.uninstall:
			// TODO: confirm dialog
			// TODO: confirm ret val and... toast soemthing
				deleteSystemApp(mAppEntry.getApplicationInfo().sourceDir);
				break;
			case R.id.freeze:
				Toast.makeText(getActivity(), "freeze", Toast.LENGTH_SHORT).show();
				break;
			default:
		}
		return super.onContextItemSelected(item);
	}

	private void deleteSystemApp(String app)
	{
		final String MOUNT_RW = "mount -o remount,rw -t rfs /dev/stl5 /system; \n";
		final String MOUNT_RO = "mount -o remount,ro -t rfs /dev/stl5 /system; \n";
		final String RM_APP = "rm -rf " + app + "; \n";
		Process process;
		try
		{
			process = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(MOUNT_RW);
			Toast.makeText(getActivity(), RM_APP, Toast.LENGTH_SHORT).show();
			//os.writeBytes(RM_APP);
			os.writeBytes(MOUNT_RO);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private void markAsBloat(AppEntry appEntry)
	{
		String to = getActivity().getString(R.string.mark_bloat_email_to);
		String subject = getActivity().getString(R.string.mark_bloat_email_subject);
		String message = appEntry.getLabel() + "\n";
		message += appEntry.getApplicationInfo().packageName + "\n";
		message += appEntry.getApplicationInfo().sourceDir;

		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { to });
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, message);
		intent.setType("message/rfc822");
		startActivity(intent);
	}

	/**********************/
	/** LOADER CALLBACKS **/
	/**********************/
	@Override
	public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args) {
		return new SysAppListLoader(getActivity());
	}
	
	@Override
	public void onLoadFinished(Loader<List<AppEntry>> loader, List<AppEntry> data) {
		mAdapter.setData(data);

		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}
	
	@Override
	public void onLoaderReset(Loader<List<AppEntry>> loader) {
		mAdapter.setData(null);
	}


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
	inflater.inflate(R.menu.main, menu);
    }
}

