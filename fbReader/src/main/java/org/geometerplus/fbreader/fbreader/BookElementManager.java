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

package org.geometerplus.fbreader.fbreader;

import org.geometerplus.zlibrary.text.view.ExtensionElementManager;

import java.util.*;

class BookElementManager extends ExtensionElementManager {

    private final FBView myView;
    private final Runnable myScreenRefresher;
    private final Map<Map<String, String>, List<BookElement>> myCache = new HashMap<Map<String, String>, List<BookElement>>();
    private Timer myTimer;

    BookElementManager(final FBView view) {
        myView = view;
        myScreenRefresher = new Runnable() {
            public void run() {
                view.Application.getViewWidget().reset();
                view.Application.getViewWidget().repaint();
            }
        };
    }

    @Override
    protected synchronized List<BookElement> getElements(String type, Map<String, String> data) {
        if (!"opds".equals(type)) {
            return Collections.emptyList();
        }

        List<BookElement> elements = myCache.get(data);
        if (elements == null) {
            try {
                final int count = Integer.valueOf(data.get("size"));
                elements = new ArrayList<BookElement>(count);
                for (int i = 0; i < count; ++i) {
                    elements.add(new BookElement(myView));
                }
                startLoading(data.get("src"), elements);
            }catch (Throwable t) {
                return Collections.emptyList();
            }
            myCache.put(data, elements);
        }
        return Collections.unmodifiableList(elements);
    }

    private void startLoading(final String url, final List<BookElement> elements) {

        new Thread() {
            public void run() {
                try {
                    myTimer = null;
                    int index = 0;
                    for (BookElement book : elements) {
                        myScreenRefresher.run();
                    }
                }catch (Exception e) {
                    if (myTimer == null) {
                        myTimer = new Timer();
                    }
                    myTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            startLoading(url, elements);
                        }
                    }, 10000);
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
