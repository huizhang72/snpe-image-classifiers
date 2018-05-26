/*
 * Copyright (c) 2016 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.snpe.imageclassifiers;

import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.SNPE;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class ModelOverviewFragment extends Fragment {

    public static final String EXTRA_MODEL = "model";

    enum MenuRuntimeGroup {

        SelectCpuRuntime(NeuralNetwork.Runtime.CPU),
        SelectGpuRuntime(NeuralNetwork.Runtime.GPU),
        SelectDspRuntime(NeuralNetwork.Runtime.DSP);

        public static int ID = 1;

        public NeuralNetwork.Runtime runtime;

        MenuRuntimeGroup(NeuralNetwork.Runtime runtime) {
            this.runtime = runtime;
        }
    }

    private GridView mImageGrid;

    private ModelImagesAdapter mImageGridAdapter;

    private ModelOverviewFragmentController mController;

    private TextView mDimensionsText;

    private TextView mModelNameText;

    private Spinner mOutputLayersSpinners;

    private TextView mClassificationText;

    private TextView mModelVersionText;

    public static ModelOverviewFragment create(final Model model) {
        final ModelOverviewFragment fragment = new ModelOverviewFragment();
        final Bundle arguments = new Bundle();
        arguments.putParcelable(EXTRA_MODEL, model);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_model, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mImageGrid = (GridView) view.findViewById(R.id.model_image_grid);
        mImageGridAdapter = new ModelImagesAdapter(getActivity());
        mImageGrid.setAdapter(mImageGridAdapter);
        mImageGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Bitmap bitmap = mImageGridAdapter.getItem(position);
                mController.classify(bitmap);
            }
        });

        mModelNameText = (TextView) view.findViewById(R.id.model_overview_name_text);
        mModelVersionText = (TextView) view.findViewById(R.id.model_overview_version_text);
        mDimensionsText = (TextView) view.findViewById(R.id.model_overview_dimensions_text);
        mOutputLayersSpinners = (Spinner) view.findViewById(R.id.model_overview_layers_spinner);
        mClassificationText = (TextView) view.findViewById(R.id.model_overview_classification_text);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        final Model model = getArguments().getParcelable(EXTRA_MODEL);
        mController = new ModelOverviewFragmentController(
                (Application) getActivity().getApplicationContext(), model);
    }

    @Override
    public void onCreateOptionsMenu(android.view.Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        final SNPE.NeuralNetworkBuilder builder = new SNPE.NeuralNetworkBuilder(
                (Application) (getActivity().getApplicationContext()));
        for (MenuRuntimeGroup item : MenuRuntimeGroup.values()) {
            if (builder.isRuntimeSupported(item.runtime)) {
                menu.add(MenuRuntimeGroup.ID, item.ordinal(), 0, item.runtime.name());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getGroupId() == MenuRuntimeGroup.ID) {
            final MenuRuntimeGroup option = MenuRuntimeGroup.values()[item.getItemId()];
            mController.setTargetRuntime(option.runtime);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        mController.attach(this);
    }

    @Override
    public void onStop() {
        mController.detach(this);
        super.onStop();
    }

    public void addSampleBitmap(Bitmap bitmap) {
        if (mImageGridAdapter.getPosition(bitmap) == -1) {
            mImageGridAdapter.add(bitmap);
            mImageGridAdapter.notifyDataSetChanged();
        }
    }

    public void setNetworkDimensions(Map<String, int[]> inputDimensions) {
        mDimensionsText.setText(Arrays.toString(inputDimensions.get("data")));
    }

    public void displayModelLoadFailed() {
        mClassificationText.setVisibility(View.VISIBLE);
        mClassificationText.setText(R.string.model_load_failed);
        Toast.makeText(getActivity(), R.string.model_load_failed, Toast.LENGTH_SHORT).show();
    }

    public void setModelName(String modelName) {
        mModelNameText.setText(modelName);
    }

    public void setModelVersion(String version) {
        mModelVersionText.setText(version);
    }

    public void setOutputLayersNames(Set<String> outputLayersNames) {
        mOutputLayersSpinners.setAdapter(new ArrayAdapter<>(
            getActivity(), android.R.layout.simple_list_item_1,
            new LinkedList<>(outputLayersNames)));
    }

    public void setClassificationResult(String[] classificationResult) {
        if (classificationResult.length > 0) {
            mClassificationText.setText(
                    String.format("%s: %s", classificationResult[0], classificationResult[1]));
        }
        mClassificationText.setVisibility(View.VISIBLE);
    }

    public void setLoadingVisible(boolean visible) {
        mClassificationText.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            mClassificationText.setText(R.string.loading_network);
        }
    }

    public void displayModelNotLoaded() {
        Toast.makeText(getActivity(), R.string.model_not_loaded, Toast.LENGTH_SHORT).show();
    }

    public void displayClassificationFailed() {
        Toast.makeText(getActivity(), R.string.classification_failed, Toast.LENGTH_SHORT).show();
    }

    private static class ModelImagesAdapter extends ArrayAdapter<Bitmap> {

        public ModelImagesAdapter(Context context) {
            super(context, R.layout.model_image_layout);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.model_image_layout, parent, false);
            } else {
                view = convertView;
            }

            final ImageView imageView = ImageView.class.cast(view);
            imageView.setImageBitmap(getItem(position));
            return view;
        }
    }
}
