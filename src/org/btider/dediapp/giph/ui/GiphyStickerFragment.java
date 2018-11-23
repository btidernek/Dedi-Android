package org.btider.dediapp.giph.ui;


import android.os.Bundle;
import android.support.v4.content.Loader;

import org.btider.dediapp.giph.model.GiphyImage;
import org.btider.dediapp.giph.net.GiphyStickerLoader;
import org.btider.dediapp.giph.model.GiphyImage;
import org.btider.dediapp.giph.net.GiphyStickerLoader;

import java.util.List;

public class GiphyStickerFragment extends GiphyFragment {
  @Override
  public Loader<List<GiphyImage>> onCreateLoader(int id, Bundle args) {
    return new GiphyStickerLoader(getActivity(), searchString);
  }
}
