/*
 * Copyright (c) 2016 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.snpe.imageclassifiers.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Pair;

import com.qualcomm.qti.snpe.FloatTensor;
import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.imageclassifiers.Model;
import com.qualcomm.qti.snpe.imageclassifiers.ModelOverviewFragmentController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ClassifyImageTask extends AsyncTask<Bitmap, Void, String[]> {

    private static final String LOG_TAG = ClassifyImageTask.class.getSimpleName();

    public static final String OUTPUT_LAYER = "prob";

    private static final int FLOAT_SIZE = 4;

    private final NeuralNetwork mNeuralNetwork;

    private final Model mModel;

    private final Bitmap mImage;

    private final ModelOverviewFragmentController mController;

    public ClassifyImageTask(ModelOverviewFragmentController controller,
                             NeuralNetwork network, Bitmap image, Model model) {
        mController = controller;
        mNeuralNetwork = network;
        mImage = image;
        mModel = model;
    }

    @Override
    protected String[] doInBackground(Bitmap... params) {
        final List<String> result = new LinkedList<>();

        final FloatTensor tensor = mNeuralNetwork.createFloatTensor(
                mNeuralNetwork.getInputTensorsShapes().get("data"));

        final int[] dimensions = tensor.getShape();
        final FloatBuffer meanImage = loadMeanImageIfAvailable(mModel.meanImage, tensor.getSize());
        if (meanImage.remaining() != tensor.getSize()) {
            return new String[0];
        }

        final boolean isGrayScale = (dimensions[dimensions.length -1] == 1);
        if (!isGrayScale) {
            writeRgbBitmapAsFloat(mImage, meanImage, tensor);
        } else {
            writeGrayScaleBitmapAsFloat(mImage, meanImage, tensor);
        }

        final Map<String, FloatTensor> inputs = new HashMap<>();
        inputs.put("data", tensor);

        final Map<String, FloatTensor> outputs = mNeuralNetwork.execute(inputs);
        for (Map.Entry<String, FloatTensor> output : outputs.entrySet()) {
            if (output.getKey().equals(OUTPUT_LAYER)) {
                for (Pair<Integer, Float> pair : topK(1, output.getValue())) {
                    result.add(mModel.labels[pair.first]);
                    result.add(String.valueOf(pair.second));
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    protected void onPostExecute(String[] labels) {
        super.onPostExecute(labels);
        if (labels.length > 0) {
            mController.onClassificationResult(labels);
        } else {
            mController.onClassificationFailed();
        }
    }

    private void writeRgbBitmapAsFloat(Bitmap image, FloatBuffer meanImage, FloatTensor tensor) {
        final int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getPixels(pixels, 0, image.getWidth(), 0, 0,
            image.getWidth(), image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                final int rgb = pixels[y * image.getWidth() + x];
                float b = ((rgb)       & 0xFF) - meanImage.get();
                float g = ((rgb >>  8) & 0xFF) - meanImage.get();
                float r = ((rgb >> 16) & 0xFF) - meanImage.get();
                float[] pixelFloats = {b, g, r};
                tensor.write(pixelFloats, 0, pixelFloats.length, y, x);
            }
        }
    }

    private FloatBuffer loadMeanImageIfAvailable(File meanImage, final int imageSize) {
        ByteBuffer buffer = ByteBuffer.allocate(imageSize * FLOAT_SIZE)
                .order(ByteOrder.nativeOrder());
        if (!meanImage.exists()) {
            return buffer.asFloatBuffer();
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(meanImage);
            final byte[] chunk = new byte[1024];
            int read;
            while ((read = fileInputStream.read(chunk)) != -1) {
                buffer.put(chunk, 0, read);
            }
            buffer.flip();
        } catch (IOException e) {
            buffer = ByteBuffer.allocate(imageSize * FLOAT_SIZE);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    // Do thing
                }
            }
        }
        return buffer.asFloatBuffer();
    }

    private void writeGrayScaleBitmapAsFloat(Bitmap image, FloatBuffer meanImage,
                                             FloatTensor tensor) {
        final int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getPixels(pixels, 0, image.getWidth(), 0, 0,
            image.getWidth(), image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                final int rgb = pixels[y * image.getWidth() + x];
                final float b = ((rgb)       & 0xFF);
                final float g = ((rgb >>  8) & 0xFF);
                final float r = ((rgb >> 16) & 0xFF);
                float grayscale = (float) (r * 0.3 + g * 0.59 + b * 0.11);
                grayscale -= meanImage.get();
                tensor.write(grayscale, y, x);
            }
        }
    }

    private Pair<Integer, Float>[] topK(int k, FloatTensor tensor) {
        final float[] array = new float[tensor.getSize()];
        tensor.read(array, 0, array.length);

        final boolean[] selected = new boolean[tensor.getSize()];
        final Pair<Integer, Float> topK[] = new Pair[k];
        int count = 0;
        while (count < k) {
            final int index = top(array, selected);
            selected[index] = true;
            topK[count] = new Pair<>(index, array[index]);
            count++;
        }
        return topK;
    }

    private int top(float[] array, boolean[] selected) {
        int index = 0;
        float max = -1.f;
        for (int i = 0; i < array.length; i++) {
            if (selected[i]) {
                continue;
            }
            if (array[i] > max) {
                max = array[i];
                index = i;
            }
        }
        return index;
    }
}
