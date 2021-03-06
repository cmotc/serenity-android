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

package us.nineworlds.serenity.core.model.impl;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.serenity.core.model.CategoryInfo;

public class TVCategoryMediaContainer extends AbstractMediaContainer {

	protected String key;
	protected final ArrayList<String> excludeCategories;
	protected List<CategoryInfo> categories = new ArrayList<CategoryInfo>();

	public TVCategoryMediaContainer(MediaContainer mc) {
		super(mc);
		excludeCategories = new ArrayList<String>();
		excludeCategories.add("search?type=2");
		excludeCategories.add("search?type=4");
		excludeCategories.add("folder");
	}

	public List<CategoryInfo> createCategories() {
		populateCategories();
		return categories;
	}

	protected void populateCategories() {
		List<Directory> dirs = mc.getDirectories();
		categories = new ArrayList<CategoryInfo>();
		for (Directory dir : dirs) {
			if (!excludeCategories.contains(dir.getKey())) {
				CategoryInfo category = new CategoryInfo();
				category.setCategory(dir.getKey());
				category.setCategoryDetail(dir.getTitle());
				if (dir.getSecondary() > 0) {
					category.setLevel(dir.getSecondary());
				}
				categories.add(category);
			}
		}
	}

}
