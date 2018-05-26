/*
 * Copyright (c) 2016 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.snpe.imageclassifiers.tasks;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.SNPE;
import com.qualcomm.qti.snpe.imageclassifiers.Model;
import com.qualcomm.qti.snpe.imageclassifiers.ModelOverviewFragmentController;

import java.io.File;
import java.io.IOException;

public class LoadNetworkTask extends AsyncTask<File, Void, NeuralNetwork> {

    private static final String LOG_TAG = LoadNetworkTask.class.getSimpleName();

    private final ModelOverviewFragmentController mController;

    private final Model mModel;

    private final Application mApplication;

    private final NeuralNetwork.Runtime mTargetRuntime;

    public LoadNetworkTask(final Application application,
                           final ModelOverviewFragmentController controller,
                           final Model model, NeuralNetwork.Runtime targetRuntime) {
        mApplication = application;
        mController = controller;
        mModel = model;
        mTargetRuntime = targetRuntime;
    }

    @Override
    protected NeuralNetwork doInBackground(File... params) {
        NeuralNetwork network = null;
        try {
            final SNPE.NeuralNetworkBuilder builder = new SNPE.NeuralNetworkBuilder(mApplication)
                    .setDebugEnabled(false)
                    .setRuntimeOrder(mTargetRuntime)
                    .setModel(mModel.file);
            network = builder.build();
        } catch (IllegalStateException | IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return network;
    }

    @Override
    protected void onPostExecute(NeuralNetwork neuralNetwork) {
        super.onPostExecute(neuralNetwork);
        if (neuralNetwork != null) {
            if (!isCancelled()) {
                mController.onNetworkLoaded(neuralNetwork);
            } else {
                neuralNetwork.release();
            }
        } else {
            if (!isCancelled()) {
                mController.onNetworkLoadFailed();
            }
        }
    }
}
