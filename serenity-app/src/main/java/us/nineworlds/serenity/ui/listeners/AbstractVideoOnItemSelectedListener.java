/**
 * The MIT License (MIT)
 * Copyright (c) 2013 David Carver
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

package us.nineworlds.serenity.ui.listeners;

import javax.inject.Inject;

import net.ganin.darv.DpadAwareRecyclerView;
import us.nineworlds.plex.rest.PlexappFactory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.serenity.R;
import us.nineworlds.serenity.core.TrailersYouTubeSearch;
import us.nineworlds.serenity.core.imageloader.SerenityBackgroundLoaderListener;
import us.nineworlds.serenity.core.imageloader.SerenityImageLoader;
import us.nineworlds.serenity.core.model.DBMetaData;
import us.nineworlds.serenity.core.model.VideoContentInfo;
import us.nineworlds.serenity.core.util.DBMetaDataSource;
import us.nineworlds.serenity.injection.BaseInjector;
import us.nineworlds.serenity.ui.util.ImageInfographicUtils;
import us.nineworlds.serenity.ui.util.ImageUtils;
import us.nineworlds.serenity.volley.DefaultLoggingVolleyErrorListener;
import us.nineworlds.serenity.volley.SimpleXmlRequest;
import us.nineworlds.serenity.volley.SubtitleVolleyResponseListener;
import us.nineworlds.serenity.volley.VolleyUtils;
import us.nineworlds.serenity.volley.YouTubeTrailerSearchResponseListener;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;

/**
 * Abstract class for handling video selection information. This can either be a
 * movie or a tv episode. This is primarily used in a detail view browsing
 * scenario.
 *
 * @author dcarver
 *
 */
public abstract class AbstractVideoOnItemSelectedListener extends BaseInjector
		implements DpadAwareRecyclerView.OnItemSelectedListener {
	@Inject
	protected SerenityImageLoader serenityImageLoader;

	@Inject
	protected PlexappFactory plexFactory;

	@Inject
	protected VolleyUtils volley;

	@Inject
	protected SharedPreferences preferences;

	public static final String CRLF = "\r\n";
	public static final int WATCHED_VIEW_ID = 1000;
	public static final float WATCHED_PERCENT = 0.98f;
	protected Activity context;
	protected Handler checkDatabaseHandler = new Handler();
	private Animation fadeIn;
	private View previous;
	protected View currentView;
	protected int position;
	protected BaseAdapter adapter;
	protected VideoContentInfo videoInfo;
	private DBMetaDataSource datasource;
	protected RequestQueue queue;
	protected Runnable checkDBRunnable;

	protected ImageLoader imageLoader;

	public AbstractVideoOnItemSelectedListener() {
		super();
		imageLoader = serenityImageLoader.getImageLoader();
	}

	protected abstract void createVideoDetail(ImageView v);

	protected void createVideoMetaData(ImageView v) {
		fetchSubtitle(videoInfo);
	}

	/**
	 * Create the images representing info such as sound, ratings, etc based on
	 * the currently selected movie poster.
	 *
	 */
	protected void createInfographicDetails(ImageView v) {

		LinearLayout infographicsView = (LinearLayout) context
				.findViewById(R.id.movieInfoGraphicLayout);
		infographicsView.removeAllViews();

		ImageInfographicUtils imageUtilsNormal = new ImageInfographicUtils(80,
				48);
		ImageInfographicUtils imageUtilsAudioChannel = new ImageInfographicUtils(
				90, 48);

		TextView durationView = imageUtilsNormal.createDurationView(
				videoInfo.getDuration(), context);
		if (durationView != null) {
			infographicsView.addView(durationView);
		}

		ImageView resv = imageUtilsNormal.createVideoCodec(
				videoInfo.getVideoCodec(), v.getContext());
		if (resv != null) {
			infographicsView.addView(resv);
		}

		ImageView resolution = imageUtilsNormal.createVideoResolutionImage(
				videoInfo.getVideoResolution(), v.getContext());
		if (resolution != null) {
			infographicsView.addView(resolution);
		}

		ImageView aspectv = imageUtilsNormal.createAspectRatioImage(
				videoInfo.getAspectRatio(), context);
		if (aspectv != null) {
			infographicsView.addView(aspectv);
		}

		ImageView acv = imageUtilsNormal.createAudioCodecImage(
				videoInfo.getAudioCodec(), context);
		if (acv != null) {
			infographicsView.addView(acv);
		}

		ImageView achannelsv = imageUtilsAudioChannel.createAudioChannlesImage(
				videoInfo.getAudioChannels(), v.getContext());
		if (achannelsv != null) {
			infographicsView.addView(achannelsv);
		}

		if (videoInfo.getRating() != 0) {
			RatingBar ratingBar = new RatingBar(context, null,
					android.R.attr.ratingBarStyleIndicator);
			ratingBar.setMax(4);
			ratingBar.setIsIndicator(true);
			ratingBar.setStepSize(0.1f);
			ratingBar.setNumStars(4);
			ratingBar.setPadding(0, 0, 0, 0);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
			params.rightMargin = 15;
			ratingBar.setLayoutParams(params);

			double rating = videoInfo.getRating();
			ratingBar.setRating((float) (rating / 2.5));
			infographicsView.addView(ratingBar);
		}

		ImageView studiov = imageUtilsNormal.createStudioImage(
				videoInfo.getStudio(), context,
				videoInfo.getMediaTagIdentifier());
		if (studiov != null) {
			infographicsView.addView(studiov);
		}

		if (checkDBRunnable != null) {
			checkDatabaseHandler.removeCallbacks(checkDBRunnable);
		}
		checkDBRunnable = new CheckDatabaseRunnable();
		checkDatabaseHandler.post(checkDBRunnable);

	}

	protected void checkDataBaseForTrailer(VideoContentInfo pi) {
		datasource = new DBMetaDataSource(context);
		datasource.open();
		DBMetaData metaData = datasource.findMetaDataByPlexId(pi.id());
		if (metaData != null) {
			pi.setTrailer(true);
			pi.setTrailerId(metaData.getYouTubeID());
		}
		datasource.close();
	}

	public void fetchSubtitle(VideoContentInfo mpi) {
		queue = volley.getRequestQueue();
		String url = plexFactory.getMovieMetadataURL("/library/metadata/"
				+ mpi.id());
		SimpleXmlRequest<MediaContainer> xmlRequest = new SimpleXmlRequest<MediaContainer>(
				Request.Method.GET, url, MediaContainer.class,
				new SubtitleVolleyResponseListener(mpi, context),
				new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e(getClass().getCanonicalName(),
								"Subtitle Retrieval failure: ", error);
					}
				});
		queue.add(xmlRequest);
	}

	public void fetchTrailer(VideoContentInfo mpi, View view) {
		checkDataBaseForTrailer(mpi);
		if (mpi.hasTrailer()) {
			if (videoInfo.hasTrailer()
					&& YouTubeInitializationResult.SUCCESS
					.equals(YouTubeApiServiceUtil
							.isYouTubeApiServiceAvailable(context))) {
				ImageView ytImage = new ImageView(context);
				ytImage.setImageResource(R.drawable.yt_social_icon_red_128px);
				ytImage.setScaleType(ScaleType.FIT_XY);
				int w = ImageUtils.getDPI(45, context);
				int h = ImageUtils.getDPI(24, context);
				ytImage.setLayoutParams(new LinearLayout.LayoutParams(w, h));
				LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) ytImage
						.getLayoutParams();
				p.leftMargin = 5;
				p.gravity = Gravity.CENTER_VERTICAL;
				LinearLayout infographicsView = (LinearLayout) context
						.findViewById(R.id.movieInfoGraphicLayout);
				infographicsView.addView(ytImage);
			}

			return;
		}

		TrailersYouTubeSearch trailerSearch = new TrailersYouTubeSearch();
		String queryURL = trailerSearch.queryURL(mpi);

		volley.volleyJSonGetRequest(queryURL,
				new YouTubeTrailerSearchResponseListener(view, mpi),
				new DefaultLoggingVolleyErrorListener());
	}

	public void changeBackgroundImage() {

		if (videoInfo.getBackgroundURL() == null) {
			return;
		}

		View fanArt = context.findViewById(R.id.fanArt);
		String transcodingURL = plexFactory.getImageURL(
				videoInfo.getBackgroundURL(), 1280, 720);

		imageLoader
		.loadImage(transcodingURL, new ImageSize(1280, 720),
				new SerenityBackgroundLoaderListener(fanArt,
						R.drawable.movies, context));
	}

	protected class CheckDatabaseRunnable implements Runnable {

		@Override
		public void run() {
			if (YouTubeInitializationResult.SUCCESS
					.equals(YouTubeApiServiceUtil
							.isYouTubeApiServiceAvailable(context))) {
				fetchTrailer(videoInfo, currentView);
			}
		}

	}
}
