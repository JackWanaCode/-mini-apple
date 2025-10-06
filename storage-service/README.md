This is a video storage serivce.

- stores uploaded (pre-encoded) assets on fast local disk,

- streams them back to your edge NGINX with Range support,

- sets ETag and long-lived Cache-Control for hot segment caching,

- exposes health and lightweight JSON metrics,

- optionally enforces a simple HMAC token for edge→origin calls.

Requisitions:
- Java 11
- Scala 2

Install Java11

```yaml
sudo apt-get install openjdk-11-jdk

```

check installed JDKs

```yaml
update-java-alternatives --list
```

if there are 2 more jdk version, switch java 11 to default
```yaml
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```
install scala 2.13.14 - download scala 2.13.14 from official scala website
```yaml
wget https://downloads.lightbend.com/scala/2.13.14/scala-2.13.14.deb
sudo dpkg -i scala-2.13.14.deb
```




Configure absolute path to the videos
```yaml
export PATH_TO_VIDEOS=</your/videos/path/here>
```

Compile and Run service
```yaml
sbt compile & run
```

Check service healthy
```yaml
curl -f http://localhost:8080/healthz
```