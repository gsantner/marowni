package net.gsantner.opoc.ui;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

public class FileSearchDialog {

    public static void showDialog(final Activity activity, final Callback.a1<CallBackOptions> dialogCallback) {
        final Dialog dialog = new Dialog(activity);
        dialog.showDialog(dialogCallback);
    }


    private static class Dialog {
        private AlertDialog _dialog;
        private final Activity _activity;

        private Dialog(final Activity activity) {
            _activity = activity;
        }

        private void showDialog(Callback.a1<CallBackOptions> dialogCallback) {
            AlertDialog.Builder dialogBuilder = buildDialog(this, dialogCallback);
            _dialog = dialogBuilder.create();
            Window _window = _dialog.getWindow();
            if (_window != null) {
                _window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
            _dialog.show();
            if (_window != null) {
                _window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private static AlertDialog.Builder buildDialog(final Dialog initializer, final Callback.a1<CallBackOptions> dialogCallback) {
        final AppSettings appSettings = new AppSettings(initializer._activity);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(initializer._activity, appSettings.isDarkThemeEnabled() ? R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);

        final LinearLayout dialogLayout = new LinearLayout(initializer._activity);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        final int dp4px = (int) (new ContextUtils(dialogLayout.getContext()).convertDpToPx(4));
        final int textColor = ContextCompat.getColor(initializer._activity, appSettings.isDarkThemeEnabled() ? R.color.dark__primary_text : R.color.light__primary_text);

        final LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        margins.setMargins(dp4px * 5, dp4px, dp4px * 5, dp4px);

        final LinearLayout.LayoutParams subCheckBoxMargins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subCheckBoxMargins.setMargins(dp4px * 5 * 2, dp4px, dp4px * 5, dp4px);

        final AppCompatEditText searchEditText = new AppCompatEditText(initializer._activity);
        final CheckBox regexCheckBox = new CheckBox(initializer._activity);
        final CheckBox caseSensitivityCheckBox = new CheckBox(initializer._activity);
        final CheckBox searchInContentCheckBox = new CheckBox(initializer._activity);
        final CheckBox onlyFirstContentMatchCheckBox = new CheckBox(initializer._activity);

        final Callback.a0 submit = () -> {
            final String query = searchEditText.getText().toString();
            if (dialogCallback != null && !TextUtils.isEmpty(query)) {
                CallBackOptions callBackOptions = new CallBackOptions(query, regexCheckBox.isChecked(), caseSensitivityCheckBox.isChecked(), searchInContentCheckBox.isChecked(), onlyFirstContentMatchCheckBox.isChecked());
                dialogCallback.callback(callBackOptions);
            }
        };

        // EdiText: Search query input
        searchEditText.setHint(R.string.search);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(textColor);
        searchEditText.setHintTextColor((textColor & 0x00FFFFFF) | 0x99000000);
        searchEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchEditText.setOnKeyListener((keyView, keyCode, keyEvent) -> {
            if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (initializer._dialog != null) {
                    initializer._dialog.dismiss();
                }
                submit.callback();
                return true;
            }
            return false;
        });
        dialogLayout.addView(searchEditText, margins);


        // Checkbox: Regex search
        regexCheckBox.setText(R.string.regex_search);
        regexCheckBox.setChecked(AppSettings.get().isSearchQueryUseRegex());
        dialogLayout.addView(regexCheckBox, margins);

        // Checkbox: Case sensitive
        caseSensitivityCheckBox.setText(R.string.case_sensitive);
        caseSensitivityCheckBox.setChecked(AppSettings.get().isSearchQueryCaseSensitive());
        dialogLayout.addView(caseSensitivityCheckBox, margins);

        // Checkbox: Search in content
        searchInContentCheckBox.setText(R.string.search_in_content);
        searchInContentCheckBox.setChecked(AppSettings.get().isSearchInContent());
        dialogLayout.addView(searchInContentCheckBox, margins);
        searchInContentCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onlyFirstContentMatchCheckBox.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
        });

        // Checkbox: Only first content match
        onlyFirstContentMatchCheckBox.setText(R.string.only_first_content_match);
        onlyFirstContentMatchCheckBox.setChecked(AppSettings.get().isOnlyFirstContentMatch());
        onlyFirstContentMatchCheckBox.setVisibility(searchInContentCheckBox.isChecked() ? View.VISIBLE : View.INVISIBLE);
        dialogLayout.addView(onlyFirstContentMatchCheckBox, subCheckBoxMargins);


        // Configure dialog
        dialogBuilder.setView(dialogLayout)
                .setMessage(R.string.recursive_search_in_current_directory)
                .setTitle(R.string.search)
                .setOnCancelListener(null)
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    submit.callback();
                });

        return dialogBuilder;
    }

    public static class CallBackOptions {
        public String _query;
        public boolean _isRegexQuery;
        public boolean _isCaseSensitiveQuery;
        public boolean _isSearchInContent;
        public boolean _isOnlyFirstContentMatch;

        public CallBackOptions(final String query, final boolean isRegexQuery, final boolean isCaseSensitiveQuery, final boolean isSearchInContent, final boolean isOnlyFirstContentMatch) {
            _query = query;
            _isRegexQuery = isRegexQuery;
            _isCaseSensitiveQuery = isCaseSensitiveQuery;
            _isSearchInContent = isSearchInContent;
            _isOnlyFirstContentMatch = isOnlyFirstContentMatch;
        }
    }
}