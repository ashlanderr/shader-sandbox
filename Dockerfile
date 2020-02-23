FROM openjdk:11-jdk AS build
COPY . /build
WORKDIR /build
RUN ./bundle.sh

FROM python:3
COPY --from=build /build/build/distributions /web
WORKDIR /web
CMD python -m http.server $PORT
