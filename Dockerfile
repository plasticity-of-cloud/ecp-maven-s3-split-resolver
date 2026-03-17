FROM public.ecr.aws/docker/library/maven:3.9-amazoncorretto-21-al2023

# Install git
RUN yum install -y git && yum clean all

# Copy the extension JAR
COPY target/maven-s3-split-resolver-*.jar /usr/share/maven/lib/ext/

# Inherit Maven entrypoint
