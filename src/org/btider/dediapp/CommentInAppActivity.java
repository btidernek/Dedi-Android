package org.btider.dediapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import org.btider.dediapp.contacts.ContactsCursorLoader;
import org.btider.dediapp.util.DynamicLanguage;
import org.btider.dediapp.util.DynamicTheme;

public class CommentInAppActivity extends PassphraseRequiredActionBarActivity {

    private DynamicTheme dynamicTheme    = new DynamicTheme();
    private DynamicLanguage dynamicLanguage = new DynamicLanguage();

    @Override
    protected void onPreCreate() {
        dynamicTheme.onCreate(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState, boolean ready) {
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initFragment(android.R.id.content, new CommentInAppFragment(), dynamicLanguage.getCurrentLocale());
    }

    @Override
    public void onResume() {
        dynamicTheme.onResume(this);
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:  finish();  return true;
        }

        return false;
    }


}
