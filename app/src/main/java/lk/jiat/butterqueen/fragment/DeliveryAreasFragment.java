package lk.jiat.butterqueen.fragment;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

import lk.jiat.butterqueen.databinding.FragmentDeliveryAreasBinding;

public class DeliveryAreasFragment extends Fragment implements OnMapReadyCallback {

    private FragmentDeliveryAreasBinding binding;
    private GoogleMap googleMap;
    private Marker selectedMarker;

    // Kiribathgoda approximate center
    private static final LatLng KIRIBATHGODA = new LatLng(6.977828, 79.927152);

    private static final double FIRST_BAND_METERS = 10_000;
    private static final double SECOND_BAND_METERS = 20_000;
    private static final double THIRD_BAND_METERS = 30_000;

    private static final double FIRST_BAND_PRICE = 800.00;
    private static final double SECOND_BAND_PRICE = 1500.00;
    private static final double THIRD_BAND_PRICE = 2500.00;

    public DeliveryAreasFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDeliveryAreasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(binding.mapContainer.getId());

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(binding.mapContainer.getId(), mapFragment)
                    .commitNow();
        }

        mapFragment.getMapAsync(this);

        binding.centerMapButton.setOnClickListener(v -> moveCameraToCenter());

        binding.clearSelectionButton.setOnClickListener(v -> clearSelection());

        showDefaultInfo();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);

        googleMap.addMarker(
                new MarkerOptions()
                        .position(KIRIBATHGODA)
                        .title("Kiribathgoda")
                        .snippet("Delivery center")
        );

        drawDeliveryCircles();
        moveCameraToCenter();

        googleMap.setOnMapClickListener(this::handleMapTap);
    }

    private void drawDeliveryCircles() {
        if (googleMap == null) return;

        googleMap.addCircle(new CircleOptions()
                .center(KIRIBATHGODA)
                .radius(FIRST_BAND_METERS)
                .strokeWidth(4f)
                .strokeColor(Color.parseColor("#27AE60"))
                .fillColor(Color.parseColor("#5527AE60")));

        googleMap.addCircle(new CircleOptions()
                .center(KIRIBATHGODA)
                .radius(SECOND_BAND_METERS)
                .strokeWidth(4f)
                .strokeColor(Color.parseColor("#F9A825"))
                .fillColor(Color.parseColor("#55F9A825")));

        googleMap.addCircle(new CircleOptions()
                .center(KIRIBATHGODA)
                .radius(THIRD_BAND_METERS)
                .strokeWidth(4f)
                .strokeColor(Color.parseColor("#E53935"))
                .fillColor(Color.parseColor("#55E53935")));
    }

    private void moveCameraToCenter() {
        if (googleMap == null) return;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(KIRIBATHGODA, 11f));
    }

    private void handleMapTap(@NonNull LatLng tappedPoint) {
        if (googleMap == null) return;

        if (selectedMarker != null) {
            selectedMarker.remove();
        }

        DeliveryBandResult result = calculateCharge(tappedPoint);

        selectedMarker = googleMap.addMarker(
                new MarkerOptions()
                        .position(tappedPoint)
                        .title(result.isWithinArea ? "Delivery available" : "Outside delivery area")
                        .snippet(result.isWithinArea
                                ? "Charge: " + String.format(Locale.getDefault(), "Rs. %.2f", result.price)
                                : "More than 30 km from Kiribathgoda")
        );

        if (selectedMarker != null) {
            selectedMarker.showInfoWindow();
        }

        binding.distanceTextView.setText(
                String.format(Locale.getDefault(), "Distance: %.2f km", result.distanceKm)
        );

        binding.selectedPointTextView.setText(
                String.format(
                        Locale.getDefault(),
                        "Selected point: %.5f, %.5f",
                        tappedPoint.latitude,
                        tappedPoint.longitude
                )
        );

        if (result.isWithinArea) {
            binding.statusTextView.setText("Inside delivery area • " + result.bandLabel);
            binding.priceTextView.setText(
                    String.format(Locale.getDefault(), "Charge: Rs. %.2f", result.price)
            );
            binding.statusTextView.setTextColor(Color.parseColor("#0D47A1"));
        } else {
            binding.statusTextView.setText("Outside delivery area");
            binding.priceTextView.setText("Charge: Not available");
            binding.statusTextView.setTextColor(Color.parseColor("#B71C1C"));
        }
    }

    private DeliveryBandResult calculateCharge(@NonNull LatLng point) {
        float[] results = new float[1];
        Location.distanceBetween(
                KIRIBATHGODA.latitude,
                KIRIBATHGODA.longitude,
                point.latitude,
                point.longitude,
                results
        );

        double distanceMeters = results[0];
        double distanceKm = distanceMeters / 1000.0;

        if (distanceMeters <= FIRST_BAND_METERS) {
            return new DeliveryBandResult(true, "0 - 10 km", FIRST_BAND_PRICE, distanceKm);
        } else if (distanceMeters <= SECOND_BAND_METERS) {
            return new DeliveryBandResult(true, "10 - 20 km", SECOND_BAND_PRICE, distanceKm);
        } else if (distanceMeters <= THIRD_BAND_METERS) {
            return new DeliveryBandResult(true, "20 - 30 km", THIRD_BAND_PRICE, distanceKm);
        } else {
            return new DeliveryBandResult(false, "Over 30 km", 0.0, distanceKm);
        }
    }

    private void clearSelection() {
        if (selectedMarker != null) {
            selectedMarker.remove();
            selectedMarker = null;
        }
        showDefaultInfo();
        moveCameraToCenter();
    }

    private void showDefaultInfo() {
        binding.statusTextView.setText("No location selected yet");
        binding.statusTextView.setTextColor(Color.parseColor("#0D47A1"));
        binding.distanceTextView.setText("Distance: -");
        binding.priceTextView.setText("Charge: -");
        binding.selectedPointTextView.setText("Selected point: -");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class DeliveryBandResult {
        final boolean isWithinArea;
        final String bandLabel;
        final double price;
        final double distanceKm;

        DeliveryBandResult(boolean isWithinArea, String bandLabel, double price, double distanceKm) {
            this.isWithinArea = isWithinArea;
            this.bandLabel = bandLabel;
            this.price = price;
            this.distanceKm = distanceKm;
        }
    }
}