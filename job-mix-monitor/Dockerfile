FROM openjdk:13.0.1-oraclelinux7
LABEL Version=1.0

# For debugging
RUN yum install -y procps vi less wget

WORKDIR /app
ADD . /app
RUN /app/recompile.sh
CMD /app/run.sh

