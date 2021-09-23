package com.application.databind.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;

import com.application.databind.BuildConfig;

import java.nio.ByteBuffer;

public class YuvToRgbConverter {

    Context context;
    RenderScript rs;
    ScriptIntrinsicYuvToRGB scriptYuvToRgb;
    int pixelCount = -1;
    ByteBuffer yuvBuffer;
    Allocation inputAllocation;
    Allocation outputAllocation;

    public YuvToRgbConverter(Context context) {
        this.context = context;
    }

    public void init() {
        rs = RenderScript.create(context);
        scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public synchronized void yuvToRgb(Image image, Bitmap output) {

        if (yuvBuffer == null) {
            pixelCount = image.getCropRect().width() * image.getCropRect().height();
            yuvBuffer = ByteBuffer.allocateDirect(
                    pixelCount * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8
            );
        }

        imageToByteBuffer(image, yuvBuffer);

        if (inputAllocation == null) {
            inputAllocation = Allocation.createSized(rs, Element.U8(rs), yuvBuffer.array().length);
        }
        if (outputAllocation == null) {
            outputAllocation = Allocation.createFromBitmap(rs, output);
        }

        inputAllocation.copyFrom(yuvBuffer.array());
        scriptYuvToRgb.setInput(inputAllocation);
        scriptYuvToRgb.forEach(outputAllocation);
        outputAllocation.copyTo(output);
    }

    private void imageToByteBuffer(Image image, ByteBuffer outputBuffer) {
        if (BuildConfig.DEBUG && !(image.getFormat() == ImageFormat.YUV_420_888)) {
            throw new AssertionError("Assertion failed");
        }

        Rect imageCrop = image.getCropRect();
        Image.Plane[] imagePlanes = image.getPlanes();
        byte[] rowData = new byte[imagePlanes[0].getRowStride()];

        for (int planeIndex = 0; planeIndex < imagePlanes.length; planeIndex++) {
            Image.Plane plane = imagePlanes[planeIndex];
            int outputStride;
            int outputOffset;
            if (planeIndex == 0) {
                outputStride = 1;
                outputOffset = 0;
            } else if (planeIndex == 1) {
                outputStride = 2;
                outputOffset = pixelCount + 1;
            } else if (planeIndex == 2) {
                outputStride = 2;
                outputOffset = pixelCount;
            } else {
                continue;
            }

            ByteBuffer buffer = plane.getBuffer();
            int rowStride = plane.getRowStride();
            int pixelStride = plane.getPixelStride();
            Rect planeCrop;
            if (planeIndex == 0) {
                planeCrop = imageCrop;
            } else {
                planeCrop = new Rect(
                        imageCrop.left / 2,
                        imageCrop.top / 2,
                        imageCrop.right / 2,
                        imageCrop.bottom / 2
                );
            }

            int planeWidth = planeCrop.width();
            int planeHeight = planeCrop.height();
            buffer.position(rowStride * planeCrop.top + pixelStride * planeCrop.left);

            for (int row = 0; row < planeHeight; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = planeWidth;
                    buffer.get(outputBuffer.array(), outputOffset, length);
                    outputOffset += length;
                } else {
                    length = (planeWidth - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < planeWidth; col++) {
                        outputBuffer.array()[outputOffset] = rowData[col * pixelStride];
                        outputOffset += outputStride;
                    }
                }

                if (row < planeHeight - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
    }

    byte[] toBytes(int i) {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }
}
