/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.util;

import android.content.Context;

import net.gsantner.markor.App;

import java.io.File;

public class ContextUtils extends net.gsantner.opoc.util.ContextUtils {
    public ContextUtils(Context context) {
        super(context);
    }

    public static ContextUtils get() {
        return new ContextUtils(App.get());
    }

    // Either pass file or null and absolutePath
    public boolean isMaybeMarkdownFile(File file, String... absolutePath) {
        String path = (absolutePath != null && absolutePath.length > 0)
                ? absolutePath[0] : file.getAbsolutePath();
        path = path.toLowerCase();
        return path.endsWith(".md") || path.endsWith(".markdown") || path.endsWith(".txt");
    }
}
