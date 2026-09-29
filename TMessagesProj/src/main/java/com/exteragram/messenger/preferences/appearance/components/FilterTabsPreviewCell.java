package com.exteragram.messenger.preferences.appearance.components;

import android.content.Context;
import app.exteraless.appearance.FoldersPreviewCell;
import org.telegram.ui.ActionBar.Theme;

/**
 * Compatibility class for legacy plugins and DEX modules expecting
 * com.exteragram.messenger.preferences.appearance.components.FilterTabsPreviewCell.
 */
public class FilterTabsPreviewCell extends FoldersPreviewCell {

    public FilterTabsPreviewCell(Context context) {
        super(context);
    }

    public FilterTabsPreviewCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider);
    }
}
