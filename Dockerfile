#FROM gradle:8.13-jdk21 as build-docs
#
#WORKDIR /build/docs
#
#COPY . ./
#
#RUN ./gradlew dokkaHtml




#FROM node:lts-alpine AS serve-docs
#
#WORKDIR /build/docs
#
#RUN npm install -g http-server
#
#EXPOSE 8081
#CMD ["http-server", ".", "--proxy", "http://localhost:8081"]




FROM busybox:1.37.0 as build
ENV PORT=8000

WORKDIR /app/javadocs

COPY ./build/javadocs /app/javadocs
EXPOSE $PORT

CMD trap "exit 0;" TERM INT; httpd -v -p $PORT -h /www -f & wait
