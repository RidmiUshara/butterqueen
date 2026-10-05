package lk.jiat.butterqueen.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.data.ContactActions;

public final class HelpFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_help, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        view.findViewById(R.id.helpCall).setOnClickListener(v ->
                ContactActions.call(requireContext()));
        view.findViewById(R.id.helpEmail).setOnClickListener(v ->
                ContactActions.email(requireContext(), "ButterQueen order help"));
    }
}
