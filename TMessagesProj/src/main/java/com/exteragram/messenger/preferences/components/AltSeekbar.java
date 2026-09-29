package com.exteragram.messenger.preferences.components;

import android.content.Context;

/**
 * Compatibility class for legacy plugins and DEX modules expecting
 * com.exteragram.messenger.preferences.components.AltSeekbar.
 */
public class AltSeekbar extends app.exteraless.appearance.AltSeekbar {

    public AltSeekbar(Context context, app.exteraless.appearance.AltSeekbar.OnDrag onDrag, int min, int max,
                      String title, String left, String right) {
        super(context, onDrag, min, max, title, left, right);
    }
}
