package com.idrnd.idvoice.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.ui.dialogs.ContinuousVerifyDialog;
import com.idrnd.idvoice.ui.dialogs.StatisticsDialog;
import com.idrnd.idvoice.ui.dialogs.speechrecord.SpeechEndpointRecordDialog;
import com.idrnd.idvoice.ui.dialogs.speechrecord.SpeechRecordDialog;
import com.idrnd.idvoice.utils.Prefs;
import com.idrnd.idvoice.utils.logs.FileUtils;
import com.idrnd.idvoice.utils.verification.EngineManager;
import com.idrnd.idvoice.utils.verification.VerificationRunner;

import net.idrnd.voicesdk.verify.QualityShortDescription;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.idrnd.idvoice.IDVoiceApplication.singleTaskRunner;
import static com.idrnd.idvoice.ui.activity.MainActivity.showToastWithSpeechRecordQualityStatus;
import static com.idrnd.idvoice.utils.Prefs.VoiceTemplateType.TextIndependent;
import static net.idrnd.voicesdk.verify.QualityShortDescription.OK;

public class StartFragment extends Fragment {

    public static final String VERIFICATION_SCORE = "VERIFICATION_SCORE";
    public static final String LIVENESS_SCORE = "LIVENESS_SCORE";
    public static final int PERMISSION_REQUEST = 44;

    private AlertDialog dialog = null;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.enrollTextDependentButton).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new EnrollFragment())
                        .addToBackStack(null)
                        .commit()
        );

        view.findViewById(R.id.verifyTextDependentButton).setOnClickListener(v ->
                phraseVerify(Prefs.VoiceTemplateType.TextDependent)
        );

        view.findViewById(R.id.enrollTextIndependentButton).setOnClickListener(v -> {

                    if (ContextCompat.checkSelfPermission(requireContext(), RECORD_AUDIO) != PERMISSION_GRANTED) {
                        requestPermissions(new String[]{ RECORD_AUDIO}, PERMISSION_REQUEST);
                        return;
                    }

                    dialog = new SpeechRecordDialog(
                            requireContext(),
                            getString(R.string.message_for_ti_enroll)
                    );

                    ((SpeechRecordDialog) dialog).setOnStopRecordingListener(audioRecord -> {
                                dialog.dismiss();
                                setUiEnabled(false);

                                if(Prefs.getInstance().getQualityCheckEnabled()) {
                                    QualityShortDescription qualityStatus = EngineManager.getInstance()
                                            .getVerifyEngine(TextIndependent)
                                            .checkQuality(audioRecord);

                                    if(qualityStatus != OK) {
                                        showToastWithSpeechRecordQualityStatus(requireContext(), qualityStatus);
                                        setUiEnabled(true);
                                        return;
                                    }
                                }

                                showLoader();

                                FileUtils.getInstance().saveAudioRecordWithLog(audioRecord);

                                singleTaskRunner.execute( () -> {
                                            // Create voice template
                                            byte[] template = EngineManager.getInstance()
                                                    .getVerifyEngine(TextIndependent)
                                                    .createVoiceTemplate(audioRecord)
                                                    .serialize();

                                            // Save voice template in shared preference
                                            Prefs.getInstance().setVoiceTemplateSync(template, TextIndependent);
                                            return "";
                                        }, (result) -> {
                                            setUiEnabled(true);
                                            updateVerifyButtonsEnabled();
                                            hideLoader();
                                            Toast.makeText(requireActivity(), R.string.enrollment_done, Toast.LENGTH_SHORT).show();
                                        }
                                );
                            }
                    );
                    dialog.show();
                }
        );

        view.findViewById(R.id.verifyTextIndependentButton).setOnClickListener(v ->
                phraseVerify(TextIndependent));

        view.findViewById(R.id.continuousVerifyButton).setOnClickListener(v -> {
                    setUiEnabled(false);
                    dialog = new ContinuousVerifyDialog(requireContext());
                    dialog.setOnCancelListener((dialogInterface) -> setUiEnabled(true));
                    dialog.setOnDismissListener((dialogInterface) -> setUiEnabled(true));
                    dialog.show();
                }
        );

        view.findViewById(R.id.imageSettings).setOnClickListener(v ->
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer , new SettingsFragment())
                        .addToBackStack(null)
                        .commit()
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        updateVerifyButtonsEnabled();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (dialog != null) dialog.dismiss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if ((grantResults.length == 1) && (grantResults[0] == PERMISSION_GRANTED)) {
                getView().findViewById(R.id.enrollTextIndependentButton).performClick();
            } else {
                Toast.makeText(requireActivity(), R.string.need_permissions, Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void phraseVerify(Prefs.VoiceTemplateType voiceTemplateType) throws IllegalStateException {
        // Disable UI for audio recording
        setUiEnabled(false);

        // Choose phrase by verification type
        final String messageForUser;

        switch (voiceTemplateType) {
            case TextIndependent:
                messageForUser = getString(R.string.message_for_ti_verify);
                break;
            case TextDependent:
                messageForUser = getString(R.string.message_for_td_enroll);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + voiceTemplateType);
        }

        dialog = new SpeechEndpointRecordDialog(
                requireContext(),
                messageForUser
        );

        ((SpeechEndpointRecordDialog) dialog).setOnStopRecordingListener(audioRecord -> {
                    dialog.dismiss();

                    if(Prefs.getInstance().getQualityCheckEnabled()) {
                        QualityShortDescription qualityStatus = EngineManager.getInstance()
                                .getVerifyEngine(Prefs.VoiceTemplateType.TextDependent)
                                .checkQuality(audioRecord);

                        if(qualityStatus != OK) {
                            showToastWithSpeechRecordQualityStatus(requireContext(), qualityStatus);
                            setUiEnabled(true);
                            return;
                        }
                    }

                    showLoader();

                    new VerificationRunner(Prefs.getInstance().getLivenessCheckEnabled()).execute(
                            audioRecord,
                            voiceTemplateType,
                            (results) -> {
                                hideLoader();
                                setUiEnabled(true);

                                // Make bundle with verify and liveness scores
                                final Bundle bundle = new Bundle();
                                bundle.putFloat(VERIFICATION_SCORE, results.first.getProbability());

                                float livenessScore = -1f;
                                if (results.second != null) {
                                    livenessScore = results.second.getScore();
                                    bundle.putFloat(LIVENESS_SCORE, livenessScore);
                                }

                                FileUtils.getInstance().saveAudioRecordWithLog(
                                        audioRecord,
                                        results.first.getProbability(),
                                        results.first.getScore(),
                                        livenessScore);

                                // Show results dialog
                                StatisticsDialog statisticDialog = StatisticsDialog.newInstance(bundle);
                                statisticDialog.show(getParentFragmentManager(), null);
                            }
                    );
                }
        );

        dialog.show();
    }

    private void updateVerifyButtonsEnabled() {
        // Make "Verify" buttons disabled if there is no saved template
        boolean textDependentVerifyButtonEnabled =
                !(Prefs.getInstance().getVoiceTemplate(Prefs.VoiceTemplateType.TextDependent) == null);

        getView().findViewById(R.id.verifyTextDependentButton).setEnabled(textDependentVerifyButtonEnabled);

        boolean textIndependentVerifyButtonEnabled =
                !(Prefs.getInstance().getVoiceTemplate(TextIndependent) == null);

        getView().findViewById(R.id.verifyTextIndependentButton).setEnabled(textIndependentVerifyButtonEnabled);
        getView().findViewById(R.id.continuousVerifyButton).setEnabled(textIndependentVerifyButtonEnabled);
    }

    private void setUiEnabled(boolean enabled) {
        getView().findViewById(R.id.imageSettings).setEnabled(enabled);
        getView().findViewById(R.id.enrollTextIndependentButton).setEnabled(enabled);
        getView().findViewById(R.id.enrollTextDependentButton).setEnabled(enabled);
        getView().findViewById(R.id.verifyTextDependentButton).setEnabled(enabled);
        getView().findViewById(R.id.verifyTextIndependentButton).setEnabled(enabled);
        getView().findViewById(R.id.continuousVerifyButton).setEnabled(enabled);
        if(enabled) {
            updateVerifyButtonsEnabled();
        }
    }

    private void showLoader() { progressBar.setVisibility(View.VISIBLE); }

    private void hideLoader() { progressBar.setVisibility(View.GONE); }
}