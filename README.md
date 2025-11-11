# CampusCoffee (WS 25/26)

## Prerequisites

* Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) or a compatible open-source alternative such as [Rancher Desktop](https://rancherdesktop.io/).
* Install the [Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21&os=any&arch=any) and [Maven 3.9](https://maven.apache.org/install.html) either via the provided [`mise.toml`](mise.toml) file (see [getting started guide](https://mise.jdx.dev/getting-started.html) for details) or directly via your favorite package manager.
* Install a Java IDE. We recommend [IntelliJ](https://www.jetbrains.com/idea/), but you are free to use alternatives such as [VS Code](https://code.visualstudio.com/) with suitable extensions.

## Build application

First, make sure that the Docker daemon is running.
Then, to build the application, run the following command in the command line (or use the Maven integration of your IDE):

```shell
mvn clean install
```
**Note:** In the `dev` profile, all repositories are cleared before startup, the initial data is loaded (see [`LoadInitialData.java`](application/src/main/java/de/seuhd/campuscoffee/LoadInitialData.java)).

You can use the quiet mode to suppress most log messages:

```shell
mvn clean install -q
```

## Start application (dev)

First, make sure that the Docker daemon is running.
Before you start the application, you first need to start a Postgres docker container:

```shell
docker run -d -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:17-alpine
```

Then, you can start the application:

```shell
cd application
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
**Note:** The data source is configured via the [`application.yaml`](application/src/main/resources/application.yaml) file.

## REST API

You can use `curl` in the command line to send HTTP requests to the REST API.

### POS endpoint

#### Get POS

All POS:
```shell
curl http://localhost:8080/api/pos
```
POS by ID:
```shell
curl http://localhost:8080/api/pos/1 # add valid POS id here
```

#### Create POS

Create a POS based on a JSON object provided in the request body:

```shell
curl --header "Content-Type: application/json" --request POST --data '{"name":"New Café","description":"Description","type":"CAFE","campus":"ALTSTADT","street":"Hauptstraße","houseNumber":"100","postalCode":69117,"city":"Heidelberg"}' http://localhost:8080/api/pos
```

#### Import POS from OpenStreetMap

Create a POS based on an OpenStreetMap node by providing the node ID. The system will fetch the node's data from OpenStreetMap and automatically convert it to a POS entity.

```shell
curl --request POST http://localhost:8080/api/pos/import/osm/5589879349 # set a valid OSM node ID here
```

**How it works:**
- Fetches the OSM node data using the provided ID
- Extracts relevant information from OSM tags (name, address, amenity type, etc.)
- Maps the data to POS fields with intelligent defaults
- Creates or updates the POS in the system

**Example OSM Node:**
The following OSM node (ID: 5589879349) contains a cafe in Heidelberg:

```xml
<node id="5589879349" lat="49.4122362" lon="8.7077883">
  <tag k="name" v="Rada Coffee &amp; Rösterei"/>
  <tag k="amenity" v="cafe"/>
  <tag k="addr:street" v="Untere Straße"/>
  <tag k="addr:housenumber" v="21"/>
  <tag k="addr:postcode" v="69117"/>
  <tag k="addr:city" v="Heidelberg"/>
  <tag k="cuisine" v="coffee_shop;breakfast"/>
  <tag k="description" v="Caffé und Rösterei"/>
</node>
```

This would create a POS with:
- Name: "Rada Coffee & Rösterei"
- Type: CAFE
- Campus: ALTSTADT
- Address: Untere Straße 21, 69117 Heidelberg
- Description: "Caffé und Rösterei"

#### Update POS

Update title and description:
```shell
curl --header "Content-Type: application/json" --request PUT --data '{"id":4,"name":"New coffee","description":"Great croissants","type":"CAFE","campus":"ALTSTADT","street":"Hauptstraße","houseNumber":"95","postalCode":69117,"city":"Heidelberg"}' http://localhost:8080/api/pos/4 # set correct POS id here and in the body
```
