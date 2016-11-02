package no.gnome;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jooby.Jooby;
import org.jooby.Mutant;
import org.jooby.hbm.Hbm;
import org.jooby.hbm.UnitOfWork;
import org.jooby.json.Jackson;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

/**
 * @author jooby generator
 */
public class App extends Jooby {

    private final static String FILENAME = "worldcitiespop.txt";

    {
        get("/", (req, rsp) -> {
            String data = "<head><title>gnome.no</title></head>Hello Jooby World!";
            rsp.status(200)
                .type("text/html")
                .send(data);
        });

        get("/favicon.ico", () -> "");

        use(new Jackson());

        use(new Hbm("jdbc:postgresql://localhost/worldcities")
                .classes(City.class)
        );

        get("/cities", () -> {
            return require(UnitOfWork.class).apply(em -> {
                return em.createQuery("from City c")
                    .setMaxResults(100)
                    .getResultList();
            });
        });
        
        get("/cities/:name", req -> {
            return require(UnitOfWork.class).apply(em -> {
                return em.createQuery("from City c where c.name = :name")
                    .setParameter("name", req.param("name").value())
                    .getResultList();
            });
        });
        
        get("/cities/:latitude/:longitude", req -> {
            Integer distanceKM = 20;
            String lat = "latitude";
            String lon = "longitude";
            Float latitude;
            Float longitude;
            
            // Try extracting latitude from params.
            try {
                latitude = Float.parseFloat(req.param(lat).value());
            } catch (NumberFormatException e) {
                latitude = Float.parseFloat("61.7428745");
            }
            
            // Try extracting longitude from params.
            try {
                longitude = Float.parseFloat(req.param(lon).value());
            } catch (NumberFormatException e) {
                longitude = Float.parseFloat("6.3968833");
            }
            
            // Define center.
            LatLng center = new LatLng(latitude, longitude);

            // Get latitude west and east of center.
            LatLng latitude_west = LatLngTool.travel(center, 180, distanceKM, LengthUnit.KILOMETER);
            LatLng latitude_east = LatLngTool.travel(center, 0, distanceKM, LengthUnit.KILOMETER);
            
            // Get longitude south and north of center.
            LatLng longitude_south = LatLngTool.travel(center, 270, distanceKM, LengthUnit.KILOMETER);
            LatLng longitude_north = LatLngTool.travel(center, 90, distanceKM, LengthUnit.KILOMETER);
            
            /*return "west: " + (float)latitude_west.getLatitude() + ", east: " + (float)latitude_east.getLatitude() +
                ", south: " + (float)longitude_south.getLongitude() + ", north: " + (float)longitude_north.getLongitude();*/
            
            return require(UnitOfWork.class).apply(em -> {
                return em.createQuery("from City c where c.latitude between :latitude_west and :latitude_east and c.longitude between :longitude_low and :longitude_high")
                    .setParameter("latitude_west", (float)latitude_west.getLatitude())
                    .setParameter("latitude_east", (float)latitude_east.getLatitude())
                    .setParameter("longitude_low", (float)longitude_south.getLongitude())
                    .setParameter("longitude_high", (float)longitude_north.getLongitude())
                    .getResultList();
            });
            
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

    private static void insertRecords(String r) {

        City city = new City(r);
        System.out.println("Insert record: " + r);

    }

    public static void main(final String[] args) {
        run(App::new, args);
    }

}
