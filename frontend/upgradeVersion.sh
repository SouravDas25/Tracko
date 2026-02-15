#!/usr/bin/env bash


if [ -z "$1" ]
 then
    echo "upgrade version has to be specified."
    exit 1
fi

cp pubspec.yaml pubspec-old.yaml
cp lib/env.dart lib/env-old.dart

sed "10s/.*/version: $1/" pubspec-old.yaml > pubspec.yaml
sed "2s/.*/  static const appVersion = \"$1\";/" lib/env-old.dart > lib/env.dart

if [ -z "$2" ]
 then
    git commit -m "version upgraded : $1"
    git tag "$1"
    echo  "Tags has been added"
fi


echo  "App has been upgraded to version - $1"
