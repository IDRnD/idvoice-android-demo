package com.idrnd.idvoice.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.ui.dialogs.speechrecord.AbstractSpeechRecordDialog;
import com.idrnd.idvoice.ui.dialogs.speechrecord.SpeechEndpointRecordDialog;
import com.idrnd.idvoice.utils.Prefs;
import com.idrnd.idvoice.utils.logs.FileUtils;
import com.idrnd.idvoice.utils.verification.EngineManager;
import com.idrnd.idvoice.utils.verification.VoiceVerifyEngine;

import net.idrnd.voicesdk.core.common.VoiceTemplate;
import net.idrnd.voicesdk.verify.QualityShortDescription;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.idrnd.idvoice.IDVoiceApplication.singleTaskRunner;
import static com.idrnd.idvoice.ui.activity.MainActivity.showToastWithSpeechRecordQualityStatus;
import static com.idrnd.idvoice.ui.fragments.StartFragment.PERMISSION_REQUEST;
import static com.idrnd.idvoice.utils.Prefs.VoiceTemplateType.TextDependent;
import static net.idrnd.voicesdk.verify.QualityShortDescription.OK;

/**
 * Fragment for text dependent enrollment
 */
public class EnrollFragment extends Fragment {

    private final int NUMBER_ENROLL_ENTRIES = 3;

    private final VoiceVerifyEngine verifyEngine = EngineManager.getInstance().getVerifyEngine(TextDependent);
    private int counter = 0;

    private TextView buttonOfRecording;
    private List<ImageView> recordingStatusImages;
    private ProgressBar enrollProgressBar;
    private final VoiceTemplate[] voiceTemplates = new VoiceTemplate[3];
    private AbstractSpeechRecordDialog dialog = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.registration_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buttonOfRecording = view.findViewById(R.id.recordButton);
        enrollProgressBar = view.findViewById(R.id.enrollProgressBar);
        initCounters(view);
        view.findViewById(R.id.backBtn).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Main function this fragment
        buttonOfRecording.setOnClickListener(buttonView -> {

                    if (ContextCompat.checkSelfPermission(requireContext(), RECORD_AUDIO) != PERMISSION_GRANTED) {
                        requestPermissions(new String[]{ RECORD_AUDIO}, PERMISSION_REQUEST);
                        return;
                    }

                    // Check if the are enough enrollment entries
                    if (counter == NUMBER_ENROLL_ENTRIES) {
                        Toast.makeText((requireActivity()), R.string.enrollment_done, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(dialog == null) {
                        dialog = new SpeechEndpointRecordDialog(
                                requireContext(),
                                getString(R.string.message_for_td_enroll)
                        );
                    }

                    dialog.setOnStopRecordingListener(audioRecord -> {
                                dialog.dismiss();

                                if(Prefs.getInstance().getQualityCheckEnabled()) {
                                    QualityShortDescription qualityStatus = verifyEngine.checkQuality(audioRecord);

                                    if(qualityStatus != OK) {
                                        showToastWithSpeechRecordQualityStatus(requireContext(), qualityStatus);
                                        return;
                                    }
                                }

                                changeCounter(counter + 1);
                                singleTaskRunner.execute(() -> {
                                            // Create voice template and store it in array
                                            voiceTemplates[counter - 1] = verifyEngine.createVoiceTemplate(audioRecord);

                                            // Logging audio record
                                            FileUtils.getInstance().saveAudioRecordWithLog(audioRecord);

                                            if(counter == 3) {
                                                requireActivity().runOnUiThread(() -> {
                                                            Toast.makeText(
                                                                    recordingStatusImages.get(counter - 1).getContext(),
                                                                    R.string.enrollment_done,
                                                                    Toast.LENGTH_SHORT
                                                            ).show();
                                                            enrollProgressBar.setVisibility(View.VISIBLE);
                                                        }
                                                );

                                                // After a successful N voice templates  creation, we need to
                                                // merge them to a union voice template which will a speaker voiceprint.
                                                // Merge created templates into one
                                                VoiceTemplate templateMerged = EngineManager
                                                        .getInstance()
                                                        .getVerifyEngine(TextDependent)
                                                        .mergeVoiceTemplates(voiceTemplates);

                                                // Save template to shared preferences
                                                Prefs.getInstance().setVoiceTemplateSync(
                                                        templateMerged.serialize(),
                                                        TextDependent
                                                );

                                                return true;
                                            } else {
                                                String message = String.format(getString(R.string.recording_done), counter);
                                                requireActivity().runOnUiThread(() -> Toast.makeText(
                                                        recordingStatusImages.get(counter - 1).getContext(),
                                                        message,
                                                        Toast.LENGTH_SHORT
                                                        ).show()
                                                );
                                                return false;
                                            }
                                        }, (result) -> { if (result) getParentFragmentManager().popBackStack(); }
                                );
                            }
                    );
                    dialog.show();
                }
        );
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
            if (grantResults.length == 1 && grantResults[0] == PERMISSION_GRANTED) {
                buttonOfRecording.performClick();
            } else {
                Toast.makeText(requireActivity(), R.string.need_permissions, Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void changeCounter(int newCounter) {
        recordingStatusImages.get(newCounter - 1).setImageResource(R.mipmap.ok_on);
        counter = newCounter;
    }

    private void initCounters(View rootView) {
        recordingStatusImages = new ArrayList<>();
        recordingStatusImages.add((ImageView) rootView.findViewById(R.id.firstCounter));
        recordingStatusImages.add((ImageView) rootView.findViewById(R.id.secondCounter));
        recordingStatusImages.add((ImageView) rootView.findViewById(R.id.thirdCounter));
    }
}