# TODO probably want to find a smaller starting place if we need a smaller image
FROM gitpod/workspace-full

# install tarballs for binaries that we need. Putting them in ~/lib
# Kafka 2.5.0 via confluent community
RUN mkdir -p ~/lib && \
      # curl -L -s "http://mirror.cc.columbia.edu/pub/software/apache/kafka/2.5.0/kafka_2.12-2.5.0.tgz" | tar xvz -C ~/lib && \
  # https://docs.confluent.io/current/installation/installing_cp/deb-ubuntu.html
  wget -qO - https://packages.confluent.io/deb/5.5/archive.key | sudo apt-key add - && \
  sudo add-apt-repository "deb [arch=amd64] https://packages.confluent.io/deb/5.5 stable main" && \
  sudo apt-get update && sudo apt-get install -y confluent-community-2.12 && \
  curl -LO https://github.com/tchiotludo/akhq/releases/download/0.15.0/akhq.jar && \
  mv akhq.jar ~/lib

# They have java 11 by default; we want java 8 for now. Consider installing java 8 and set as default
# RUN sdk update && \
#       sdk install java 8.0.265.hs-adpt && \
#       sdk ...

# TODO add akhq

# RUN sudo apt-get update \
#  && sudo apt-get install -y \
#  && sudo rm -rf /var/lib/apt/lists/*
