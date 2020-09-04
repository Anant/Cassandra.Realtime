# TODO probably want to find a smaller starting place if we need a smaller image
FROM gitpod/workspace-full

ENV BINARY_DIR /home/gitpod/lib
# after unzipped, will look like this
ENV CONFLUENT_HOME /home/gitpod/lib/confluent-5.5.1
ENV PROJECT_HOME /workspace/cassandra.realtime
ENV PATH ${CONFLUENT_HOME}/bin:${PATH}


# using confluent community, and tarball, since gitpod doesn't allow sudo use, so can't do sudo systemctl... in gitpod
# install tarballs for binaries that we need. Putting them in ~/lib
# Kafka 2.5.0 via confluent community
RUN mkdir -p ${BINARY_DIR}
RUN sudo curl -L -s http://packages.confluent.io/archive/5.5/confluent-community-5.5.1-2.12.tar.gz | tar xvz -C ${BINARY_DIR}
# confluent cli
RUN curl -L --http1.1 https://cnfl.io/cli | sudo sh -s -- v1.6.0 -b /usr/local/bin

# get akhq, and put it in our lib dir
RUN curl -LO https://github.com/tchiotludo/akhq/releases/download/0.15.0/akhq.jar && \
  mv akhq.jar $BINARY_DIR
