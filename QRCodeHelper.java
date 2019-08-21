package com.example.st;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

public class QRCodeHelper {
    private ErrorCorrectionLevel mErrorCorrectionLevel;
    private Bitmap mLogo;
    private String mContent; // String to encode

    private int mWidth = 400, mHeight = 400;
    private int mMargin;

    public static QRCodeHelper newInstance() {
        return new QRCodeHelper();
    }

    public QRCodeHelper setLogo(Bitmap logo) {
        mLogo = logo;
        return this;
    }

    public QRCodeHelper setErrorCorrectionLevel(ErrorCorrectionLevel level) {
        mErrorCorrectionLevel = level;
        return this;
    }

    // Set the string we are encoding
    public QRCodeHelper setContent(String content) {
        mContent = content;
        return this;
    }

    public QRCodeHelper setWidthAndHeight(@IntRange(from = 1) int width, @IntRange(from = 1) int height) {
        mWidth = width;
        mHeight = height;
        return this;
    }

    public QRCodeHelper setMargin(@IntRange(from = 0) int margin) {
        mMargin = margin;
        return this;
    }

    public Bitmap generate() {
        Map<EncodeHintType, Object> hintsMap = new HashMap<>();
        hintsMap.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hintsMap.put(EncodeHintType.ERROR_CORRECTION, mErrorCorrectionLevel);
        hintsMap.put(EncodeHintType.MARGIN, mMargin);

        try {
            // create the QR code BitMatrix
            BitMatrix bitMatrix = new QRCodeWriter().encode(mContent, BarcodeFormat.QR_CODE, mWidth, mHeight, hintsMap);
            int[] pixels = new int[mWidth * mHeight];

            // set white and black pixels according to the BitMatrix
            for (int i = 0; i < mHeight; i++) {
                for (int j = 0; j < mWidth; j++) {
                    if (bitMatrix.get(j, i)) pixels[i * mWidth + j] = 0x00000000;
                    else pixels[i * mWidth + j] = 0xffffffff;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(pixels, 0, mWidth, mWidth, mHeight, Bitmap.Config.RGB_565);

            // add a logo if desired
            if (mLogo != null) bitmap = addLogo(bitmap, mLogo);
            return bitmap;
        }
        catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isInRect(int x, int y) {
        return ((x > mMargin * 8 && x < mWidth - mMargin * 8) && (y > mMargin * 8 && y < mHeight - mMargin * 8));
    }

    private Bitmap addLogo(@NonNull Bitmap qrCodeBitmap, @NonNull Bitmap logo) {
        int qrCodeWidth = qrCodeBitmap.getWidth();
        int qrCodeHeight = qrCodeBitmap.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();

        Bitmap blankBitmap = Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(blankBitmap);

        /*
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        RectF rect = new RectF(50, 50, 200, 200);
        canvas.drawRoundRect(rect, logoWidth, logoHeight, paint);
        */

        // background is the barcode
        canvas.drawBitmap(qrCodeBitmap, 0, 0, null);
        // canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.save();

        // shrink the canvas and draw the logo in the center (draw a smaller logo)
        float scaleSize = 1.0f;
        while ((logoWidth / scaleSize) > (qrCodeWidth / 5) || (logoHeight / scaleSize) > (qrCodeHeight / 5)) {
            scaleSize *= 2;
        }
        float sx = 1.0f / scaleSize;
        canvas.scale(sx, sx, qrCodeWidth / 2, qrCodeHeight / 2);
        canvas.drawBitmap(logo, (qrCodeWidth - logoWidth) / 2, (qrCodeHeight - logoHeight) / 2, null);

        canvas.restore();
        return blankBitmap;
    }
}
