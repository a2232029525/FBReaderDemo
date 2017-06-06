/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;

import com.orhanobut.logger.Logger;

import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class SelectionBookmarkAction extends FBAndroidAction {

    SelectionBookmarkAction(FBReader baseApplication, FBReaderApp fbreader) {
        super(baseApplication, fbreader);
    }

    @Override
    protected void run(Object... params) {
        final Bookmark bookmark;
        if (params.length != 0) {
            bookmark = (Bookmark)params[0];
        }else {
            bookmark = Reader.addSelectionBookmark();
        }
        if (bookmark == null) {
            return;
        }
        Logger.e("这里是操作书签的一个操作**********************另类的笔记数据------------"+bookmark.toString());
    }
}
