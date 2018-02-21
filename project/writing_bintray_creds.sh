echo Writing bintray creds at `pwd`/.bintray_credentials
echo "realm = Bintray API Realm
host = api.bintray.com
user = $BINTRAY_USER
password = $BINTRAY_PASS" > `pwd`/.bintray_credentials