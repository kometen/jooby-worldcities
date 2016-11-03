# jooby-worldcities
Show cities within a radius via a rest interface.

I'm using the very handy java web framework [jooby][1] which simplifies java web development alot. Just
create the project and you are (almost) up and running.

Jooby requires java 8 and maven to be installed. My example uses postgresql so this needs to be installed as well.

I have used postgresql 9.4 on macOS sierra and 9.6 on FreeBSD 11.0 RELEASE.

Create the jooby-project using

```
$ mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=jooby-archetype -DarchetypeGroupId=org.jooby -DarchetypeVersion=1.0.0.CR8
```

Start the project

```
$ mvn jooby:run
```

Add json to pom.xml

```
<!-- Jackson json -->
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jackson</artifactId>
</dependency>
```
And in App.java

```
use (new Jackson());
```

Add postgresql to pom.xml.

```
<!-- postgresql -->
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>9.4.1211.jre7</version>
</dependency>
```

Add hibernate ORM to pom.xml.

```
<!-- hibernate orm -->
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-hbm</artifactId>
</dependency>
```

Then create the database using

```
$ createdb worldcities
```

Connect the postgresql-client to the database and create indexes to speed up searches.

```
$ psql worldcities
create index latitude_idx on city (latitude);
create index longitude_idx on city (longitude);
create index name_idx on city (name);
create index name_lowercase_idx on city (name_lowercase);
create index region_idx on city (region);
```

The file I load is called worldcitiespop.txt. Place this on the root-level of the project where pom.xml is located. Download a copy of the file from my [dropbox-account][2].

Then import the data using http://localhost:8080/import. This took approx. seven minutes on my 15" macbook pro from 2012 with 16 GB ram. When importing on FreeBSD I had to [increase][3] ram allocated to java with

```
$ export MAVEN_OPTS="-Xmx3G -Xss256M -XX:+CMSClassUnloadingEnabled"
$ mvn jooby:run
```

It took a bit longer importing the data on FreeBSD but since it is running inside vmware fusion and disk-access isn't that great that's fine. I have allocated six GB ram and two cpu-cores.

When the import is completed you can search for a city like Berlin using [http://localhost:8080/cities/Berlin][4]. You can also search for cities within a radius in kilometer using latitude and longitude and an optional radius like [http://cities/61.7428745/6.3968833/25][5]. If kilometer is omitted it defaults to 20.

[1]: http://jooby.org
[2]: https://dl.dropboxusercontent.com/u/2729115/worldcitiespop.zip
[3]: http://stackoverflow.com/a/18771124/319826
[4]: http://localhost:8080/cities/Berlin
[5]: http://cities/61.7428745/6.3968833/25
