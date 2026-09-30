package com.c2.ew.agent.geojson;

import com.c2.ew.agent.dto.TacticalEmissionSummaryDto;
import com.c2.ew.domain.model.ErrorEllipse;
import com.c2.ew.domain.model.GeoPoint;
import com.c2.ew.util.GeoUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TacticalGeoJsonBuilder {

    private static final double DEFAULT_LOB_LENGTH_METERS = 45_000.0;
    private static final int ELLIPSE_STEPS = 36;

    public Map<String, Object> buildEmissionsFeatureCollection(Collection<TacticalEmissionSummaryDto> emissions) {
        List<Map<String, Object>> features = new ArrayList<>();

        if (emissions != null) {
            for (TacticalEmissionSummaryDto emi : emissions) {
                if (emi.yayinEnlem() != null && emi.yayinBoylam() != null) {
                    features.add(createEmissionPointFeature(emi));
                    if (emi.yayinSemiMajorMeters() != null && emi.yayinSemiMinorMeters() != null) {
                        features.add(createErrorEllipsePolygonFeature(emi));
                    }
                }

                // Sensör kerteriz yön çizgisi
                if (emi.ehUnsuruEnlem() != null && emi.ehUnsuruBoylam() != null && emi.yon() != null) {
                    features.add(createBearingLineFeature(emi));
                }
            }
        }

        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", features);
        return collection;
    }

    private Map<String, Object> createEmissionPointFeature(TacticalEmissionSummaryDto emi) {
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("id", emi.id());

        Map<String, Object> geom = new LinkedHashMap<>();
        geom.put("type", "Point");
        geom.put("coordinates", List.of(emi.yayinBoylam(), emi.yayinEnlem()));
        feature.put("geometry", geom);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("layerType", "EMISSION_TARGET");
        props.put("emissionId", emi.id());
        props.put("radarAdi", emi.radarAdi());
        props.put("teshisKimlik", emi.teshisKimlik());
        props.put("veriKaynagi", emi.veriKaynagi());
        props.put("hss", emi.hss());
        props.put("taciz", emi.taciz());
        props.put("etUygulamaDurumu", emi.etUygulamaDurumu());
        props.put("minFrekansMhz", emi.minFrekansMhz());
        props.put("maxFrekansMhz", emi.maxFrekansMhz());
        props.put("hedefYerBilgisi", emi.hedefYerBilgisi());
        feature.put("properties", props);

        return feature;
    }

    private Map<String, Object> createErrorEllipsePolygonFeature(TacticalEmissionSummaryDto emi) {
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("id", emi.id() + "-ellipse");

        GeoPoint center = new GeoPoint(emi.yayinEnlem(), emi.yayinBoylam());
        ErrorEllipse ellipse = new ErrorEllipse(
            emi.yayinSemiMajorMeters(),
            emi.yayinSemiMinorMeters(),
            emi.yayinOrientationDegrees() != null ? emi.yayinOrientationDegrees() : 0.0,
            95
        );

        List<List<Double>> ring = GeoUtils.generateEllipsePolygon(center, ellipse, ELLIPSE_STEPS);

        Map<String, Object> geom = new LinkedHashMap<>();
        geom.put("type", "Polygon");
        geom.put("coordinates", List.of(ring));
        feature.put("geometry", geom);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("layerType", "ERROR_ELLIPSE");
        props.put("emissionId", emi.id());
        props.put("teshisKimlik", emi.teshisKimlik());
        feature.put("properties", props);

        return feature;
    }

    private Map<String, Object> createBearingLineFeature(TacticalEmissionSummaryDto emi) {
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("id", emi.id() + "-bearing");

        GeoPoint origin = new GeoPoint(emi.ehUnsuruEnlem(), emi.ehUnsuruBoylam());
        GeoPoint end = GeoUtils.projectDestination(origin, emi.yon(), DEFAULT_LOB_LENGTH_METERS);

        Map<String, Object> geom = new LinkedHashMap<>();
        geom.put("type", "LineString");
        geom.put("coordinates", List.of(
            List.of(origin.longitude(), origin.latitude()),
            List.of(end.longitude(), end.latitude())
        ));
        feature.put("geometry", geom);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("layerType", "BEARING_RAY");
        props.put("emissionId", emi.id());
        props.put("ehUnsuru", emi.ehUnsuru());
        props.put("bearingDegrees", emi.yon());
        feature.put("properties", props);

        return feature;
    }
}
