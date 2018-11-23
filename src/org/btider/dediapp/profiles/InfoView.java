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
import android.widget.ImageView;
import android.widget.TextView;

import org.btider.dediapp.R;
import org.btider.dediapp.util.ViewUtil;
import org.w3c.dom.Text;

public class InfoView extends FrameLayout {

  private           View      container;
  private TextView text_view_message;
  private ImageView imageview;

  private Integer logo = null;
  private Integer background_color = null;
  private String message = "";
  private String full_message = null;
  private Integer text_color = null;

  public void setText_color(Integer text_color) {
    this.text_color = text_color;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  private Integer getLogo() {
    return logo;
  }

  public void setLogo(Integer logo) {
    this.logo = logo;
  }

  private Integer getBackground_color() {
    return background_color;
  }

  public void setBackground_color(Integer background_color) {
    this.background_color = background_color;
  }

  private String getFull_message() {
    return full_message;
  }

  public void setFull_message(String full_message) {
    this.full_message = full_message;
  }

  public InfoView(@NonNull Context context) {
    super(context);
    //initialize();
  }

  public InfoView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    //initialize();
  }

  public InfoView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    //initialize();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public InfoView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    //initialize();
  }

  public void initialize() {
    inflate(getContext(), R.layout.profile_info_view, this);

    this.container = ViewUtil.findById(this, R.id.container);
    this.text_view_message = ViewUtil.findById(this,R.id.message_view);
    this.imageview = ViewUtil.findById(this,R.id.imageview);

    this.text_view_message.setText(message);

    if(this.text_color != null){
      this.text_view_message.setTextColor(text_color);
    }

    if(getLogo() != null)
      this.imageview.setImageResource(getLogo());
    if(getBackground_color() != null)
      this.container.setBackgroundResource(getBackground_color());

    if(getFull_message() != null){
      this.container.setOnClickListener(view -> {
        new AlertDialog.Builder(getContext())
              .setIconAttribute(R.attr.dialog_info_icon)
              .setMessage(getFull_message())
              .setNegativeButton(R.string.close, null)
              .show();
      });
    }
  }

}
