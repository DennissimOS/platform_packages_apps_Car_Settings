/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.settings.suggestions;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.android.car.list.ActionIconButtonLineItem;

/**
 * Represents suggestion list item.
 */
public class SuggestionLineItem extends ActionIconButtonLineItem {

    private final CharSequence mSummary;
    private final Drawable mIconDrawable;
    private final Drawable mActionIconDrawable;
    private final View.OnClickListener mOnClickListener;
    private final ActionListener mActionListener;

    /**
     * Creates a {@link SuggestionLineItem} with title, summary, icons, and click handlers.
     */
    public SuggestionLineItem(
            CharSequence title,
            CharSequence summary,
            Drawable iconDrawable,
            Drawable actionIconDrawable,
            View.OnClickListener onClickListener,
            ActionListener actionListener) {
        super(title);
        mSummary = summary;
        mIconDrawable = iconDrawable;
        mActionIconDrawable = actionIconDrawable;
        mOnClickListener = onClickListener;
        mActionListener = actionListener;
    }

    @Override
    public void setIcon(ImageView iconView) {
        iconView.setImageDrawable(mIconDrawable);
    }

    @Override
    public void setActionButtonIcon(ImageView actionButtonIconView) {
        actionButtonIconView.setImageDrawable(mActionIconDrawable);
    }

    @Override
    public CharSequence getDesc() {
        return mSummary;
    }

    @Override
    public boolean isExpandable() {
        return true;
    }

    @Override
    public void onClick(View view) {
        mOnClickListener.onClick(view);
    }

    @Override
    public void onActionButtonClick(int adapterPosition) {
        mActionListener.onSuggestionItemDismissed(adapterPosition);
    }

    /**
     * Interface that surfaces events on the suggestion.
     */
    public interface ActionListener {

        /**
         * Invoked when a suggestions item is dismissed.
         *
         * @param adapterPosition the position of the suggestion item in it's adapter.
         *
         */
        void onSuggestionItemDismissed(int adapterPosition);
    }
}
