# webring-api
Wenring API & Frontend.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

API code: [SiteResource.kt](https://github.com/diademiemi/webring-api/blob/main/src/main/kotlin/dev/queercoded/webring/SiteResource.kt)  
Integration Tests: [SiteResourceTest.kt](https://github.com/diademiemi/webring-api/blob/main/src/test/kotlin/dev/queercoded/webring/SiteResourceTest.kt)  


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./gradlew build -Dquarkus.package.type=native
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/webring-api-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.

# API Documentation

## Env vars
```
# Dev
export WEBRING_API_TOKEN=ADMIN_TOKEN_CHANGE_ME
export WEBRING_API_URL=http://127.0.0.1:8080

# Prod
export WEBRING_API_TOKEN=...
export WEBRING_API_URL=https://webring.queercoded.dev
```

## List sites
```
curl -X GET -H "Content-Type: application/json" -H "Authorization: Bearer ${WEBRING_API_TOKEN}" -v -k ${WEBRING_API_URL}/sites/all | jq

```

## List all disabled sites
```
curl -X GET -H "Content-Type: application/json" -H "Authorization: Bearer ${WEBRING_API_TOKEN}" -v -k ${WEBRING_API_URL}/sites/disabled | jq

```

## List all dead end sites
```
curl -X GET -H "Content-Type: application/json" -H "Authorization: Bearer ${WEBRING_API_TOKEN}" -v -k ${WEBRING_API_URL}/sites/all-dead-end | jq

```

## List all sites including disabled & dead ends
```
curl -X GET -H "Content-Type: application/json" -H "Authorization: Bearer ${WEBRING_API_TOKEN}" -v -k ${WEBRING_API_URL}/sites/all-plus-disabled | jq

```

## Create site
```
export SITE_DATA=$(cat << EOF
{
	"name": "Queer Coded",
	"domain": "queercoded.dev",
	"https": true,
	"author": "Queer Coded Staff",
	"path": "/",
	"disable_checks": false,
	"enabled": true
}
EOF
)

curl -X POST \
-H \
"Content-Type: application/json" \
--data ${SITE_DATA} \
-H \
"Authorization: Bearer ${WEBRING_API_TOKEN}" \
-v -k ${WEBRING_API_URL}/sites/ --raw

```

## Update site
```
export SITE_ID=1
export SITE_DATA=$(cat << EOF
{
	"disable_checks": true
}
EOF
)

curl -X PUT \
-H \
"Content-Type: application/json" \
--data ${SITE_DATA} \
-H \
"Authorization: Bearer ${WEBRING_API_TOKEN}" \
-v -k ${WEBRING_API_URL}/sites/id/${SITE_ID}/update --raw
```

## Enable site
```
export SITE_ID=1

curl -X PUT \
-H \
"Content-Type: application/json" \
-H \
"Authorization: Bearer ${WEBRING_API_TOKEN}" \
-v -k ${WEBRING_API_URL}/sites/id/${SITE_ID}/enable --raw

```

## Disable site

```
export SITE_ID=1

curl -X PUT \
-H \
"Content-Type: application/json" \
-H \
"Authorization: Bearer ${WEBRING_API_TOKEN}" \
-v -k ${WEBRING_API_URL}/sites/id/${SITE_ID}/disable --raw

```

## Delete site
```
export SITE_ID=1

curl -X DELETE -H "Content-Type: application/json" -H "Authorization: Bearer ${WEBRING_API_TOKEN}" -v -k ${WEBRING_API_URL}/sites/id/${SITE_ID} --raw
```

## Force recheck sites
```
curl -X DELETE -H "Content-Type: application/json" -H "Authorization: Bearer ${WEBRING_API_TOKEN}" -v -k ${WEBRING_API_URL}/sites/force-recheck --raw
```

