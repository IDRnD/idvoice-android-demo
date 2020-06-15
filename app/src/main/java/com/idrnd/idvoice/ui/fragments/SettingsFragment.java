package com.idrnd.idvoice.ui.fragments;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.utils.logs.FileUtils;

import org.jetbrains.annotations.NotNull;

public class SettingsFragment extends Fragment {

    private final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onActivityCreated(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // This is done so that the toolbar is not too contrasting against the background of the transparent bar status
        requireActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.settingsTitle).setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );

        view.findViewById(R.id.sendLogsButton).setOnClickListener((button) -> {
                    button.setEnabled(false);
                    View progressLayout = view.findViewById(R.id.sendingLogsLoader);
                    progressLayout.setVisibility(View.VISIBLE);
                    FileUtils.getInstance().zipLogs((zipLogs) -> {
                                progressLayout.setVisibility(View.GONE);
                                try {
                                    Uri zipUri = FileProvider.getUriForFile(
                                            requireContext(),
                                            requireContext().getPackageName() +  ".fileprovider",
                                            zipLogs
                                    );

                                    FileUtils.getInstance().sendFileByEmail(requireActivity(),  zipUri, "Logs from IDVoice");
                                } catch (PackageManager.NameNotFoundException e) {
                                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Send-message application was not found", e);
                                }
                                button.setEnabled(true);
                            }
                    );
                }
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        requireView().findViewById(R.id.sendLogsButton).setEnabled(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Return back transparent status bar
        requireActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
    }
}