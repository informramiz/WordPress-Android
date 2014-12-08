package org.wordpress.persistentedittext;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

public class PersistentEditTextHelper {
    private String mUniqueId;
    private PersistentEditTextDatabase mPersistentEditTextDatabase;

    public PersistentEditTextHelper(Context context) {
        mPersistentEditTextDatabase = new PersistentEditTextDatabase(context);
    }

    public void setUniqueId(String uniqueId) {
        mUniqueId = uniqueId;
    }

    public String getUniqueId() {
        return mUniqueId;
    }

    private static String getViewPathId(View view) {
        StringBuilder sb = new StringBuilder();
        for (View currentView = view; currentView != null && currentView.getParent() != null
                && currentView.getParent() instanceof View; currentView = (View) currentView.getParent()) {
            sb.append(currentView.getId());
        }
        return sb.toString();
    }

    public void clearSavedText(View view) {
        clearSavedText(view, mUniqueId);
    }

    public void loadString(EditText editText) {
        String text = mPersistentEditTextDatabase.get(getViewPathId(editText) + mUniqueId, "");
        if (!text.isEmpty()) {
            editText.setText(text);
            editText.setSelection(text.length());
        }
    }

    public void saveString(EditText editText) {
        if (editText.getText() == null) {
            return;
        }
        mPersistentEditTextDatabase.put(getViewPathId(editText) + mUniqueId, editText.getText().toString());
    }

    public void clearSavedText(View view, String uniqueId) {
        mPersistentEditTextDatabase.remove(getViewPathId(view) + uniqueId);
    }
}
