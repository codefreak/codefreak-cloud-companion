# Code FREAK Cloud Workspace Companion

Application that runs in each Code FREAK Cloud Workspace.
It uses a hybrid protocol based on GraphQL over Websockets and REST/HTTP.
GraphQL is used for control messages and REST for native file upload/download.
The application is written in Kotlin based on Spring Boot, WebFlux (Java RX) and Java GraphQL.
It is shipped as self-contained container image that can be run on Docker, Kubernetes etc.

Current Features:
* Download/Upload files
* List and watch project files on the server

## Context
The companion is used for Code FREAK's *Cloud Workspaces* feature which allows writing code in your browser and running
it on the server.
Each user receives its own "workspace" on the server which is created from multiple containers.

## Running locally
The companion is a standard Spring Boot web application which can be run locally for development/testing.
Please copy the file `src/main/resources/application-dev.yml.dist` to `src/main/resources/application-dev.yml`. 
To start the webserver on port 8080 (default) run the following command:
```shell
./gradlew bootRun
```

## Endpoints

### GraphQL (`/graphql`)
The GraphQL server is available at `/graphql`.
During development, you can use the GraphiQL playground for testing at `/playground`.
The server accepts both `POST` and Websocket queries. 
The GQL websocket protocol is based on [`graphql-ws`](https://github.com/enisdenjo/graphql-ws)!

### `POST /files`
Expects a `multipart/form-data` request with one or multiple field with the name `files`.
Each file will be stored inside the container in `/code` by default.
The filename can also contain slashes to upload to sub-directories.
If the sub-directories do not exist the will be created.
If part of the sub-directory are existing files an exception will be thrown.
Existing files will be overridden without further questioning.
The server responds with `201 Created` in case everything was uploaded properly.

Example request:

```
POST /files HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data; boundary=----random-string-generated-by-browser
...

------random-string-generated-by-browser
Content-Disposition: form-data; name="files"; filename="main.c"

...
------random-string-generated-by-browser
Content-Disposition: form-data; name="files"; filename="lib/func.c"
Content-Type: application/x-object

...
------random-string-generated-by-browser
```

### `GET /files/{filepath}`
Allows downloading a file specified by `{filepath}`, e.g. `/files/main.c`.
The will serve the workspace file (if it exists) with the one of the following mime-types:
* `text/plain` for all textual files
* The proper mime type for images (mime starts with `image/*`)
* `application/octet-stream` for everything else



## Testing
```shell
./gradlew check
```

## Building
To build the container image we use Google's Jib.
The image will be built to your local Docker daemon as `ghcr.io/henningcash/codefreak-cloud-companion` with the following command:
```shell
./gradlew jibDockerBuild
```

## LICENSE
See the `LICENSE` file that is shipped with the source code.