package com.ai_autocreate.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;

public class MediaUtils {

    public static Bitmap createVideoThumbnail(String videoPath) {
        try {
            return ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap createVideoThumbnail(String videoPath, int width, int height) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);

            // Get frame at 1 second
            Bitmap bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            retriever.release();

            if (bitmap != null) {
                // Scale bitmap to desired dimensions
                return Bitmap.createScaledBitmap(bitmap, width, height, false);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap createImageThumbnail(String imagePath, int width, int height) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, width, height);
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

            if (bitmap != null) {
                // Scale bitmap to exact dimensions
                return Bitmap.createScaledBitmap(bitmap, width, height, false);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static boolean saveBitmapToFile(Bitmap bitmap, String filePath) {
        try {
            File file = new File(filePath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getVideoDuration(String videoPath) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);

            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();

            if (durationStr != null) {
                long duration = Long.parseLong(durationStr);
                return formatDuration(duration);
            }

            return "00:00";
        } catch (Exception e) {
            return "00:00";
        }
    }

    public static String getVideoResolution(String videoPath) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);

            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            retriever.release();

            if (widthStr != null && heightStr != null) {
                return widthStr + "x" + heightStr;
            }

            return "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static long getVideoSize(String videoPath) {
        try {
            File file = new File(videoPath);
            return file.length();
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return (size / 1024) + " KB";
        } else if (size < 1024 * 1024 * 1024) {
            return (size / (1024 * 1024)) + " MB";
        } else {
            return (size / (1024 * 1024 * 1024)) + " GB";
        }
    }

    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    public static void recycleViewBitmaps(View view) {
        if (view == null) return;

        view.setBackground(null);

        if (view instanceof android.widget.ImageView) {
            android.widget.ImageView imageView = (android.widget.ImageView) view;
            imageView.setImageDrawable(null);
        }

        if (view instanceof android.widget.VideoView) {
            android.widget.VideoView videoView = (android.widget.VideoView) view;
            videoView.stopPlayback();
        }
    }
}
