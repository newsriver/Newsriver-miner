#Old method to intall Chrmore and selenium
#FROM centos:centos7.2.1511

#Install Google Chrome, Java 8, Xvfb and unzip
#---------------------------------------------
#RUN echo -e "[google-chrome]\n\
#name=google-chrome\n\
#baseurl=http://dl.google.com/linux/chrome/rpm/stable/\$basearch\n\
#enabled=1\n\
#gpgcheck=1\n\
#gpgkey=https://dl-ssl.google.com/linux/linux_signing_key.pub" > /etc/yum.repos.d/google-chrome.repo

#RUN yum install -y java-1.8.0-openjdk.x86_64 google-chrome-stable.x86_64 xorg-x11-server-Xvfb unzip wget

#RUN wget http://chromedriver.storage.googleapis.com/2.22/chromedriver_linux64.zip && unzip chromedriver_linux64.zip -d /home/

# ENV DISPLAY=:10

#RUN echo -e "#!/bin/bash\n\
# /usr/bin/Xvfb \$DISPLAY -screen 0 1366x768x24 -ac&\n\
# java -Duser.timezone=GMT -Dfile.encoding=utf-8 -Dwebdriver.chrome.driver=./chromedriver -Duser.timezone=GMT -Dfile.encoding=utf-8 -Xms512m -Xmx1g -Xss1m -XX:MaxMetaspaceSize=512m -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:OnOutOfMemoryError=\"kill -9 %p\" -jar /home/newsriver-miner.jar \"\$@\"" > /home/start.sh
#RUN chmod +x /home/start.sh
#ENTRYPOINT ["./start.sh"]

FROM openjdk:8-jre-alpine
COPY newsriver-miner-*.jar /home/newsriver-miner.jar
WORKDIR /home
EXPOSE 31000-32000
ENV PORT 31112
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Xss1m -XX:MaxMetaspaceSize=512m -Duser.timezone=GMT -Dfile.encoding=utf-8 -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:OnOutOfMemoryError='kill -9 %p'"
#ENTRYPOINT ["java","${JAVA_OPTS}","-Duser.timezone=GMT","-Dfile.encoding=utf-8","-Xms256m","-Xmx512m","-Xss1m","-XX:MaxMetaspaceSize=128m","-XX:+UseConcMarkSweepGC","-XX:+CMSParallelRemarkEnabled","-XX:+UseCMSInitiatingOccupancyOnly","-XX:CMSInitiatingOccupancyFraction=70","-XX:OnOutOfMemoryError='kill -9 %p'","-jar","/home/newsriver-miner.jar"]
ENTRYPOINT exec java $JAVA_OPTS -jar /home/newsriver-miner.jar
CMD []
