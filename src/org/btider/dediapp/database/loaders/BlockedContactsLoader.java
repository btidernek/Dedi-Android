package org.btider.dediapp.database.loaders;

import android.content.Context;
import android.database.Cursor;

import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.util.AbstractCursorLoader;

public class BlockedContactsLoader extends AbstractCursorLoader {

  public BlockedContactsLoader(Context context) {
    super(context);
  }

  @Override
  public Cursor getCursor() {
    return DatabaseFactory.getRecipientDatabase(getContext())
                          .getBlocked();
  }

}
