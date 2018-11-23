package org.btider.dediapp.util.concurrent;

import org.btider.dediapp.util.concurrent.ListenableFuture.Listener;

import java.util.concurrent.ExecutionException;

public abstract class AssertedSuccessListener<T> implements ListenableFuture.Listener<T> {
  @Override
  public void onFailure(ExecutionException e) {
    throw new AssertionError(e);
  }
}
