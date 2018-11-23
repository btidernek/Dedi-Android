package org.btider.dediapp.giph.ui;


import android.os.Bundle;
import android.support.v4.content.Loader;

import org.btider.dediapp.giph.model.GiphyImage;
import org.btider.dediapp.giph.net.GiphyGifLoader;
import org.btider.dediapp.giph.model.GiphyImage;
import org.btider.dediapp.giph.net.GiphyGifLoader;

import java.util.List;

public class GiphyGifFragment extends GiphyFragment {

  @Override
  public Loader<List<GiphyImage>> onCreateLoader(int id, Bundle args) {
    return new GiphyGifLoader(getActivity(), searchString);
  }

}
