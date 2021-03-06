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

package us.nineworlds.serenity.core.imageloader;

import android.app.Activity;
import android.view.View;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import us.nineworlds.serenity.BuildConfig;
import us.nineworlds.serenity.R;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SerenityBackgroundLoadListenerTest {

	View backgroundView;
	Activity activity;

	@Before
	public void setUp() {
		backgroundView = Mockito.mock(View.class);
		activity = Mockito.mock(Activity.class);
		doNothing().when(activity).runOnUiThread(
				any(BackgroundBitmapDisplayer.class));
		when(backgroundView.getContext()).thenReturn(activity);
	}

	@After
	public void tearDown() {
		if (activity != null) {
			activity.finish();
		}
	}

	@Test
	public void assertThatBackgroundBitmapDisplayerIsRunOnUIThread() {
		SerenityBackgroundLoaderListener listener = new SerenityBackgroundLoaderListener(
				backgroundView, R.drawable.movies, activity);
		listener.onLoadingComplete(null, backgroundView, null);
		verify(activity).runOnUiThread(any(BackgroundBitmapDisplayer.class));
	}
}
