package org.btider.dediapp.profiles;


import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import org.btider.dediapp.R;
import org.btider.dediapp.util.ViewUtil;

public class InformationSecurityInfoView extends FrameLayout {

  private           View      container;

  public InformationSecurityInfoView(@NonNull Context context) {
    super(context);
    initialize();
  }

  public InformationSecurityInfoView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public InformationSecurityInfoView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public InformationSecurityInfoView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initialize();
  }

  private void initialize() {
    inflate(getContext(), R.layout.profile_security_info_view, this);

    this.container = ViewUtil.findById(this, R.id.container);
    this.container.setOnClickListener(view -> {
      new AlertDialog.Builder(getContext())
            .setIconAttribute(R.attr.dialog_info_icon)
            .setMessage(R.string.profile_security_info_text_full)
            .setNegativeButton(R.string.close, null)
            .show();
    });
  }

}
