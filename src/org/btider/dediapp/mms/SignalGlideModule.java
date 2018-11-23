package org.btider.dediapp.mms;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.UnitModelLoader;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.StreamGifDecoder;
import com.bumptech.glide.module.AppGlideModule;

import org.btider.dediapp.contacts.avatars.ContactPhoto;
import org.btider.dediapp.crypto.AttachmentSecret;
import org.btider.dediapp.crypto.AttachmentSecretProvider;
import org.btider.dediapp.giph.model.GiphyPaddedUrl;
import org.btider.dediapp.glide.ContactPhotoLoader;
import org.btider.dediapp.glide.GiphyPaddedUrlLoader;
import org.btider.dediapp.glide.OkHttpUrlLoader;
import org.btider.dediapp.glide.cache.EncryptedBitmapCacheDecoder;
import org.btider.dediapp.glide.cache.EncryptedBitmapResourceEncoder;
import org.btider.dediapp.glide.cache.EncryptedCacheEncoder;
import org.btider.dediapp.glide.cache.EncryptedGifCacheDecoder;
import org.btider.dediapp.glide.cache.EncryptedGifDrawableResourceEncoder;
import org.btider.dediapp.contacts.avatars.ContactPhoto;
import org.btider.dediapp.crypto.AttachmentSecret;
import org.btider.dediapp.crypto.AttachmentSecretProvider;
import org.btider.dediapp.giph.model.GiphyPaddedUrl;
import org.btider.dediapp.glide.ContactPhotoLoader;
import org.btider.dediapp.glide.cache.EncryptedBitmapCacheDecoder;
import org.btider.dediapp.glide.cache.EncryptedCacheEncoder;
import org.btider.dediapp.glide.cache.EncryptedGifCacheDecoder;
import org.btider.dediapp.glide.cache.EncryptedBitmapResourceEncoder;
import org.btider.dediapp.glide.cache.EncryptedGifDrawableResourceEncoder;
import org.btider.dediapp.glide.GiphyPaddedUrlLoader;
import org.btider.dediapp.glide.OkHttpUrlLoader;
import org.btider.dediapp.mms.AttachmentStreamUriLoader.AttachmentModel;
import org.btider.dediapp.mms.DecryptableStreamUriLoader.DecryptableUri;

import java.io.File;
import java.io.InputStream;

@GlideModule
public class SignalGlideModule extends AppGlideModule {

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }

  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setLogLevel(Log.ERROR);
//    builder.setDiskCache(new NoopDiskCacheFactory());
  }

  @Override
  public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    AttachmentSecret attachmentSecret = AttachmentSecretProvider.getInstance(context).getOrCreateAttachmentSecret();
    byte[]           secret           = attachmentSecret.getModernKey();

    registry.prepend(File.class, File.class, UnitModelLoader.Factory.getInstance());
    registry.prepend(InputStream.class, new EncryptedCacheEncoder(secret, glide.getArrayPool()));
    registry.prepend(File.class, Bitmap.class, new EncryptedBitmapCacheDecoder(secret, new StreamBitmapDecoder(new Downsampler(registry.getImageHeaderParsers(), context.getResources().getDisplayMetrics(), glide.getBitmapPool(), glide.getArrayPool()), glide.getArrayPool())));
    registry.prepend(File.class, GifDrawable.class, new EncryptedGifCacheDecoder(secret, new StreamGifDecoder(registry.getImageHeaderParsers(), new ByteBufferGifDecoder(context, registry.getImageHeaderParsers(), glide.getBitmapPool(), glide.getArrayPool()), glide.getArrayPool())));

    registry.prepend(Bitmap.class, new EncryptedBitmapResourceEncoder(secret));
    registry.prepend(GifDrawable.class, new EncryptedGifDrawableResourceEncoder(secret));

    registry.append(ContactPhoto.class, InputStream.class, new ContactPhotoLoader.Factory(context));
    registry.append(DecryptableUri.class, InputStream.class, new DecryptableStreamUriLoader.Factory(context));
    registry.append(AttachmentModel.class, InputStream.class, new AttachmentStreamUriLoader.Factory());
    registry.append(GiphyPaddedUrl.class, InputStream.class, new GiphyPaddedUrlLoader.Factory());
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }

  public static class NoopDiskCacheFactory implements DiskCache.Factory {
    @Override
    public DiskCache build() {
      return new DiskCacheAdapter();
    }
  }
}
