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
                return em.createQuery("from City")
                    .setMaxResults(100)
                    .getResultList();
            });
        });
        
        get("/cities/:name", req -> {
            Map<String, Mutant> name = req.params("name").toMap();
            return require(UnitOfWork.class).apply(em -> {
                return em.createQuery("from City where name = :name")
                    .setParameter("name", name.get("name").value())
                    .getResultList();
            });
        });

        get("/import", () -> {
            return require(UnitOfWork.class).apply(em -> {
                List<City> cities = em.createQuery("from City").setMaxResults(1).getResultList();
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
