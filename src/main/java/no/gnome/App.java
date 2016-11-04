package no.gnome;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jooby.Jooby;
import org.jooby.hbm.Hbm;
import org.jooby.hbm.UnitOfWork;
import org.jooby.json.Jackson;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.jooby.Request;

/**
 * @author jooby generator
 */
public class App extends Jooby {

    private final static String FILENAME = "worldcitiespop.txt";

    Cache<String, List<City>> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    {
            
        use(new Jackson());

        use(new Hbm("jdbc:postgresql://localhost/worldcities")
                .classes(City.class)
        );

        get("/", (req, rsp) -> {
            String data = "<head><title>gnome.no</title></head>Hello Jooby World!";
            rsp.status(200)
                .type("text/html")
                .send(data);
        });

        get("/favicon.ico", () -> "");

        get("/cities", (req) -> {
            
            String key = "cities";
            List<City> locations = cache.getIfPresent(key);

            if (locations == null) {
                locations = require(UnitOfWork.class).apply(em -> {
                    return em.createQuery("from City c")
                            .setMaxResults(100)
                            .getResultList();
                });
                cache.put(key, locations);
            }
            
            return locations;
        });
        
        get("/cities/:name", req -> {
            return require(UnitOfWork.class).apply(em -> {
                return em.createQuery("from City c where c.name = :name")
                    .setParameter("name", req.param("name").value())
                    .getResultList();
            });
        });

        get("/cities/:latitude/:longitude", req -> {
            return this.locate_points_in_circle(req);
        });

        get("/cities/:latitude/:longitude/:radius", req -> {
            req.set("radius", true);
            return this.locate_points_in_circle(req);
        });

        get("/import", () -> {
            return require(UnitOfWork.class).apply(em -> {
                List<City> cities = em.createQuery("from City c").setMaxResults(1).getResultList();
                if (cities.size() == 1) {
                    return "Data have already been imported!";
                } else {
                    Path path = Paths.get(no.gnome.App.FILENAME);
                    try (Stream<String> stream = Files.lines(path)) {
                        stream
                                .filter(line -> !line.startsWith("Country"))
                                //.limit(5)
                                .forEach(r -> {
                                    City city = new City(r);
                                    try {
                                        require(UnitOfWork.class).accept(em2 -> {
                                            em2.persist(city);
                                        });
                                    } catch (Throwable ex) {
                                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                });
                    } catch (IOException e) {
                        throw new FileNotFoundException(path.toString());
                    }
                    return "Imported data";
                }
            });
        });

    }

    private List<City> locate_points_in_circle(Request req) throws Throwable {
        Float latitude;
        Float longitude;
        Integer radius;
        Integer defaultRadius = 20;

        // Try extracting latitude from param().
        try {
            latitude = Float.parseFloat(req.param("latitude").value());
        } catch (NumberFormatException e) {
            latitude = Float.parseFloat("61.7428745");
        }

        // Try extracting longitude from param().
        try {
            longitude = Float.parseFloat(req.param("longitude").value());
        } catch (NumberFormatException e) {
            longitude = Float.parseFloat("6.3968833");
        }

        // Try extracting radius (in kilometer) from param().
        if (req.ifGet("radius").isPresent()) {
            try {
                radius = Integer.parseInt(req.param("radius").value());
            } catch (NumberFormatException e) {
                radius = defaultRadius;
            }
        } else {
            radius = defaultRadius;
        }
        
        // Key used to lookup in Caffeine cache.
        String key = latitude.toString() + longitude.toString() + radius.toString();
        List<City> locations = cache.getIfPresent(key);

        if (locations == null) {
            // Define center.
            LatLng center = new LatLng(latitude, longitude);

            // Get latitude west and east of center.
            LatLng latitude_west = LatLngTool.travel(center, 180, radius, LengthUnit.KILOMETER);
            LatLng latitude_east = LatLngTool.travel(center, 0, radius, LengthUnit.KILOMETER);

            // Get longitude south and north of center.
            LatLng longitude_south = LatLngTool.travel(center, 270, radius, LengthUnit.KILOMETER);
            LatLng longitude_north = LatLngTool.travel(center, 90, radius, LengthUnit.KILOMETER);

            // Get locations within a square.
            locations = require(UnitOfWork.class).apply(em -> {
                return em.createQuery("from City c where c.latitude between :latitude_west and :latitude_east and c.longitude between :longitude_low and :longitude_high")
                        .setParameter("latitude_west", (float) latitude_west.getLatitude())
                        .setParameter("latitude_east", (float) latitude_east.getLatitude())
                        .setParameter("longitude_low", (float) longitude_south.getLongitude())
                        .setParameter("longitude_high", (float) longitude_north.getLongitude())
                        .getResultList();
            });

            // Define iterator that will traverse the list and remove locations outside the radius.
            Iterator<City> locationsIterator = locations.iterator();
            while (locationsIterator.hasNext()) {
                City city = locationsIterator.next();
                LatLng endpoint = new LatLng(city.getLatitude(), city.getLongitude());
                double distance = LatLngTool.distance(center, endpoint, LengthUnit.KILOMETER);
                if (distance > radius) {
                    locationsIterator.remove();
                }
            }

            cache.put(key, locations);
        }

        return locations;
    }
    
    public static void main(final String[] args) {
        run(App::new, args);
    }

}
