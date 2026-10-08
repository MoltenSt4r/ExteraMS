package app.exteraless.camera;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Size;
import android.util.SizeF;

import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CameraLensStops {

    private static final float[] NO_RATIOS = new float[0];
    private static final float[] SNAP_RATIOS = {1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 10f, 12f, 15f, 20f};
    private static final float[] ROUND_RATIOS = {1.5f, 2f, 3f, 5f, 10f, 15f, 20f, 30f};
    private static final float[] RULER_LADDER = {1f, 2f, 5f, 10f, 30f};
    private static final float EPSILON = 1.0E-4f;
    private static final Map<String, float[]> RATIO_CACHE = new HashMap<>();

    private CameraLensStops() {
    }

    public static float[] opticalZoomRatios(Context context, String cameraId, float minZoom) {
        if (context == null || cameraId == null || Build.VERSION.SDK_INT < 28) {
            return NO_RATIOS;
        }
        float[] ratios;
        synchronized (RATIO_CACHE) {
            ratios = RATIO_CACHE.get(cameraId);
        }
        if (ratios == null) {
            ratios = readOpticalZoomRatios(context, cameraId);
            synchronized (RATIO_CACHE) {
                RATIO_CACHE.put(cameraId, ratios);
            }
        }
        return normalizeRatios(ratios, minZoom);
    }

    public static float[] buildToggleStops(float min, float max, float[] opticalRatios) {
        float[] telephoto = telephotoRatios(opticalRatios, max);
        if (telephoto.length == 0) {
            return null;
        }
        ArrayList<Float> stops = new ArrayList<>(6);
        if (min < 0.9999f) {
            addDistinctStop(stops, min);
        }
        addDistinctStop(stops, clamp(1f, min, max));
        for (float ratio : telephoto) {
            addDistinctStop(stops, ratio);
        }
        fillWideGaps(stops);
        addReachStop(stops, telephoto[telephoto.length - 1], max);
        dropCrowdedStops(stops);
        return toArray(stops);
    }

    public static float[] buildRulerStops(float min, float max, float[] toggles) {
        ArrayList<Float> stops = new ArrayList<>(toggles.length + RULER_LADDER.length + 2);
        addDistinctStop(stops, min);
        for (float toggle : toggles) {
            addDistinctStop(stops, clamp(toggle, min, max));
        }
        addDistinctStop(stops, max);
        for (float step : RULER_LADDER) {
            if (step >= min - EPSILON && step <= max + EPSILON && nearestOctaveDistance(stops, step) >= 0.55) {
                addDistinctStop(stops, step);
            }
        }
        return toArray(stops);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double octaves(float from, float to) {
        if (from <= 0f || to <= 0f) {
            return 0;
        }
        return Math.log(to / from) / Math.log(2);
    }

    private static void addDistinctStop(ArrayList<Float> stops, float value) {
        if (value <= 0f || !Float.isFinite(value)) {
            return;
        }
        for (int i = 0; i < stops.size(); i++) {
            float existing = stops.get(i);
            if (Math.abs(existing - value) <= EPSILON) {
                return;
            }
            if (existing > value) {
                stops.add(i, value);
                return;
            }
        }
        stops.add(value);
    }

    private static float[] toArray(ArrayList<Float> stops) {
        float[] result = new float[stops.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = stops.get(i);
        }
        return result;
    }

    private static float snapToNiceRatio(float value) {
        if (!Float.isFinite(value) || value <= 0f) {
            return 0f;
        }
        float best = 0f;
        float bestDistance = Float.MAX_VALUE;
        for (float nice : SNAP_RATIOS) {
            float distance = (nice - value) / value;
            if (distance >= -0.03f && distance <= 0.1f && Math.abs(distance) < bestDistance) {
                best = nice;
                bestDistance = Math.abs(distance);
            }
        }
        return best > 0f ? best : Math.round(value * 10f) / 10f;
    }

    private static float[] telephotoRatios(float[] ratios, float max) {
        if (ratios == null || ratios.length == 0) {
            return NO_RATIOS;
        }
        ArrayList<Float> result = new ArrayList<>(ratios.length);
        for (float ratio : ratios) {
            if (ratio >= 1.15f && ratio <= max + EPSILON) {
                addDistinctStop(result, ratio);
            }
        }
        return toArray(result);
    }

    private static double nearestOctaveDistance(ArrayList<Float> stops, float value) {
        double nearest = Double.MAX_VALUE;
        for (int i = 0; i < stops.size(); i++) {
            nearest = Math.min(nearest, Math.abs(octaves(stops.get(i), value)));
        }
        return nearest;
    }

    private static float chooseRoundRatio(float from, float to) {
        double middle = Math.sqrt(from * to);
        float best = 0f;
        double bestDistance = Double.MAX_VALUE;
        for (float round : ROUND_RATIOS) {
            if (round > from + EPSILON && round < to - EPSILON) {
                double distance = Math.abs(octaves(round, (float) middle));
                if (distance < bestDistance) {
                    best = round;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static void fillWideGaps(ArrayList<Float> stops) {
        while (stops.size() < 4) {
            int widest = -1;
            double widestGap = 2;
            for (int i = 1; i < stops.size(); i++) {
                double gap = octaves(stops.get(i - 1), stops.get(i));
                if (gap > widestGap) {
                    widest = i;
                    widestGap = gap;
                }
            }
            if (widest < 0) {
                return;
            }
            float round = chooseRoundRatio(stops.get(widest - 1), stops.get(widest));
            if (round <= 0f) {
                return;
            }
            int size = stops.size();
            addDistinctStop(stops, round);
            if (stops.size() == size) {
                return;
            }
        }
    }

    private static void addReachStop(ArrayList<Float> stops, float longestLens, float max) {
        if (stops.size() >= 4 || stops.isEmpty()) {
            return;
        }
        float reach = snapToNiceRatio(longestLens * 2f);
        if (reach > max + EPSILON || reach <= stops.get(stops.size() - 1) + EPSILON) {
            return;
        }
        addDistinctStop(stops, reach);
    }

    private static void dropCrowdedStops(ArrayList<Float> stops) {
        while (stops.size() > 5) {
            int crowded = -1;
            double smallestGap = Double.MAX_VALUE;
            for (int i = 1; i < stops.size() - 1; i++) {
                double gap = octaves(stops.get(i - 1), stops.get(i));
                if (gap < smallestGap) {
                    crowded = i;
                    smallestGap = gap;
                }
            }
            if (crowded < 0) {
                return;
            }
            stops.remove(crowded);
        }
    }

    private static float[] normalizeRatios(float[] ratios, float minZoom) {
        if (ratios.length < 2) {
            return NO_RATIOS;
        }
        float scale = minZoom > 0f && ratios[0] > minZoom * 1.25f ? minZoom / ratios[0] : 1f;
        float base = 0f;
        double baseDistance = Double.MAX_VALUE;
        for (float ratio : ratios) {
            float scaled = ratio * scale;
            double distance = Math.abs(octaves(1f, scaled));
            if (distance < baseDistance) {
                base = scaled;
                baseDistance = distance;
            }
        }
        if (Math.abs(base - 1f) > 0.12f) {
            return NO_RATIOS;
        }
        ArrayList<Float> result = new ArrayList<>(ratios.length);
        for (float ratio : ratios) {
            addDistinctStop(result, snapToNiceRatio(ratio * scale / base));
        }
        return toArray(result);
    }

    private static float halfFieldOfViewTangent(CameraCharacteristics characteristics) {
        float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        SizeF physicalSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        Rect activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Size pixelArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (focalLengths == null || focalLengths.length == 0 || physicalSize == null || activeArray == null || pixelArray == null || orientation == null) {
            return 0f;
        }
        boolean rotated = orientation % 180 == 90;
        float sensorWidth = rotated ? physicalSize.getHeight() : physicalSize.getWidth();
        float activeWidth = rotated ? activeArray.height() : activeArray.width();
        float pixelWidth = rotated ? pixelArray.getHeight() : pixelArray.getWidth();
        float focalLength = focalLengths[0];
        if (focalLength <= 0f || sensorWidth <= 0f || activeWidth <= 0f || pixelWidth <= 0f) {
            return 0f;
        }
        return sensorWidth * activeWidth / pixelWidth / (focalLength * 2f);
    }

    private static float[] readOpticalZoomRatios(Context context, String cameraId) {
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) {
                return NO_RATIOS;
            }
            CameraCharacteristics logical = manager.getCameraCharacteristics(cameraId);
            Set<String> physicalIds = logical.getPhysicalCameraIds();
            if (physicalIds == null || physicalIds.size() < 2) {
                return NO_RATIOS;
            }
            float logicalTangent = halfFieldOfViewTangent(logical);
            if (logicalTangent <= 0f) {
                return NO_RATIOS;
            }
            ArrayList<Float> ratios = new ArrayList<>(physicalIds.size());
            for (String physicalId : physicalIds) {
                try {
                    float tangent = halfFieldOfViewTangent(manager.getCameraCharacteristics(physicalId));
                    if (tangent > 0f) {
                        addDistinctStop(ratios, logicalTangent / tangent);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return toArray(ratios);
        } catch (Exception e) {
            FileLog.e(e);
            return NO_RATIOS;
        }
    }
}
