/**
 * The MIT License (MIT)
 * Copyright (c) 2012 David Carver
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package us.nineworlds.serenity.ui.browser.tv;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import net.ganin.darv.DpadAwareRecyclerView;
import us.nineworlds.plex.rest.PlexappFactory;
import us.nineworlds.serenity.R;
import us.nineworlds.serenity.core.menus.MenuDrawerItem;
import us.nineworlds.serenity.core.menus.MenuDrawerItemImpl;
import us.nineworlds.serenity.core.model.SeriesContentInfo;
import us.nineworlds.serenity.recyclerutils.ItemOffsetDecoration;
import us.nineworlds.serenity.recyclerutils.SpaceItemDecoration;
import us.nineworlds.serenity.ui.activity.SerenityMultiViewVideoActivity;
import us.nineworlds.serenity.ui.adapters.AbstractPosterImageGalleryAdapter;
import us.nineworlds.serenity.ui.adapters.MenuDrawerAdapter;
import us.nineworlds.serenity.ui.util.DisplayUtils;
import us.nineworlds.serenity.volley.DefaultLoggingVolleyErrorListener;
import us.nineworlds.serenity.volley.TVCategoryResponseListener;
import us.nineworlds.serenity.volley.VolleyUtils;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.view.KeyEvent;
import android.view.View;
import us.nineworlds.serenity.widgets.SerenityMenuGridLayoutManager;

public class TVShowBrowserActivity extends SerenityMultiViewVideoActivity {

	private boolean restarted_state = false;
	private static String key;
	private RecyclerView.Adapter adapter;

	@Inject
	protected SharedPreferences preferences;

	@Inject
	protected TVCategoryState categoryState;

	@Inject
	protected VolleyUtils volleyUtils;

	@Inject
	protected PlexappFactory factory;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		actionBar.setCustomView(R.layout.tvshow_custom_actionbar);
		actionBar.setDisplayShowCustomEnabled(true);

		key = getIntent().getExtras().getString("key");

		createSideMenu();

		DisplayUtils.overscanCompensation(this, getWindow().getDecorView());
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		restarted_state = true;
		populateMenuDrawer();
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateMenuDrawer();
		if (!restarted_state) {
			String url = factory.getSectionsUrl(key);
			TVCategoryResponseListener response = new TVCategoryResponseListener(
					this, key);
			volleyUtils.volleyXmlGetRequest(url, response,
					new DefaultLoggingVolleyErrorListener());
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		DpadAwareRecyclerView gallery = findGalleryView();

		boolean menuKeySlidingMenu = preferences.getBoolean(
				"remote_control_menu", true);
		if (menuKeySlidingMenu) {
			if (keyCode == KeyEvent.KEYCODE_MENU) {
				if (drawerLayout.isDrawerOpen(linearDrawerLayout)) {
					drawerLayout.closeDrawers();
				} else {
					drawerLayout.openDrawer(linearDrawerLayout);
				}
				return true;
			}
		}

		if (keyCode == KeyEvent.KEYCODE_BACK
				&& drawerLayout.isDrawerOpen(linearDrawerLayout)) {
			drawerLayout.closeDrawers();
			if (gallery != null) {
				gallery.requestFocusFromTouch();
			}
			return true;
		}

		if (gallery == null) {
			return super.onKeyDown(keyCode, event);
		}

		AbstractPosterImageGalleryAdapter adapter = (AbstractPosterImageGalleryAdapter) gallery
				.getAdapter();
		if (adapter != null) {
			int itemsCount = adapter.getItemCount();

			if (contextMenuRequested(keyCode)) {
				int pos = gallery.getSelectedItemPosition();
				RecyclerView.LayoutManager layoutManager = gallery.getLayoutManager();
				View view = layoutManager.findViewByPosition(pos);
				view.performLongClick();
				return true;
			}
			if (isKeyCodeSkipBack(keyCode)) {
				int selectedItem = gallery.getSelectedItemPosition();
				int newPosition = selectedItem - 10;
				if (newPosition < 0) {
					newPosition = 0;
				}
				gallery.setSelection(newPosition);
				gallery.requestFocusFromTouch();
				return true;
			}
			if (isKeyCodeSkipForward(keyCode)) {
				int selectedItem = gallery.getSelectedItemPosition();
				int newPosition = selectedItem + 10;
				if (newPosition > itemsCount) {
					newPosition = itemsCount - 1;
				}
				gallery.setSelection(newPosition);
				gallery.requestFocusFromTouch();
				return true;
			}
			if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
					|| keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
				int selectedItem = gallery.getSelectedItemPosition();
				SeriesContentInfo info = (SeriesContentInfo) ((AbstractPosterImageGalleryAdapter) gallery.getAdapter()).getItem(selectedItem);
				new FindUnwatchedAsyncTask(this).execute(info);
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void createSideMenu() {

		posterLayoutActive = preferences.getBoolean("series_layout_posters",
				false);
		gridViewActive = preferences.getBoolean("series_layout_grid", false);
		if (isGridViewActive()) {
			setContentView(R.layout.activity_tvbrowser_show_gridview_posters);
		} else if (posterLayoutActive) {
			setContentView(R.layout.activity_tvbrowser_show_posters);
		} else {
			setContentView(R.layout.activity_tvbrowser_show_banners);
		}

		DpadAwareRecyclerView dpadAwareRecyclerView = findGalleryView() != null ? findGalleryView() : findGridView();
		if (!gridViewActive) {
			LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
			dpadAwareRecyclerView.setLayoutManager(linearLayoutManager);
			dpadAwareRecyclerView.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.horizontal_spacing)));
			dpadAwareRecyclerView.setOnItemSelectedListener(new TVShowGalleryOnItemSelectedListener(this));
		} else {
			SerenityMenuGridLayoutManager serenityMenuGridLayoutManager = new SerenityMenuGridLayoutManager(this, 3, SerenityMenuGridLayoutManager.HORIZONTAL, false);
			serenityMenuGridLayoutManager.setCircular(true);
			dpadAwareRecyclerView.setLayoutManager(serenityMenuGridLayoutManager);
			dpadAwareRecyclerView.addItemDecoration(new ItemOffsetDecoration(getResources().getDimensionPixelSize(R.dimen.grid_spacing_dimen)));
		}
		dpadAwareRecyclerView.setOnItemClickListener(new TVShowBrowserGalleryOnItemClickListener(this));


		View fanArt = findViewById(R.id.fanArt);
		fanArt.setBackgroundResource(R.drawable.tvshows);

		initMenuDrawerViews();

		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
				R.drawable.menudrawer_selector, R.string.drawer_open,
				R.string.drawer_closed) {
			@Override
			public void onDrawerOpened(View drawerView) {

				super.onDrawerOpened(drawerView);
				getSupportActionBar().setTitle(R.string.app_name);
				drawerList.requestFocusFromTouch();
			}

			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				getSupportActionBar().setTitle(R.string.app_name);
			}
		};

		drawerLayout.setDrawerListener(drawerToggle);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

		populateMenuDrawer();
	}

	protected void populateMenuDrawer() {
		List<MenuDrawerItem> drawerMenuItem = new ArrayList<MenuDrawerItem>();
		drawerMenuItem.add(new MenuDrawerItemImpl("Grid View",
				R.drawable.ic_action_collections_view_as_grid));
		drawerMenuItem.add(new MenuDrawerItemImpl("Detail View",
				R.drawable.ic_action_collections_view_detail));
		drawerMenuItem.add(new MenuDrawerItemImpl("Play All from Queue",
				R.drawable.menu_play_all_queue));

		drawerList.setAdapter(new MenuDrawerAdapter(this, drawerMenuItem));
		drawerList
		.setOnItemClickListener(new TVShowMenuDrawerOnItemClickedListener(
				drawerLayout));
	}

	@Override
	public AbstractPosterImageGalleryAdapter getAdapter() {
		return null;
	}

	@Override
	protected DpadAwareRecyclerView findGalleryView() {
		return (DpadAwareRecyclerView) findViewById(R.id.tvShowBannerGallery);
	}

	@Override
	protected DpadAwareRecyclerView findGridView() {
		return (DpadAwareRecyclerView) findViewById(R.id.tvShowGridView);
	}

	@Override
	protected void onPause() {
		super.onPause();

		DpadAwareRecyclerView galleryView = findGalleryView() != null ? findGalleryView() : findGridView();
		if (galleryView != null) {
			adapter = galleryView.getAdapter();
		}
	}
}
