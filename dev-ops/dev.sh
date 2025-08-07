#!/bin/bash
SERVER_DEPLOY=192.168.1.103
TARGET_DIR=/opt/deploy/dreamcareer/media
APP_ID=itdream-media
PACKAGE_NAME=com.itcareer.media

echo "Build source..."
cd ../source/itcareer-media
mvn clean package -Dmaven.test.skip
cd ../../dev-ops

echo "Update config..."
# delete release folder if exist
rm -rf release 
mkdir release

cp ../source/itcareer-media/target/itcareer-media-spring-boot.jar release/app.jar

cp config/* release/
rm -rf release/application-prod.properties
sed -i "s/{ENV}/local/g" release/application.properties
sed -i "s/{APP_ID}/$APP_ID/g" release/application-local.properties
sed -i "s/{PACKAGE_NAME}/$PACKAGE_NAME/g" release/application-local.properties
sed -i "s/{PACKAGE_NAME}/$PACKAGE_NAME/g" release/logback-spring.xml

cp service-template-local.service release/$APP_ID.service
sed -i "s/{CONFIG_LOCATION}/$(printf '%s\n' "$TARGET_DIR" | sed -e 's/[]\/$*.^[]/\\&/g')/g" release/$APP_ID.service
sed -i "s/{ENV}/local/g" release/$APP_ID.service
sed -i "s/{APP_ID}/$APP_ID/g" release/$APP_ID.service

cp logs-template.conf release/$APP_ID.conf
sed -i "s/{APP_ID}/$APP_ID/g" release/$APP_ID.conf


echo "Compress source..."
tar -czf api.tar.gz release

echo "Deploy to server..."
echo " ---> Stop old service..."
ssh root@$SERVER_DEPLOY "mkdir -p $TARGET_DIR"
ssh root@$SERVER_DEPLOY "systemctl stop $APP_ID.service"
ssh root@$SERVER_DEPLOY "rm -rf $TARGET_DIR/*"

echo " ---> Remove old service"
ssh root@$SERVER_DEPLOY "rm -rf /lib/systemd/system/$APP_ID.service"

echo " ---> Upload build..."
scp api.tar.gz root@$SERVER_DEPLOY:$TARGET_DIR/api.tar.gz
ssh root@$SERVER_DEPLOY "cd $TARGET_DIR && tar -xzf api.tar.gz && rm -rf api.tar.gz && mv release/* . && rm -rf release"

echo " ---> Deploy new service..."
ssh root@$SERVER_DEPLOY "mv $TARGET_DIR/$APP_ID.service /lib/systemd/system/$APP_ID.service"
ssh root@$SERVER_DEPLOY "chmod 644 /lib/systemd/system/$APP_ID.service && systemctl daemon-reload"
ssh root@$SERVER_DEPLOY "systemctl enable $APP_ID.service"

echo " ---> Deploy log service..."
# Create log directory
ssh root@$SERVER_DEPLOY "mkdir -p /var/log/$APP_ID/ && touch /var/log/$APP_ID/log.log"
# Deploy log service
ssh root@$SERVER_DEPLOY "rm -rf /etc/rsyslog.d/$APP_ID.conf && cd $TARGET_DIR && cp $APP_ID.conf /etc/rsyslog.d/$APP_ID.conf && systemctl restart rsyslog"

# Start application
ssh root@$SERVER_DEPLOY "systemctl start $APP_ID.service"

echo "Cleanup..."
rm -rf release
rm -rf api.tar.gz
echo "############# DONE #############"