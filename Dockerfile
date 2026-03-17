FROM public.ecr.aws/docker/library/maven:3.9-amazoncorretto-21-al2023

# Install git
RUN dnf install -y git && dnf clean all

# Create maven user with uid/gid 1000
RUN groupadd -g 1000 maven && \
    useradd -u 1000 -g 1000 -m -d /home/maven maven

# Copy the extension JAR
COPY target/maven-s3-split-resolver-*.jar /usr/share/maven/lib/ext/

# Switch to maven user
USER maven

