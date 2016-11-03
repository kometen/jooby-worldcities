# jooby-worldcities
Show cities within a radius via a rest interface.

I'm using the very handy java web framework jooby (jooby.org) which simplifies java web development alot. Just
create the project and you are (almost) up and running.

Jooby requires java 8 and maven to be installed. My example uses postgresql so this needs to be installed as well.

I have used postgresql 9.4 on macOS sierra and 9.6 on FreeBSD 11.0 RELEASE.

Create the jooby-project using

$ mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=jooby-archetype -DarchetypeGroupId=org.jooby -DarchetypeVersion=1.0.0.CR8

Start the project

$ mvn jooby:run

Add json to pom.xml

<!-- Jackson json -->
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jackson</artifactId>
</dependency>

And in App.java

use (new Jackson());

Add postgresql to pom.xml.

<!-- postgresql -->
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>9.4.1211.jre7</version>
</dependency>

Add hibernate ORM to pom.xml.

<!-- hibernate orm -->
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-hbm</artifactId>
</dependency>

Then create the database using

$ createdb worldcities

Connect the postgresql-client to the database and create indexes to speed up searches.

$ psql worldcities
create index latitude_idx on city (latitude);
create index longitude_idx on city (longitude);
create index name_idx on city (name);
create index name_lowercase_idx on city (name_lowercase);
create index region_idx on city (region);
