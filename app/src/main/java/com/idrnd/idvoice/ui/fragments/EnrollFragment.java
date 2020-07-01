package com.idrnd.idvoice.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.ui.dialogs.speechrecord.AbstractSpeechRecordDialog;
import com.idrnd.idvoice.ui.dialogs.speechrecord.SpeechEndpointRecordDialog;
import com.idrnd.idvoice.utils.EngineManager;
import com.idrnd.idvoice.utils.Prefs;
import com.idrnd.idvoice.utils.Prefs.VoiceTemplateType;
import com.idrnd.idvoice.utils.logs.FileUtils;

import net.idrnd.voicesdk.verify.VoiceTemplate;
import net.idrnd.voicesdk.verify.VoiceVerifyEngine;

import java.util.ArrayList;
import java.util.List;

import static com.idrnd.idvoice.IDVoiceApplication.singleTaskRunner;

/**
 * Fragment for text dependent enrollment
 */
public class EnrollFragment extends Fragment {

    public final int PERMISSION_REQUEST = 44;
    private final int NUMBER_ENROLL_ENTRIES = 3;

    private VoiceVerifyEngine verifyEngine = EngineManager.getInstance().getVerifyEngine(VoiceTemplateType.TextDependent);
    private int counter = 0;

    private TextView recordButton;
    private List<ImageView> images;
    private LinearLayout loaderView;
    private VoiceTemplate[] voices = new VoiceTemplate[3];
    private AbstractSpeechRecordDialog dialog = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recordButton = view.findViewById(R.id.recordBtn);
        loaderView = view.findViewById(R.id.progerssLayout);
        initCounters(view);
        view.findViewById(R.id.backBtn).setOnClickListener(v ->
            getFragmentManager().popBackStack()
        );

        // Main function this fragment
        initRecordButton();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recordButton.performClick();
            } else {
                Toast.makeText(requireActivity(), R.string.need_permissions, Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initRecordButton() {
        recordButton.setOnClickListener(view -> {
            // Check if the are enough enrollment entries
            if (counter < NUMBER_ENROLL_ENTRIES) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                    dialog = new SpeechEndpointRecordDialog(
                        requireContext(),
                        getString(R.string.message_for_td_enroll)
                    );

                    dialog.setStopRecordingListener(recordObject -> {
                        dialog.dismiss();
                        changeCounter(counter + 1);

                        singleTaskRunner.execute(() -> {
                                // Create voice template and store it in array
                                voices[counter - 1] = verifyEngine.createVoiceTemplate(
                                    recordObject.samples,
                                    recordObject.sampleRate
                                );

                                // Logging audio record
                                FileUtils.getInstance().saveAudioRecordWithLog(recordObject);

                                if (counter <= 2) {
                                    String message = String.format(getString(R.string.recording_done), counter);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(
                                            images.get(counter - 1).getContext(),
                                            message,
                                            Toast.LENGTH_SHORT
                                        ).show();
                                    });
                                } else {
                                    counter = 3;
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(
                                            images.get(counter - 1).getContext(),
                                            R.string.enrollment_done,
                                            Toast.LENGTH_SHORT
                                        ).show();
                                        loaderView.setVisibility(View.VISIBLE);
                                    });

                                    // After a successful N voice templates  creation, we need to
                                    // merge them to a union voice template which will a speaker voiceprint.
                                    mergeAndSaveTemplates();

                                    return true;
                                }
                                return false;
                            }, (result) -> {
                                if (result) {
                                    getFragmentManager().popBackStack();
                                }
                            }
                        );
                    });
                    
                    dialog.show();
                } else {
                    requestPermissions(new String[]{ Manifest.permission.RECORD_AUDIO }, PERMISSION_REQUEST);
                }
            } else {
                Toast.makeText((requireActivity()), R.string.enrollment_done, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mergeAndSaveTemplates() {
        // Merge created templates into one
        VoiceTemplate templateMerged = EngineManager.getInstance().getVerifyEngine(VoiceTemplateType.TextDependent).mergeVoiceTemplates(voices);

        // Save template to shared preferences
        Prefs.getInstance().setVoiceTemplateSync(templateMerged.serialize(), VoiceTemplateType.TextDependent);
    }

    private void changeCounter(int newCounter) {
        images.get(newCounter - 1).setImageResource(R.mipmap.ok_on);
        counter = newCounter;
    }

    private void initCounters(View rootView) {
        images = new ArrayList<>();
        images.add((ImageView) rootView.findViewById(R.id.firstCounter));
        images.add((ImageView) rootView.findViewById(R.id.secondCounter));
        images.add((ImageView) rootView.findViewById(R.id.thirdCounter));
    }
}

