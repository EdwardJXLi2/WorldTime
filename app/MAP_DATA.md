# World map data

The map bundles public-domain [Natural Earth](https://www.naturalearthdata.com/about/terms-of-use/) data from release `v5.1.2`:

- [Land at 1:110 million](https://github.com/nvkelso/natural-earth-vector/blob/v5.1.2/geojson/ne_110m_land.geojson), stored in `src/main/res/drawable/world_map_land.xml`.
- [Time zones at 1:10 million](https://github.com/nvkelso/natural-earth-vector/blob/v5.1.2/geojson/ne_10m_time_zones.geojson), stored in `src/main/res/drawable/world_map_time_zones.xml`.

`src/main/res/drawable/world_map_zone_regions.xml` groups the same time zone polygons by standard UTC offset in minutes. Each group is combined and simplified with a 0.2-degree tolerance.

The map highlights the band matching the home zone's standard offset at the selected instant. The highlight includes other regions with that offset. Daylight saving does not shift the highlight to a neighboring band.

The vectors use an equirectangular projection with a 360 by 180 viewport. Coordinates map longitude to `longitude + 180` and latitude to `90 - latitude`.

Land polygons use topology-preserving simplification with a 0.15-degree tolerance. Time zone boundaries are combined, merged, and simplified with a 0.2-degree tolerance. Coordinates are rounded to two decimal places.

The boundaries provide a geographic overview. They do not represent current legal boundaries or daylight saving changes. City clocks continue to use Android time zone rules.

Daylight shading uses the selected instant and the [NOAA solar position approximation](https://gml.noaa.gov/grad/solcalc/solareqns.PDF). Shading increases at solar altitudes of 0, −6, and −12 degrees. Atmospheric refraction and local terrain are excluded.

The home city marker uses public-domain [IANA time zone coordinates](https://www.iana.org/time-zones/theory), bundled in `src/main/res/raw/time_zone_locations.csv`. Coordinates come from `zone.tab` in tzdb `2026e`. Aliases from `tzdata.zi` use their target coordinates when no separate city coordinates exist. Fixed-offset zones have no city marker. The marker identifies the zone's reference city, not the device's GPS location.

The Sun and Moon icons mark the approximate points where each body is overhead at the selected instant. Lunar coordinates use [Paul Schlyter's orbital model and principal perturbations](https://www.stjarnhimlen.se/comp/ppcomp.html). The crescent symbol identifies the Moon and does not indicate its phase. Overlapping celestial icons use a short line to retain their actual map positions.
