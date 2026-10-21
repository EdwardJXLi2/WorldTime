# World map data

The map bundles public-domain [Natural Earth](https://www.naturalearthdata.com/about/terms-of-use/) data from release `v5.1.2`:

- [Land at 1:110 million](https://github.com/nvkelso/natural-earth-vector/blob/v5.1.2/geojson/ne_110m_land.geojson), stored in `src/main/res/drawable/world_map_land.xml`.
- [Time zones at 1:10 million](https://github.com/nvkelso/natural-earth-vector/blob/v5.1.2/geojson/ne_10m_time_zones.geojson), stored in `src/main/res/drawable/world_map_time_zones.xml`.

The vectors use an equirectangular projection with a 360 by 180 viewport. Coordinates map longitude to `longitude + 180` and latitude to `90 - latitude`.

Land polygons use topology-preserving simplification with a 0.15-degree tolerance. Time zone boundaries are combined, merged, and simplified with a 0.2-degree tolerance. Coordinates are rounded to two decimal places.

The boundaries provide a geographic overview. They do not represent current legal boundaries or daylight saving changes. City clocks continue to use Android time zone rules.

Daylight shading uses the selected instant and the [NOAA solar position approximation](https://gml.noaa.gov/grad/solcalc/solareqns.PDF). Shading increases at solar altitudes of 0, −6, and −12 degrees. Atmospheric refraction and local terrain are excluded.
