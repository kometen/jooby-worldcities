package no.gnome;

import java.util.List;
import org.jooby.Jooby;
import org.jooby.hbm.Hbm;
import org.jooby.hbm.UnitOfWork;

/**
 * @author jooby generator
 */
public class App extends Jooby {

  {
    get("/", (req, rsp) -> {
        String data = "<head><title>gnome.no</title></head>Hello Jooby World!";
        rsp.status(200)
            .type("text/html")
            .send(data);
    });

    get("/favicon.ico", () -> "");
    
    use(new Hbm("jdbc:postgresql://localhost/worldcities")
        .classes(City.class)
    );
    
    get("/cities", req -> {
        return require(UnitOfWork.class).apply(em -> {
            List<City> c = em.createQuery("from City").getResultList();
            String r = "";
            for (City city : c) {
                r = city.getName();
	    }
            return r;
	});
    });

  }
  public static void main(final String[] args) {
    run(App::new, args);
  }

}
